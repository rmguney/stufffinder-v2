package com.swe574.group2.backend.dao;

import com.swe574.group2.backend.entity.MysteryObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MysteryObjectRepository extends JpaRepository<MysteryObject, Long> {
    // Migration method
    Page<MysteryObject> findByImageIsNotNull(Pageable pageable);
    
    // Recovery method
    Page<MysteryObject> findByImageUrlIsNotNullAndImageIsNull(Pageable pageable);
    
    // Additional query to find objects that still need recovery
    @Query("SELECT COUNT(mo) FROM MysteryObject mo WHERE mo.imageUrl IS NOT NULL AND mo.image IS NULL")
    Long countObjectsNeedingRecovery();
}