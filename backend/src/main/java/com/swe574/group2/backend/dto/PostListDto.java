package com.swe574.group2.backend.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
public class PostListDto {
    private Long id;
    private String author;
    private String title;
    private String description;
    private Set<String> tags;
    private String mysteryObjectImageUrl;
    private boolean isSolved;
    private LocalDateTime createdAt;
    private int commentCount;
    private int upvotesCount;
    private int downvotesCount;

    public PostListDto(Long id, String author, String title, String description, String mysteryObjectImageUrl, boolean isSolved, int upvotesCount, int downvotesCount, Long commentCount) {
        this.id = id;
        this.author = author;
        this.title = title;
        this.description = description;
        this.mysteryObjectImageUrl = mysteryObjectImageUrl;
        this.isSolved = isSolved;
        this.upvotesCount = upvotesCount;
        this.downvotesCount = downvotesCount;
        this.commentCount = commentCount != null ? commentCount.intValue() : 0;
    }
    

    public PostListDto() {
    }
}
