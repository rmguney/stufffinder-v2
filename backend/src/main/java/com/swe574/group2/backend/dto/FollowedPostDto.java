package com.swe574.group2.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FollowedPostDto {
    private Long id;
    private Long followerId;
    private Long postId;
    private String followerUsername;
}
