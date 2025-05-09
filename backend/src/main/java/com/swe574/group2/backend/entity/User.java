package com.swe574.group2.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import com.swe574.group2.backend.entity.Role;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
// Import Set if needed, already imported

@Entity
@Table(name = "users")
@Data
@EqualsAndHashCode(exclude = { "createdPosts", "comments", "upvotedPosts", "downvotedPosts", "upvotedComments",
                "downvotedComments", "notifications", "userBadges", "followers", "following" })
@ToString(exclude = { "createdPosts", "comments", "upvotedPosts", "downvotedPosts", "upvotedComments",
                "downvotedComments", "notifications", "userBadges", "followers", "following" })
public class User {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(unique = true, nullable = false)
        private String username;

        @Column(unique = true, nullable = false)
        private String email;

        @Column(nullable = false)
        private String password; // This should be stored as a hashed value

        @Enumerated(EnumType.STRING)
        @Column(nullable = false, columnDefinition = "VARCHAR(20)")
        private Role role = Role.USER;

        @Column(length = 500)
        private String bio;

        private String profilePictureUrl;

        @Column(length = 255) // Store as JSON string, e.g., ["TR", "US"]
        private String location;

        @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
        private Set<UserBadge> userBadges;

        @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
        private List<Post> createdPosts;

        @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
        private List<Comment> comments;

        @ManyToMany
        @JoinTable(name = "user_upvoted_posts", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "post_id"))
        private Set<Post> upvotedPosts;

        @ManyToMany
        @JoinTable(name = "user_downvoted_posts", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "post_id"))
        private Set<Post> downvotedPosts;

        @ManyToMany
        @JoinTable(name = "user_upvoted_comments", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "comment_id"))
        private Set<Comment> upvotedComments;

        @ManyToMany
        @JoinTable(name = "user_downvoted_comments", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "comment_id"))
        private Set<Comment> downvotedComments;

        @Column(nullable = false)
        private Boolean receiveNotifications = true;

        @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
        private List<Notification> notifications;

        @Column(nullable = false, updatable = false)
        private LocalDateTime createdAt = LocalDateTime.now();

        @Column(nullable = false)
        private LocalDateTime updatedAt = LocalDateTime.now();

        @ManyToMany
        @JoinTable(name = "user_following", joinColumns = @JoinColumn(name = "follower_id"), inverseJoinColumns = @JoinColumn(name = "following_id"))
        private Set<User> following; // users this user is following

        @ManyToMany(mappedBy = "following")
        private Set<User> followers; // users who follow this user
}
