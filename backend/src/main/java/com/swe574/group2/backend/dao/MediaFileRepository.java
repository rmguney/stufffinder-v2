package com.swe574.group2.backend.dao;

import com.swe574.group2.backend.entity.MediaFile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MediaFileRepository extends JpaRepository<MediaFile, Long> {
    List<MediaFile> findByMysteryObjectId(Long mysteryObjectId);
}