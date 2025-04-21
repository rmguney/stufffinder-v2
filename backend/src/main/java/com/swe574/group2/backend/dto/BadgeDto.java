package com.swe574.group2.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BadgeDto {
    private Long id;
    private String name;
    private String description;
    private String iconUrl;
}
