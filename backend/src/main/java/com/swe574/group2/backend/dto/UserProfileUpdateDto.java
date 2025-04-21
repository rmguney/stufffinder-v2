package com.swe574.group2.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List; // Import List for location

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileUpdateDto {
    private String bio;
    private List<String> location; // Expecting a list of country codes, e.g., ["TR", "US"]
}
