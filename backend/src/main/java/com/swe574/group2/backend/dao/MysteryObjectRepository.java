package com.swe574.group2.backend.dao;

import com.swe574.group2.backend.entity.MysteryObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MysteryObjectRepository extends JpaRepository<MysteryObject, Long> {
    // Find mystery objects that have image URLs
    Page<MysteryObject> findByImageUrlIsNotNull(Pageable pageable);
}
