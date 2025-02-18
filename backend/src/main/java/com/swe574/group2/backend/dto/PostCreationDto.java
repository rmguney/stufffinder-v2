package com.swe574.group2.backend.dto;

import com.swe574.group2.backend.entity.MysteryObject;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@Data
public class PostCreationDto {
    private String title;
    private String content;
    private Set<String> tags;
    private MultipartFile image;
    private MysteryObject mysteryObject;
}
