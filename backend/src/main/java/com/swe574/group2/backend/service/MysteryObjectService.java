package com.swe574.group2.backend.service;

import com.swe574.group2.backend.dao.MysteryObjectRepository;
import com.swe574.group2.backend.dto.MysteryObjectDto;
import com.swe574.group2.backend.entity.MysteryObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MysteryObjectService {

    private final MysteryObjectRepository mysteryObjectRepository;

    @Autowired
    public MysteryObjectService(MysteryObjectRepository mysteryObjectRepository) {
        this.mysteryObjectRepository = mysteryObjectRepository;
    }

    @Transactional
    public MysteryObjectDto addSubPart(Long parentId, MysteryObjectDto subPartDto) {
        MysteryObject parent = mysteryObjectRepository.findById(parentId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent mystery object not found with ID: " + parentId));
        
        MysteryObject subPart = subPartDto.toEntity();
        parent.addSubPart(subPart);
        
        mysteryObjectRepository.save(parent);
        return MysteryObjectDto.fromEntity(subPart);
    }

    @Transactional
    public List<MysteryObjectDto> getSubParts(Long parentId) {
        MysteryObject parent = mysteryObjectRepository.findById(parentId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent mystery object not found with ID: " + parentId));
        
        return parent.getSubParts().stream()
                .map(MysteryObjectDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public MysteryObjectDto getMysteryObjectWithSubParts(Long id) {
        MysteryObject mysteryObject = mysteryObjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mystery object not found with ID: " + id));
        
        return MysteryObjectDto.fromEntity(mysteryObject);
    }
    
    @Transactional
    public void removeSubPart(Long parentId, Long subPartId) {
        MysteryObject parent = mysteryObjectRepository.findById(parentId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent mystery object not found with ID: " + parentId));
        
        MysteryObject subPartToRemove = parent.getSubParts().stream()
                .filter(subPart -> subPart.getId().equals(subPartId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Sub-part not found with ID: " + subPartId));
        
        parent.removeSubPart(subPartToRemove);
        mysteryObjectRepository.save(parent);
    }
    
    @Transactional
    public MysteryObjectDto updateSubPart(Long parentId, Long subPartId, MysteryObjectDto updatedSubPartDto) {
        MysteryObject parent = mysteryObjectRepository.findById(parentId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent mystery object not found with ID: " + parentId));
        
        MysteryObject subPartToUpdate = parent.getSubParts().stream()
                .filter(subPart -> subPart.getId().equals(subPartId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Sub-part not found with ID: " + subPartId));
        
        // Update the sub-part fields
        updateMysteryObjectFields(subPartToUpdate, updatedSubPartDto);
        
        mysteryObjectRepository.save(parent);
        return MysteryObjectDto.fromEntity(subPartToUpdate);
    }
    
    private void updateMysteryObjectFields(MysteryObject target, MysteryObjectDto source) {
        if (source.getDescription() != null) target.setDescription(source.getDescription());
        if (source.getMaterial() != null) target.setMaterial(source.getMaterial());
        if (source.getWrittenText() != null) target.setWrittenText(source.getWrittenText());
        if (source.getColor() != null) target.setColor(source.getColor());
        if (source.getShape() != null) target.setShape(source.getShape());
        if (source.getDescriptionOfParts() != null) target.setDescriptionOfParts(source.getDescriptionOfParts());
        if (source.getLocation() != null) target.setLocation(source.getLocation());
        if (source.getHardness() != null) target.setHardness(source.getHardness());
        if (source.getTimePeriod() != null) target.setTimePeriod(source.getTimePeriod());
        if (source.getSmell() != null) target.setSmell(source.getSmell());
        if (source.getTaste() != null) target.setTaste(source.getTaste());
        if (source.getTexture() != null) target.setTexture(source.getTexture());
        if (source.getValue() != null) target.setValue(source.getValue());
        if (source.getOriginOfAcquisition() != null) target.setOriginOfAcquisition(source.getOriginOfAcquisition());
        if (source.getPattern() != null) target.setPattern(source.getPattern());
        if (source.getBrand() != null) target.setBrand(source.getBrand());
        if (source.getPrint() != null) target.setPrint(source.getPrint());
        if (source.getFunctionality() != null) target.setFunctionality(source.getFunctionality());
        if (source.getImageLicenses() != null) target.setImageLicenses(source.getImageLicenses());
        if (source.getMarkings() != null) target.setMarkings(source.getMarkings());
        if (source.getHandmade() != null) target.setHandmade(source.getHandmade());
        if (source.getOneOfAKind() != null) target.setOneOfAKind(source.getOneOfAKind());
        if (source.getSizeX() != null) target.setSizeX(source.getSizeX());
        if (source.getSizeY() != null) target.setSizeY(source.getSizeY());
        if (source.getSizeZ() != null) target.setSizeZ(source.getSizeZ());
        if (source.getWeight() != null) target.setWeight(source.getWeight());
        if (source.getItem_condition() != null) target.setItem_condition(source.getItem_condition());
    }
}
