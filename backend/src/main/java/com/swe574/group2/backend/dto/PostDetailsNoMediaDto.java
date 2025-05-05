package com.swe574.group2.backend.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
public class PostDetailsNoMediaDto {
    private Long id;
    private String author;
    private String title;
    private String description;
    private Set<String> tags;
    private MysteryObjectDto mysteryObject; // Keep the MysteryObject DTO
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    // mediaFiles field is intentionally omitted

    private int upvotes;
    private int downvotes;

    private boolean userUpvoted;
    private boolean userDownvoted;

    private boolean solved;
    private String resolutionDescription;
    private LocalDateTime resolvedAt;
    private List<Long> contributingCommentIds;

    // Using @Data, so explicit constructors might not be needed unless for specific mapping logic.
    // Keeping it simple for now.
    public PostDetailsNoMediaDto() {
    }
}
