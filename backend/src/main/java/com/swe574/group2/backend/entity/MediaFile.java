package com.swe574.group2.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "media_file")
public class MediaFile {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "mystery_object_id", nullable = true)
    private MysteryObject mysteryObject;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "comment_id", nullable = true)
    private Comment comment;
    
    @Column(nullable = false)
    private String fileName;
    
    @Column(nullable = false)
    private String fileType;
    
    @Column(nullable = false)
    private String fileUrl;
    
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "resolution_id", nullable = true)
    private Resolution resolution;
}
