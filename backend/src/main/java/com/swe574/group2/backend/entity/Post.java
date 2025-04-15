package com.swe574.group2.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Entity
@Table(name = "posts")
@Data
@EqualsAndHashCode(exclude = {"comments", "upvotedBy", "downvotedBy", "contributingComments"})
@ToString(exclude = {"comments", "upvotedBy", "downvotedBy", "contributingComments"})
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000) // Optional description with a limit
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "mystery_object_id", referencedColumnName = "id")
    private MysteryObject mysteryObject;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments;

    @ElementCollection
    @CollectionTable(name = "post_tags", joinColumns = @JoinColumn(name = "post_id"))
    @MapKeyColumn(name = "tag")
    @Column(name = "tag_label")
    private Map<String, String> tagMap = new HashMap<>();

    @Column(nullable = false)
    private int upvotesCount = 0;

    @Column(nullable = false)
    private int downvotesCount = 0;

    @ManyToMany
    @JoinTable(
            name = "post_upvotes",
            joinColumns = @JoinColumn(name = "post_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> upvotedBy;

    @ManyToMany
    @JoinTable(
            name = "post_downvotes",
            joinColumns = @JoinColumn(name = "post_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> downvotedBy;

    @Column(length = 1000)
    private String resolutionDescription;
    
    @Column(nullable = true)
    private LocalDateTime resolvedAt;
    
    @ManyToMany
    @JoinTable(
            name = "post_contributing_comments",
            joinColumns = @JoinColumn(name = "post_id"),
            inverseJoinColumns = @JoinColumn(name = "comment_id")
    )
    private Set<Comment> contributingComments;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(nullable = false)
    private boolean solved = false;
}
