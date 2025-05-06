package com.swe574.group2.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swe574.group2.backend.dao.CommentRepository;
import com.swe574.group2.backend.dao.MediaFileRepository;
import com.swe574.group2.backend.dao.MysteryObjectRepository;
import com.swe574.group2.backend.dao.PostRepository;
import com.swe574.group2.backend.dao.UserRepository;
import com.swe574.group2.backend.dto.PostCreationDto;
import com.swe574.group2.backend.dto.PostDetailsDto;
import com.swe574.group2.backend.dto.PostDetailsNoMediaDto; // Added import
import com.swe574.group2.backend.dto.PostListDto;
import com.swe574.group2.backend.dto.ResolutionDto;
import com.swe574.group2.backend.dto.SearchResultDto;
import com.swe574.group2.backend.dto.MediaFileDto;
import com.swe574.group2.backend.dto.MysteryObjectDto;
import com.swe574.group2.backend.entity.Comment;
import com.swe574.group2.backend.entity.MediaFile;
import com.swe574.group2.backend.entity.MysteryObject;
import com.swe574.group2.backend.entity.Post;
import com.swe574.group2.backend.entity.User;
import com.swe574.group2.backend.enums.FollowerNotificationType;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final MysteryObjectRepository mysteryObjectRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final NotificationService notificationService;
    private final MediaFileRepository mediaFileRepository;
    private final FollowerNotificationService followerNotificationService;

    public PostService(PostRepository postRepository, MysteryObjectRepository mysteryObjectRepository,
            UserRepository userRepository, CommentRepository commentRepository, NotificationService notificationService,
            MediaFileRepository mediaFileRepository, FollowerNotificationService followerNotificationService) {
        this.postRepository = postRepository;
        this.mysteryObjectRepository = mysteryObjectRepository;
        this.userRepository = userRepository;
        this.commentRepository = commentRepository;
        this.notificationService = notificationService;
        this.mediaFileRepository = mediaFileRepository;
        this.followerNotificationService = followerNotificationService;
    }

    // Main color mapping utility
    private static final Map<String, String> MAIN_COLORS = Map.of(
        "red", "#FF0000",
        "green", "#00FF00",
        "blue", "#0000FF",
        "yellow", "#FFFF00",
        "orange", "#FFA500",
        "purple", "#800080",
        "brown", "#A52A2A",
        "black", "#000000",
        "white", "#FFFFFF",
        "gray", "#808080"
    );

    private static int[] hexToRgb(String hex) {
        if (hex == null) return new int[]{0,0,0};
        hex = hex.replace("#", "");
        if (hex.length() == 3) {
            hex = "" + hex.charAt(0) + hex.charAt(0)
                    + hex.charAt(1) + hex.charAt(1)
                    + hex.charAt(2) + hex.charAt(2);
        }
        try {
            int r = Integer.parseInt(hex.substring(0, 2), 16);
            int g = Integer.parseInt(hex.substring(2, 4), 16);
            int b = Integer.parseInt(hex.substring(4, 6), 16);
            return new int[]{r, g, b};
        } catch (Exception e) {
            return new int[]{0,0,0};
        }
    }

    private static String getMainColorHex(String hex) {
        int[] rgb = hexToRgb(hex);
        String closest = null;
        double minDist = Double.MAX_VALUE;
        for (Map.Entry<String, String> entry : MAIN_COLORS.entrySet()) {
            int[] mainRgb = hexToRgb(entry.getValue());
            double dist = Math.pow(rgb[0] - mainRgb[0], 2)
                        + Math.pow(rgb[1] - mainRgb[1], 2)
                        + Math.pow(rgb[2] - mainRgb[2], 2);
            if (dist < minDist) {
                minDist = dist;
                closest = entry.getValue();
            }
        }
        return closest;
    }

    private static void assignMainColorRecursive(MysteryObject obj) {
        if (obj == null) return;
        obj.setMainColor(getMainColorHex(obj.getColor()));
        if (obj.getSubParts() != null) {
            for (MysteryObject sub : obj.getSubParts()) {
                assignMainColorRecursive(sub);
            }
        }
    }

    @Transactional
    public Map<String, Long> createPost(PostCreationDto postCreationDto, String userName) {
        Post post = new Post();
        post.setTitle(postCreationDto.getTitle());
        post.setDescription(postCreationDto.getContent());

        // Handle tags and their labels
        if (postCreationDto.getTags() != null && !postCreationDto.getTags().isEmpty()) {
            Map<String, String> tagMap = new HashMap<>();
            Set<String> tags = postCreationDto.getTags();

            RestTemplate restTemplate = new RestTemplate();
            ObjectMapper mapper = new ObjectMapper();

            // Process each tag individually
            for (String tagId : tags) {
                try {
                    String url = "https://www.wikidata.org/w/api.php?action=wbgetentities&ids=" + tagId +
                            "&props=labels&languages=en&format=json";

                    String response = restTemplate.getForObject(url, String.class);
                    JsonNode root = mapper.readTree(response);
                    JsonNode entity = root.path("entities").path(tagId);

                    String label = tagId; // Default fallback
                    if (!entity.isMissingNode()) {
                        JsonNode enLabel = entity.path("labels").path("en").path("value");
                        if (!enLabel.isMissingNode()) {
                            label = enLabel.asText();
                        }
                    }

                    tagMap.put(tagId, label);
                } catch (Exception e) {
                    // Log the error and use the tag ID as fallback
                    System.err.println("Error fetching label for tag " + tagId + ": " + e.getMessage());
                    tagMap.put(tagId, tagId);
                }
            }

            post.setTagMap(tagMap);
        }

        post.setUser(userRepository.findByEmail(userName).orElseThrow());

        MysteryObject mysteryObject = postCreationDto.getMysteryObject();        

        // Handle sub-parts if any
        List<MysteryObject> subParts = new ArrayList<>();
        if (mysteryObject.getSubParts() != null && !mysteryObject.getSubParts().isEmpty()) {
            // Save the sub-parts temporarily
            subParts.addAll(mysteryObject.getSubParts());
            // Clear the sub-parts from the object so we can save it first
            mysteryObject.setSubParts(new ArrayList<>());
        }

        assignMainColorRecursive(mysteryObject);

        mysteryObjectRepository.save(mysteryObject);

        // Now add sub-parts if any
        if (!subParts.isEmpty()) {
            for (MysteryObject subPart : subParts) {
                mysteryObject.addSubPart(subPart);
            }
            mysteryObjectRepository.save(mysteryObject);
        }

        post.setMysteryObject(mysteryObject);

        Post savedPost = postRepository.save(post);

        Map<String, Long> response = new HashMap<>();
        response.put("postId", savedPost.getId());
        response.put("mysteryObjectId", mysteryObject.getId());
        
        followerNotificationService.sendFollowedUserPostCreatedNotificationToAll(userName, savedPost.getId());

        return response;
    }

    public Page<PostListDto> getAllPostsForPostList(Pageable pageable) {
        Page<PostListDto> posts = postRepository.findAllForPostList(pageable);
        posts.forEach(post -> {
            // Using tag keys (Q-IDs) for consistency with existing code
            Set<String> tags = postRepository.findTagKeysByPostId(post.getId());
            post.setTags(tags);
        });
        return posts;
    }

    public Map<String, Long> upvotePost(Long postId, String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        Post post = postRepository.findById(postId).orElseThrow();

        if (post.getUpvotedBy().contains(user)) {
            post.getUpvotedBy().remove(user);
            post.setUpvotesCount(post.getUpvotesCount() - 1);
        } else {
            post.getUpvotedBy().add(user);
            post.setUpvotesCount(post.getUpvotesCount() + 1);

            if (post.getDownvotedBy().contains(user)) {
                post.getDownvotedBy().remove(user);
                post.setDownvotesCount(post.getDownvotesCount() - 1);
            }
            followerNotificationService.sendFollowedPostActivityNotification(post,
                    FollowerNotificationType.POST_UPVOTED);
        }

        postRepository.save(post);

        Map<String, Long> response = new HashMap<>();
        response.put("postId", post.getId());

        return response;
    }

    public Map<String, Long> downvotePost(Long postId, String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        Post post = postRepository.findById(postId).orElseThrow();

        if (post.getDownvotedBy().contains(user)) {
            post.getDownvotedBy().remove(user);
            post.setDownvotesCount(post.getDownvotesCount() - 1);
        } else {
            post.getDownvotedBy().add(user);
            post.setDownvotesCount(post.getDownvotesCount() + 1);

            if (post.getUpvotedBy().contains(user)) {
                post.getUpvotedBy().remove(user);
                post.setUpvotesCount(post.getUpvotesCount() - 1);
            }
            followerNotificationService.sendFollowedPostActivityNotification(post,
                    FollowerNotificationType.POST_DOWNVOTED);
        }

        postRepository.save(post);

        Map<String, Long> response = new HashMap<>();
        response.put("postId", post.getId());

        return response;
    }

    public PostDetailsDto getPostDetails(Long postId, String username) {
        User user = username != null ? userRepository.findByEmail(username).orElseThrow() : null;
        Post post = postRepository.findPostDetailsById(postId);
        if (post == null) {
            throw new ResourceNotFoundException("Post not found with ID: " + postId);
        }

        Set<String> tags = postRepository.findTagKeysByPostId(postId);

        PostDetailsDto postDetailsDto = new PostDetailsDto();
        mapPostToDto(post, tags, postDetailsDto, user);

        // Add media files information if mystery object exists
        if (post.getMysteryObject() != null) {
            List<MediaFile> mediaFiles = mediaFileRepository.findByMysteryObjectId(post.getMysteryObject().getId());
            List<MediaFileDto> mediaFileDtos = mediaFiles.stream().map(mediaFile -> {
                MediaFileDto dto = new MediaFileDto();
                dto.setId(mediaFile.getId());
                dto.setFileName(mediaFile.getFileName());
                dto.setFileType(mediaFile.getFileType());
                dto.setCreatedAt(mediaFile.getCreatedAt());
                return dto;
            }).collect(Collectors.toList());

            postDetailsDto.setMediaFiles(mediaFileDtos);
        }

        return postDetailsDto;
    }

    @Transactional
    public boolean resolvePost(Long postId, ResolutionDto resolutionDto, String username) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new IllegalArgumentException("Post not found."));

        // Check if the logged-in user is the creator of the post
        if (!post.getUser().getEmail().equals(username)) {
            return false; // Not authorized
        }

        // Set resolution details
        post.setResolutionDescription(resolutionDto.getDescription());
        post.setResolvedAt(LocalDateTime.now());
        post.setSolved(true);

        // Add contributing comments
        if (resolutionDto.getContributingCommentIds() != null && !resolutionDto.getContributingCommentIds().isEmpty()) {
            Set<Comment> contributingComments = new HashSet<>();
            for (Long commentId : resolutionDto.getContributingCommentIds()) {
                Comment comment = commentRepository.findById(commentId)
                        .orElseThrow(() -> new IllegalArgumentException("Comment not found with ID: " + commentId));
                contributingComments.add(comment);

                // Send notification to comment author
                notificationService.sendContributingCommentNotification(comment.getUser().getId(), post, comment);
            }
            post.setContributingComments(contributingComments);
        }

        // Save the updated post
        postRepository.save(post);
        followerNotificationService.sendFollowedPostActivityNotification(post, FollowerNotificationType.POST_RESOLVED);

        return true;
    }

    @Transactional
    public boolean unresolvePost(Long postId, String username) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new IllegalArgumentException("Post not found."));

        // Check if the logged-in user is the creator of the post
        if (!post.getUser().getEmail().equals(username)) {
            return false; // Not authorized
        }

        // Clear resolution details
        post.setResolutionDescription(null);
        post.setResolvedAt(null);
        post.setSolved(false);

        // Clear contributing comments
        if (post.getContributingComments() != null) {
            post.getContributingComments().clear();
        }

        // Save the updated post
        postRepository.save(post);

        return true;
    }

    public Page<SearchResultDto> searchPosts(String keyword, Pageable pageable) {
        // Check if keyword contains commas
        if (keyword != null && keyword.contains(",")) {
            // Split by comma and trim whitespace
            List<String> keywords = Arrays.stream(keyword.split(","))
                    .map(String::trim)
                    .filter(kw -> !kw.isEmpty())
                    .collect(Collectors.toList());

            // Use a Map with Post ID as key to ensure uniqueness
            Map<Long, Post> uniquePosts = new HashMap<>();

            // Search for each keyword and add results to the map
            for (String kw : keywords) {
                Page<Post> keywordResults = postRepository.searchPosts(kw, Pageable.unpaged());
                for (Post post : keywordResults.getContent()) {
                    uniquePosts.put(post.getId(), post);
                }
            }

            // Convert map values to a list
            List<Post> allPosts = new ArrayList<>(uniquePosts.values());

            // Create a Page from the list using the original paging mechanism
            Page<Post> postsPage = new PageImpl<>(allPosts, pageable, allPosts.size());

            // Map to DTOs using your existing approach
            return postsPage.map(post -> {
                Set<String> tags = postRepository.findTagKeysByPostId(post.getId());
                SearchResultDto searchResultDto = new SearchResultDto(
                        post.getId(),
                        post.getUser().getUsername(),
                        post.getTitle(),
                        post.getDescription(),
                        post.getMysteryObject(),
                        post.isSolved()
                );
                searchResultDto.setTags(tags);
                return searchResultDto;
            });
        } else {
            // Original logic for single keyword
            Page<Post> postsPage = postRepository.searchPosts(keyword, pageable);
            return postsPage.map(post -> {
                Set<String> tags = postRepository.findTagKeysByPostId(post.getId());
                SearchResultDto searchResultDto = new SearchResultDto(
                        post.getId(),
                        post.getUser().getUsername(),
                        post.getTitle(),
                        post.getDescription(),
                        post.getMysteryObject(),
                        post.isSolved()
                );
                searchResultDto.setTags(tags);
                return searchResultDto;
            });
        }
    }

    private void mapPostToDto(Post post, Set<String> tags, PostDetailsDto postDetailsDto, User currentUser) {
        postDetailsDto.setId(post.getId());
        postDetailsDto.setAuthor(post.getUser().getUsername());
        postDetailsDto.setTitle(post.getTitle());
        postDetailsDto.setDescription(post.getDescription());
        postDetailsDto.setTags(tags);
        postDetailsDto.setMysteryObject(post.getMysteryObject() != null ? 
                                        MysteryObjectDto.fromEntity(post.getMysteryObject()) : null);
        postDetailsDto.setCreatedAt(post.getCreatedAt());
        postDetailsDto.setUpdatedAt(post.getUpdatedAt());
        postDetailsDto.setUpvotes(post.getUpvotesCount());
        postDetailsDto.setDownvotes(post.getDownvotesCount());
        postDetailsDto.setSolved(post.isSolved()); // Set solved status regardless of authentication

        // Add resolution information if post is solved (moved before auth check)
        if (post.isSolved()) {
            postDetailsDto.setResolutionDescription(post.getResolutionDescription());
            postDetailsDto.setResolvedAt(post.getResolvedAt());

            // Convert contributing comments to IDs
            if (post.getContributingComments() != null && !post.getContributingComments().isEmpty()) {
                List<Long> contributingCommentIds = post.getContributingComments().stream()
                        .map(Comment::getId)
                        .collect(Collectors.toList());
                postDetailsDto.setContributingCommentIds(contributingCommentIds);
            }
        }

        if (currentUser == null) {
            postDetailsDto.setUserUpvoted(false);
            postDetailsDto.setUserDownvoted(false);
            return; // Return after setting basic info + resolution (if solved)
        }

                // Check if the user has upvoted or downvoted the post (only for authenticated users)
        boolean userUpvoted = post.getUpvotedBy().contains(currentUser);
        boolean userDownvoted = post.getDownvotedBy().contains(currentUser);

        postDetailsDto.setUserUpvoted(userUpvoted);
        postDetailsDto.setUserDownvoted(userDownvoted);
        // Solved status is already set above
    }

    // New method to get all post details without media
    public List<PostDetailsNoMediaDto> getAllPostDetailsNoMedia(String username) {
        User currentUser = username != null ? userRepository.findByEmail(username).orElse(null) : null; // Allow null user
        List<Post> allPosts = postRepository.findAll(); // Fetch all posts

        List<PostDetailsNoMediaDto> results = new ArrayList<>();
        for (Post post : allPosts) {
            Set<String> tags = postRepository.findTagKeysByPostId(post.getId());
            PostDetailsNoMediaDto dto = new PostDetailsNoMediaDto();
            mapPostToNoMediaDto(post, tags, dto, currentUser); // Use the new mapping method
            results.add(dto);
        }
        return results;
    }

    // Helper method to map Post to PostDetailsNoMediaDto (without media)
    private void mapPostToNoMediaDto(Post post, Set<String> tags, PostDetailsNoMediaDto postDetailsDto, User currentUser) {
        postDetailsDto.setId(post.getId());
        postDetailsDto.setAuthor(post.getUser().getUsername());
        postDetailsDto.setTitle(post.getTitle());
        postDetailsDto.setDescription(post.getDescription());
        postDetailsDto.setTags(tags);
        postDetailsDto.setMysteryObject(post.getMysteryObject() != null ?
                MysteryObjectDto.fromEntity(post.getMysteryObject()) : null);
        postDetailsDto.setCreatedAt(post.getCreatedAt());
        postDetailsDto.setUpdatedAt(post.getUpdatedAt());
        postDetailsDto.setUpvotes(post.getUpvotesCount());
        postDetailsDto.setDownvotes(post.getDownvotesCount());
        postDetailsDto.setSolved(post.isSolved()); // Set solved status regardless of authentication

        // Add resolution information if post is solved
        if (post.isSolved()) {
            postDetailsDto.setResolutionDescription(post.getResolutionDescription());
            postDetailsDto.setResolvedAt(post.getResolvedAt());

            // Convert contributing comments to IDs
            if (post.getContributingComments() != null && !post.getContributingComments().isEmpty()) {
                List<Long> contributingCommentIds = post.getContributingComments().stream()
                        .map(Comment::getId)
                        .collect(Collectors.toList());
                postDetailsDto.setContributingCommentIds(contributingCommentIds);
            }
        }

        if (currentUser == null) {
            postDetailsDto.setUserUpvoted(false);
            postDetailsDto.setUserDownvoted(false);
        } else {
            // Check if the user has upvoted or downvoted the post
            boolean userUpvoted = post.getUpvotedBy().contains(currentUser);
            boolean userDownvoted = post.getDownvotedBy().contains(currentUser);
            postDetailsDto.setUserUpvoted(userUpvoted);
            postDetailsDto.setUserDownvoted(userDownvoted);
        }
        // No media file mapping here
    }


    public List<PostListDto> getUserPosts(Long userId) {
        List<Post> posts = postRepository.findByUserId(userId);

        return posts.stream()
                .map(post -> {
                    Set<String> tags = postRepository.findTagKeysByPostId(post.getId());
                    PostListDto postListDto = new PostListDto(
                            post.getId(),
                            post.getUser().getUsername(),
                            post.getTitle(),
                            post.getDescription(),
                            post.getMysteryObject() != null ? post.getMysteryObject().getImageUrl() : null,
                            post.isSolved(),
                            post.getUpvotesCount(),
                            post.getDownvotesCount(),
                            (long) post.getComments().size()
                    );

                    postListDto.setTags(tags);
                    postListDto.setCreatedAt(post.getCreatedAt());
                    return postListDto;
                })
                .collect(Collectors.toList());
    }

    /**
     * Queries the Wikidata API to get labels for the given entity IDs (tags)
     * @param tags A set of Wikidata entity IDs (e.g., "Q123", "Q456")
     * @return A set of corresponding human-readable labels
     */
    @SuppressWarnings("unused")
    private Set<String> fetchTagLabelsFromWikidata(Set<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return new HashSet<>();
        }

        RestTemplate restTemplate = new RestTemplate();
        ObjectMapper mapper = new ObjectMapper();
        Set<String> tagLabels = new HashSet<>();

        // Process each tag individually
        for (String tagId : tags) {
            try {
                String url = "https://www.wikidata.org/w/api.php?action=wbgetentities&ids=" + tagId +
                        "&props=labels&languages=en&format=json";

                String response = restTemplate.getForObject(url, String.class);
                JsonNode root = mapper.readTree(response);
                JsonNode entity = root.path("entities").path(tagId);

                if (!entity.isMissingNode()) {
                    JsonNode enLabel = entity.path("labels").path("en").path("value");
                    if (!enLabel.isMissingNode()) {
                        tagLabels.add(enLabel.asText());
                    } else {
                        // If English label is not available, use the tag ID as fallback
                        tagLabels.add(tagId);
                    }
                } else {
                    // Entity not found, use the tag ID as fallback
                    tagLabels.add(tagId);
                }
            } catch (Exception e) {
                // Log the error for this specific tag
                System.err.println("Error fetching label for tag " + tagId + ": " + e.getMessage());
                // Use the tag ID as fallback
                tagLabels.add(tagId);
            }
        }

        return tagLabels;
    }

    public MysteryObject getMysteryObjectById(Long id) {
        return mysteryObjectRepository.findById(id).orElse(null);
    }

    public MysteryObject saveMysteryObject(MysteryObject mysteryObject) {
        return mysteryObjectRepository.save(mysteryObject);
    }

    @Transactional
    public Map<String, Long> updatePost(Long postId, PostCreationDto postUpdateDto, String userName) {
        User user = userRepository.findByEmail(userName).orElseThrow();
        Post post = postRepository.findById(postId).orElseThrow();

        // Verify the user is the owner of the post
        if (!post.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You don't have permission to update this post");
        }

        // Update post details
        post.setTitle(postUpdateDto.getTitle());
        post.setDescription(postUpdateDto.getContent());
        post.setUpdatedAt(LocalDateTime.now());

        // Handle tags and their labels
        if (postUpdateDto.getTags() != null && !postUpdateDto.getTags().isEmpty()) {
            Map<String, String> tagMap = new HashMap<>();
            Set<String> tags = postUpdateDto.getTags();

            RestTemplate restTemplate = new RestTemplate();
            ObjectMapper mapper = new ObjectMapper();

            // Process each tag individually
            for (String tagId : tags) {
                try {
                    String url = "https://www.wikidata.org/w/api.php?action=wbgetentities&ids=" + tagId +
                            "&props=labels&languages=en&format=json";

                    String response = restTemplate.getForObject(url, String.class);
                    JsonNode root = mapper.readTree(response);
                    JsonNode entity = root.path("entities").path(tagId);

                    String label = tagId; // Default fallback
                    if (!entity.isMissingNode()) {
                        JsonNode enLabel = entity.path("labels").path("en").path("value");
                        if (!enLabel.isMissingNode()) {
                            label = enLabel.asText();
                        }
                    }

                    tagMap.put(tagId, label);
                } catch (Exception e) {
                    // Log the error and use the tag ID as fallback
                    System.err.println("Error fetching label for tag " + tagId + ": " + e.getMessage());
                    tagMap.put(tagId, tagId);
                }
            }

            post.setTagMap(tagMap);
        } else {
            post.setTagMap(new HashMap<>());
        }

        // Update mystery object if provided
        MysteryObject mysteryObject = post.getMysteryObject();
        MysteryObject updatedMysteryObject = postUpdateDto.getMysteryObject();

        if (updatedMysteryObject != null && mysteryObject != null) {
            // Preserve subparts before updating
            List<MysteryObject> existingSubParts = new ArrayList<>(mysteryObject.getSubParts());

            // Update mystery object fields
            mysteryObject.setDescription(updatedMysteryObject.getDescription());
            mysteryObject.setMaterial(updatedMysteryObject.getMaterial());
            mysteryObject.setWrittenText(updatedMysteryObject.getWrittenText());
            mysteryObject.setColor(updatedMysteryObject.getColor());
            mysteryObject.setShape(updatedMysteryObject.getShape());
            mysteryObject.setDescriptionOfParts(updatedMysteryObject.getDescriptionOfParts());
            mysteryObject.setLocation(updatedMysteryObject.getLocation());
            mysteryObject.setHardness(updatedMysteryObject.getHardness());
            mysteryObject.setTimePeriod(updatedMysteryObject.getTimePeriod());
            mysteryObject.setSmell(updatedMysteryObject.getSmell());
            mysteryObject.setTaste(updatedMysteryObject.getTaste());
            mysteryObject.setTexture(updatedMysteryObject.getTexture());
            mysteryObject.setValue(updatedMysteryObject.getValue());
            mysteryObject.setOriginOfAcquisition(updatedMysteryObject.getOriginOfAcquisition());
            mysteryObject.setPattern(updatedMysteryObject.getPattern());
            mysteryObject.setBrand(updatedMysteryObject.getBrand());
            mysteryObject.setPrint(updatedMysteryObject.getPrint());
            mysteryObject.setFunctionality(updatedMysteryObject.getFunctionality());
            mysteryObject.setImageLicenses(updatedMysteryObject.getImageLicenses());
            mysteryObject.setMarkings(updatedMysteryObject.getMarkings());
            mysteryObject.setHandmade(updatedMysteryObject.getHandmade());
            mysteryObject.setOneOfAKind(updatedMysteryObject.getOneOfAKind());
            mysteryObject.setSizeX(updatedMysteryObject.getSizeX());
            mysteryObject.setSizeY(updatedMysteryObject.getSizeY());
            mysteryObject.setSizeZ(updatedMysteryObject.getSizeZ());
            mysteryObject.setWeight(updatedMysteryObject.getWeight());
            mysteryObject.setItem_condition(updatedMysteryObject.getItem_condition());

            // SubParts are handled separately via MysteryObjectController endpoints
            // We're not overwriting them here, as they will be managed by separate API calls

            mysteryObjectRepository.save(mysteryObject);
        }

        Post savedPost = postRepository.save(post);

        Map<String, Long> response = new HashMap<>();
        response.put("postId", savedPost.getId());
        response.put("mysteryObjectId", mysteryObject.getId());

        return response;
    }
}
