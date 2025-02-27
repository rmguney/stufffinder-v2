package com.swe574.group2.backend.dto;

import com.swe574.group2.backend.entity.MysteryObject;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
public class PostDetailsDto {
    private Long id;
    private String author;
    private String title;
    private String description;
    private Set<String> tags;
    private MysteryObject mysteryObject;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private int upvotes;
    private int downvotes;

    private boolean userUpvoted;
    private boolean userDownvoted;

    private boolean solved;

    public PostDetailsDto(Long id, String author, String title, String description, Set<String> tags, 
                            MysteryObject mysteryObject, LocalDateTime createdAt, LocalDateTime updatedAt, int upvotes, 
                            int downvotes, boolean userUpvoted, boolean userDownvoted, boolean solved) {
        this.id = id;
        this.author = author;
        this.title = title;
        this.description = description;
        this.tags = tags;
        this.mysteryObject = mysteryObject;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.upvotes = upvotes;
        this.downvotes = downvotes;
        this.userUpvoted = userUpvoted;
        this.userDownvoted = userDownvoted;
        this.solved = solved;
    }

    public PostDetailsDto() {
    }
}
