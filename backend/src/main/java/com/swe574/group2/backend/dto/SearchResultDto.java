package com.swe574.group2.backend.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

import com.swe574.group2.backend.entity.MysteryObject;
import com.swe574.group2.backend.dto.MysteryObjectDto;

@Data
public class SearchResultDto {
    private Long id;
    private String author;
    private String title;
    private String description;
    private Set<String> tags;
    private MysteryObjectDto mysteryObject;
    private boolean isSolved;
    private LocalDateTime createdAt;
    private int upvotesCount;
    private int downvotesCount;

    public SearchResultDto(Long id, String author, String title, String description, MysteryObject mysteryObject, boolean isSolved) {
        this.id = id;
        this.author = author;
        this.title = title;
        this.description = description;
        this.mysteryObject = mysteryObject != null ? MysteryObjectDto.fromEntity(mysteryObject) : null;
        this.isSolved = isSolved;
        this.upvotesCount = 0;
        this.downvotesCount = 0;
    }

    public SearchResultDto() {
        this.upvotesCount = 0;
        this.downvotesCount = 0;
    }
}
