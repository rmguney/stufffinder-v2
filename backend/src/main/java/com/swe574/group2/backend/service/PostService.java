package com.swe574.group2.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swe574.group2.backend.dao.CommentRepository;
import com.swe574.group2.backend.dao.MysteryObjectRepository;
import com.swe574.group2.backend.dao.PostRepository;
import com.swe574.group2.backend.dao.UserRepository;
import com.swe574.group2.backend.dto.PostCreationDto;
import com.swe574.group2.backend.dto.PostDetailsDto;
import com.swe574.group2.backend.dto.PostListDto;
import com.swe574.group2.backend.entity.Comment;
import com.swe574.group2.backend.entity.MysteryObject;
import com.swe574.group2.backend.entity.Post;
import com.swe574.group2.backend.entity.User;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.stream.Collectors;

import java.util.*;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final MysteryObjectRepository mysteryObjectRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final NotificationService notificationService;

    public PostService(PostRepository postRepository, MysteryObjectRepository mysteryObjectRepository, UserRepository userRepository, CommentRepository commentRepository, NotificationService notificationService) {
        this.postRepository = postRepository;
        this.mysteryObjectRepository = mysteryObjectRepository;
        this.userRepository = userRepository;
        this.commentRepository = commentRepository;
        this.notificationService = notificationService;
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

        if (mysteryObject.getImage() != null) {
            // Decode the Base64 image
            mysteryObject.setImage(Base64.getDecoder().decode(mysteryObject.getImage()));
        }

        mysteryObjectRepository.save(mysteryObject);
        post.setMysteryObject(mysteryObject);

        Post savedPost = postRepository.save(post);

        Map<String, Long> response = new HashMap<>();
        response.put("postId", savedPost.getId());
        response.put("mysteryObjectId", mysteryObject.getId());

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
        }

        postRepository.save(post);

        Map<String, Long> response = new HashMap<>();
        response.put("postId", post.getId());

        return response;
    }

    public PostDetailsDto getPostDetails(Long postId, String email) {
        User user = email != null ? userRepository.findByEmail(email).orElseThrow() : null;
        Post post = postRepository.findPostDetailsById(postId);
        if (post == null) {
            throw new ResourceNotFoundException("Post not found with ID: " + postId);
        }

        // Use tag keys (Q-IDs) for consistency
        Set<String> tags = postRepository.findTagKeysByPostId(postId);

        PostDetailsDto postDetailsDto = new PostDetailsDto();
        mapPostToDto(post, tags, postDetailsDto, user);

        return postDetailsDto;
    }

    public boolean markBestAnswer(Long postId, Long commentId, String username) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new IllegalArgumentException("Post not found."));
        Comment comment = commentRepository.findById(commentId).orElseThrow(() -> new IllegalArgumentException("Comment not found."));

        // Check if the logged-in user is the creator of the post
        if (!post.getUser().getEmail().equals(username)) {
            return false; // Not authorized
        }

        // Mark the comment as the best answer
        if (post.getBestAnswer() != null) {
            // Reset the previous best answer
            Comment previousBestAnswer = post.getBestAnswer();
            previousBestAnswer.setBestAnswer(false);
            commentRepository.save(previousBestAnswer);
        }

        comment.setBestAnswer(true);
        post.setBestAnswer(comment);
        post.setSolved(true);

        // Save the updated entities
        commentRepository.save(comment);
        postRepository.save(post);

        // Send a notification to the comment author
        notificationService.sendBestAnswerNotification(comment.getUser().getId(), post, comment);

        return true;
    }

    public Page<PostListDto> searchPosts(String keyword, Pageable pageable) {
        Page<Post> postsPage = postRepository.searchPosts(keyword, pageable);
        return postsPage.map(post -> {
            Set<String> tags = postRepository.findTagKeysByPostId(post.getId());
            PostListDto postListDto = new PostListDto(post.getId(), post.getUser().getUsername(),
                    post.getTitle(), post.getDescription(), post.getMysteryObject().getImage(), post.isSolved());
            postListDto.setTags(tags);
            return postListDto;
        });
    }

    private void mapPostToDto(Post post, Set<String> tags, PostDetailsDto postDetailsDto, User currentUser) {
        postDetailsDto.setId(post.getId());
        postDetailsDto.setAuthor(post.getUser().getUsername());
        postDetailsDto.setTitle(post.getTitle());
        postDetailsDto.setDescription(post.getDescription());
        postDetailsDto.setTags(tags);
        postDetailsDto.setMysteryObject(post.getMysteryObject());
        postDetailsDto.setCreatedAt(post.getCreatedAt());
        postDetailsDto.setUpdatedAt(post.getUpdatedAt());
        postDetailsDto.setUpvotes(post.getUpvotesCount());
        postDetailsDto.setDownvotes(post.getDownvotesCount());

        if (currentUser == null) {
            postDetailsDto.setUserUpvoted(false);
            postDetailsDto.setUserDownvoted(false);
            return;
        }

        // Check if the user has upvoted or downvoted the post
        boolean userUpvoted = post.getUpvotedBy().contains(currentUser);
        boolean userDownvoted = post.getDownvotedBy().contains(currentUser);

        postDetailsDto.setUserUpvoted(userUpvoted);
        postDetailsDto.setUserDownvoted(userDownvoted);
        postDetailsDto.setSolved(post.getBestAnswer() != null);
    }

    public List<PostListDto> getUserPosts(Long userId) {
        List<Post> posts = postRepository.findByUserId(userId);

        return posts.stream()
                .map(post -> {
                    Set<String> tags = postRepository.findTagKeysByPostId(post.getId());
                    PostListDto postListDto = new PostListDto(post.getId(), post.getUser().getUsername(),
                            post.getTitle(), post.getDescription(), post.getMysteryObject().getImage(), post.isSolved());
                    postListDto.setTags(tags);
                    return postListDto;
                })
                .collect(Collectors.toList());
    }

    /**
     * Queries the Wikidata API to get labels for the given entity IDs (tags)
     * @param tags A set of Wikidata entity IDs (e.g., "Q123", "Q456")
     * @return A set of corresponding human-readable labels
     */
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
}
