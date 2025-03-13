package com.swe574.group2.backend.migration;

import com.google.cloud.storage.Acl;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fixed migration script to move existing images from database to Google Cloud Storage.
 * Only runs when the "migration" profile is active.
 *  * java -jar your-app.jar --spring.profiles.active=migration
 */
@Component
@Profile("migration")
public class MigrateImagesFromDbToGCS implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(MigrateImagesFromDbToGCS.class);
    
    @Autowired
    private MysteryObjectRepository mysteryObjectRepository;
    
    @Autowired
    private Storage storage;
    
    @Value("${gcp.storage.bucket-name}")
    private String bucketName;
    
    @Value("${migration.batch-size:100}")
    private int batchSize;
    
    @Value("${migration.dry-run:false}")
    private boolean dryRun;
    
    @Value("${migration.url-expiration-days:0}")
    private int urlExpirationDays; // 0 means create permanent public URLs

    @Override
    public void run(String... args) {
        logger.info("Starting database to Google Cloud Storage image migration" + (dryRun ? " (DRY RUN)" : ""));
        
        AtomicInteger processedCount = new AtomicInteger(0);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failedCount = new AtomicInteger(0);
        
        // Fixed approach: Use page number instead of offset
        int pageNumber = 0;
        Page<MysteryObject> page;
        
        do {
            // Correctly create a Page request with page number
            Pageable pageable = PageRequest.of(pageNumber, batchSize);
            
            // Use repository method that returns a Page instead of List
            page = mysteryObjectRepository.findByImageIsNotNull(pageable);
            
            List<MysteryObject> batch = page.getContent();
            
            logger.info("Processing batch of {} objects on page {}", batch.size(), pageNumber);
            
            for (MysteryObject mysteryObject : batch) {
                try {
                    processedCount.incrementAndGet();
                    
                    if (mysteryObject.getImage() == null || mysteryObject.getImage().length == 0) {
                        logger.warn("Object {} has null or empty image data, skipping", mysteryObject.getId());
                        continue;
                    }
                    
                    // Generate a unique filename
                    String fileName = "mystery-objects/" + UUID.randomUUID() + "-" + mysteryObject.getId() + ".jpg";
                    
                    if (!dryRun) {
                        // Create the blob info
                        BlobId blobId = BlobId.of(bucketName, fileName);
                        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                                .setContentType("image/jpeg") // Assuming JPEG, adjust as needed
                                .setAcl(new ArrayList<>(Collections.singletonList(
                                    Acl.of(Acl.User.ofAllUsers(), Acl.Role.READER))))
                                .build();
                        
                        // Upload the image to GCS
                        storage.create(blobInfo, mysteryObject.getImage());
                        
                        // Generate a URL for the image
                        String imageUrl;
                        if (urlExpirationDays > 0) {
                            // Generate a signed URL with expiration
                            imageUrl = storage.get(blobId)
                                    .signUrl(urlExpirationDays, TimeUnit.DAYS)
                                    .toString();
                        } else {
                            // Generate a permanent public URL
                            imageUrl = String.format("https://storage.googleapis.com/%s/%s", bucketName, fileName);
                        }
                        
                        // Update the object with the GCS URL
                        mysteryObject.setImageUrl(imageUrl);
                        
                        // Clear the binary image data to free up space in the database
                        mysteryObject.setImage(null);
                        
                        // Save the updated object
                        mysteryObjectRepository.save(mysteryObject);
                    }
                    
                    successCount.incrementAndGet();
                    
                    if (processedCount.get() % 10 == 0) {
                        logger.info("Processed {} items, {} successful, {} failed", 
                                processedCount.get(), successCount.get(), failedCount.get());
                    }
                } catch (Exception e) {
                    logger.error("Error migrating image for object ID {}: {}", mysteryObject.getId(), e.getMessage(), e);
                    failedCount.incrementAndGet();
                }
            }
            
            // Increment the page number for next iteration
            pageNumber++;
            
            // Properly save after each batch to ensure we don't lose progress
            if (!dryRun) {
                logger.info("Flushing changes to database");
            }
            
        } while (page.hasNext());  // Use Page.hasNext() to check if there are more pages
        
        logger.info("Migration completed. Processed: {}, Successful: {}, Failed: {}",
                processedCount.get(), successCount.get(), failedCount.get());
    }
}