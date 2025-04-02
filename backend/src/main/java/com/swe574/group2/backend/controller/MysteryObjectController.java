package com.swe574.group2.backend.controller;

import com.swe574.group2.backend.dto.MysteryObjectDto;
import com.swe574.group2.backend.service.MysteryObjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mysteryObjects")
public class MysteryObjectController {

    private final MysteryObjectService mysteryObjectService;

    @Autowired
    public MysteryObjectController(MysteryObjectService mysteryObjectService) {
        this.mysteryObjectService = mysteryObjectService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<MysteryObjectDto> getMysteryObjectWithSubParts(@PathVariable Long id) {
        return ResponseEntity.ok(mysteryObjectService.getMysteryObjectWithSubParts(id));
    }

    @GetMapping("/{id}/subParts")
    public ResponseEntity<List<MysteryObjectDto>> getSubParts(@PathVariable Long id) {
        return ResponseEntity.ok(mysteryObjectService.getSubParts(id));
    }

    @PostMapping("/{id}/subParts")
    public ResponseEntity<MysteryObjectDto> addSubPart(
            @PathVariable Long id,
            @RequestBody MysteryObjectDto subPartDto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mysteryObjectService.addSubPart(id, subPartDto));
    }

    @PutMapping("/{id}/subParts/{subPartId}")
    public ResponseEntity<MysteryObjectDto> updateSubPart(
            @PathVariable Long id,
            @PathVariable Long subPartId,
            @RequestBody MysteryObjectDto subPartDto) {
        return ResponseEntity.ok(mysteryObjectService.updateSubPart(id, subPartId, subPartDto));
    }

    @DeleteMapping("/{id}/subParts/{subPartId}")
    public ResponseEntity<Void> removeSubPart(
            @PathVariable Long id,
            @PathVariable Long subPartId) {
        mysteryObjectService.removeSubPart(id, subPartId);
        return ResponseEntity.noContent().build();
    }
}
