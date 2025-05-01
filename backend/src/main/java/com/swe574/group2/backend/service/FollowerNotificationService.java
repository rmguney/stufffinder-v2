package com.swe574.group2.backend.service;

import com.swe574.group2.backend.dao.FollowedUserRepository;
import com.swe574.group2.backend.dao.FollowerNotificationRepository;
import com.swe574.group2.backend.dao.PostRepository;
import com.swe574.group2.backend.dao.UserRepository;
import com.swe574.group2.backend.dto.FollowerNotificationDto;
import com.swe574.group2.backend.entity.Comment;
import com.swe574.group2.backend.entity.FollowedUser;
import com.swe574.group2.backend.entity.FollowerNotification;
import com.swe574.group2.backend.entity.Post;
import com.swe574.group2.backend.entity.User;
import com.swe574.group2.backend.enums.FollowerNotificationType;

import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FollowerNotificationService {

    private final FollowerNotificationRepository followerNotificationRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final FollowedUserRepository followedUserRepository;

    public FollowerNotificationService(FollowerNotificationRepository followerNotificationRepository,
            UserRepository userRepository,
            PostRepository postRepository,
            FollowedUserRepository followedUserRepository) {
        this.followerNotificationRepository = followerNotificationRepository;
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.followedUserRepository = followedUserRepository;
    }

    // Send notification when a followed user creates a post
    public void sendFollowedUserPostCreatedNotificationToAll(String actorName, Long postId) {
    User actor = userRepository.findByEmail(actorName).orElseThrow();
    Post post = postRepository.findById(postId).orElseThrow();

    List<FollowedUser> followRelations = followedUserRepository.findByFollowed(actor);

    if (followRelations.isEmpty()) {
        System.out.println("No followers found for user: " + actorName);
        return;
    }

    for (FollowedUser relation : followRelations) {
        User follower = relation.getFollower();

        FollowerNotification notification = new FollowerNotification();
        notification.setUser(follower);
        notification.setActor(actor);
        notification.setPost(post);
        notification.setType(FollowerNotificationType.FOLLOWED_USER_POST_CREATED);
        notification.setMessage(actor.getUsername() + " created a new post you might like!");

        followerNotificationRepository.save(notification);
    }
}


    public void sendFollowedPostCommentNotification(Post post, User actor, Comment comment) {
        if (post.getFollowers() == null || post.getFollowers().isEmpty()) {
            System.out.println("No followers found for post id: " + post.getId());
            return;
        }

        for (User follower : post.getFollowers()) {
            // Prevent sending notification to the author of the comment
            if (follower.getId().equals(actor.getId()))
                continue;

            FollowerNotification notification = new FollowerNotification();
            notification.setUser(follower);
            notification.setActor(actor);
            notification.setPost(post);
            notification.setComment(comment);
            notification.setType(FollowerNotificationType.POST_COMMENTED);
            notification.setMessage(actor.getUsername() + " commented on a post you follow!");

            followerNotificationRepository.save(notification);
        }
    }

    public void sendFollowedPostActivityNotification(Post post, FollowerNotificationType type) {
        Set<User> followers = post.getFollowers();

        if (followers == null || followers.isEmpty()) {
            System.out.println("No followers found for post: " + post.getId());
            return;
        }

        for (User follower : followers) {
            FollowerNotification notification = new FollowerNotification();
            notification.setUser(follower);
            notification.setActor(post.getUser()); // Post creator as actor
            notification.setPost(post);
            notification.setType(type);

            switch (type) {
                case POST_UPVOTED:
                    notification.setMessage("A post you follow was upvoted!");
                    break;
                case POST_DOWNVOTED:
                    notification.setMessage("A post you follow was downvoted!");
                    break;
                case POST_RESOLVED:
                    notification.setMessage("A post you follow was marked as resolved!");
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported notification type: " + type);
            }

            followerNotificationRepository.save(notification);
        }
    }

    // Get all follower notifications for a user
    public List<FollowerNotificationDto> getFollowerNotifications(Long userId) {
        return followerNotificationRepository.findByUserIdAndIsReadFalse(userId)
                .stream()
                .map(notification -> {
                    FollowerNotificationDto dto = new FollowerNotificationDto();
                    dto.setId(notification.getId());
                    dto.setMessage(notification.getMessage());
                    dto.setRead(notification.isRead());
                    dto.setCreatedAt(notification.getCreatedAt());
                    dto.setUserId(notification.getUser().getId());
                    dto.setActorId(notification.getActor().getId());
                    if (notification.getPost() != null) {
                        dto.setPostId(notification.getPost().getId());
                    }
                    if (notification.getComment() != null) {
                        dto.setCommentId(notification.getComment().getId());
                    }
                    dto.setType(notification.getType());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    // Mark a follower notification as read
    public void markFollowerNotificationAsRead(Long notificationId) {
        FollowerNotification notification = followerNotificationRepository.findById(notificationId).orElseThrow();

        notification.setRead(true);
        followerNotificationRepository.save(notification);
    }
}
