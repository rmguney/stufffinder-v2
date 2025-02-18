package com.swe574.group2.backend.dao;

import com.swe574.group2.backend.entity.MysteryObject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.web.bind.annotation.CrossOrigin;

@CrossOrigin(origins = {"http://localhost:4200", "https://swe573-frontend-594781402587.us-central1.run.app"})
public interface MysteryObjectRepository extends JpaRepository<MysteryObject, Long> {
}