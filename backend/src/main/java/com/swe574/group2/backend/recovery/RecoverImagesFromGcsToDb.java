package com.swe574.group2.backend.recovery;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.swe574.group2.backend.dao.MysteryObjectRepository;
import com.swe574.group2.backend.entity.MysteryObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Recovery script to restore base64 images from Google Cloud Storage back to the database.
 * Only runs when the "recovery" profile is active.
 */
@Component
@Profile("recovery")
public class RecoverImagesFromGcsToDb implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(RecoverImagesFromGcsToDb.class);
    
    @Autowired
    private MysteryObjectRepository mysteryObjectRepository;
    
    @Autowired
    private Storage storage;
    
    @Value("${gcp.storage.bucket-name}")
    private String bucketName;
    
    @Value("${recovery.batch-size:50}")
    private int batchSize;
    
    @Value("${recovery.dry-run:false}")
    private boolean dryRun;
    
    // Pattern to extract object name from Google Cloud Storage URL
    private static final Pattern GCS_URL_PATTERN = Pattern.compile("https://storage\\.googleapis\\.com/[^/]+/(.+)");

    @Override
    public void run(String... args) {
        logger.info("Starting recovery from Google Cloud Storage to database" + (dryRun ? " (DRY RUN)" : ""));
        
        AtomicInteger processedCount = new AtomicInteger(0);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failedCount = new AtomicInteger(0);
        
        int pageNumber = 0;
        Page<MysteryObject> page;
        
        do {
            // Get objects with imageUrl but no image data
            Pageable pageable = PageRequest.of(pageNumber, batchSize);
            page = mysteryObjectRepository.findByImageUrlIsNotNullAndImageIsNull(pageable);
            
            logger.info("Processing batch of {} objects on page {}", page.getContent().size(), pageNumber);
            
            for (MysteryObject mysteryObject : page.getContent()) {
                try {
                    processedCount.incrementAndGet();
                    
                    // Skip if no imageUrl
                    if (mysteryObject.getImageUrl() == null || mysteryObject.getImageUrl().isEmpty()) {
                        logger.warn("Object {} has no imageUrl, skipping", mysteryObject.getId());
                        continue;
                    }
                    
                    byte[] imageData = null;
                    
                    // If it's a GCS URL, extract the object name and download from GCS
                    if (mysteryObject.getImageUrl().contains("storage.googleapis.com")) {
                        imageData = downloadFromGcs(mysteryObject.getImageUrl());
                    } else {
                        // Regular HTTP URL
                        imageData = downloadFromUrl(mysteryObject.getImageUrl());
                    }
                    
                    if (imageData == null || imageData.length == 0) {
                        logger.warn("Downloaded empty image data for object {}, skipping", mysteryObject.getId());
                        failedCount.incrementAndGet();
                        continue;
                    }
                    
                    if (!dryRun) {
                        // Update the object with the downloaded image data
                        mysteryObject.setImage(imageData);
                        mysteryObjectRepository.save(mysteryObject);
                    }
                    
                    successCount.incrementAndGet();
                    
                    if (processedCount.get() % 10 == 0) {
                        logger.info("Processed {} items, {} successful, {} failed", 
                                processedCount.get(), successCount.get(), failedCount.get());
                    }
                } catch (Exception e) {
                    logger.error("Error recovering image for object ID {}: {}", mysteryObject.getId(), e.getMessage(), e);
                    failedCount.incrementAndGet();
                }
            }
            
            // Increment the page number for next iteration
            pageNumber++;
            
            // Save after each batch to ensure we don't lose progress
            if (!dryRun) {
                logger.info("Flushing changes to database");
            }
            
        } while (page.hasNext());
        
        logger.info("Recovery completed. Processed: {}, Successful: {}, Failed: {}",
                processedCount.get(), successCount.get(), failedCount.get());
    }
    
    /**
     * Extract object name from GCS URL and download the content directly from GCS
     */
    private byte[] downloadFromGcs(String imageUrl) {
        try {
            Matcher matcher = GCS_URL_PATTERN.matcher(imageUrl);
            if (!matcher.find()) {
                logger.warn("Could not extract object name from URL: {}", imageUrl);
                return null;
            }
            
            String objectName = matcher.group(1);
            logger.info("Downloading from GCS: {}", objectName);
            
            // Get the blob from GCS
            BlobId blobId = BlobId.of(bucketName, objectName);
            Blob blob = storage.get(blobId);
            
            if (blob == null) {
                logger.warn("Blob not found in GCS: {}", objectName);
                return null;
            }
            
            // Download the content
            return blob.getContent();
        } catch (Exception e) {
            logger.error("Error downloading from GCS: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Download image from a regular HTTP URL
     */
    private byte[] downloadFromUrl(String imageUrl) {
        try {
            logger.info("Downloading from URL: {}", imageUrl);
            URL url = new URL(imageUrl);
            URLConnection connection = url.openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            
            // Read all bytes from the input stream
            try (java.io.InputStream in = connection.getInputStream()) {
                return in.readAllBytes();
            }
        } catch (Exception e) {
            logger.error("Error downloading from URL: {}", e.getMessage(), e);
            return null;
        }
    }
}