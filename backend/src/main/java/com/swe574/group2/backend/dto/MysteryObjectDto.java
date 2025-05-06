package com.swe574.group2.backend.dto;

import com.swe574.group2.backend.entity.MysteryObject;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class MysteryObjectDto {
    private Long id;
    private String description;
    private String material;
    private String writtenText;
    private String color;
    private String mainColor;
    private String shape;
    private String descriptionOfParts;
    private String location;
    private String hardness;
    private String timePeriod;
    private String smell;
    private String taste;
    private String texture;
    private Double value;
    private String originOfAcquisition;
    private String pattern;
    private String brand;
    private String print;
    private String functionality;
    private String imageLicenses;
    private String markings;
    private Boolean handmade;
    private Boolean oneOfAKind;
    private Double sizeX;
    private Double sizeY;
    private Double sizeZ;
    private Double weight;
    private MysteryObject.Condition item_condition;
    private String imageUrl;
    private List<MysteryObjectDto> subParts = new ArrayList<>();
    
    // Static method to convert from entity to DTO
    public static MysteryObjectDto fromEntity(MysteryObject mysteryObject) {
        if (mysteryObject == null) {
            return null;
        }
        
        MysteryObjectDto dto = new MysteryObjectDto();
        dto.setId(mysteryObject.getId());
        dto.setDescription(mysteryObject.getDescription());
        dto.setMaterial(mysteryObject.getMaterial());
        dto.setWrittenText(mysteryObject.getWrittenText());
        dto.setColor(mysteryObject.getColor());
        dto.setMainColor(mysteryObject.getMainColor());
        dto.setShape(mysteryObject.getShape());
        dto.setDescriptionOfParts(mysteryObject.getDescriptionOfParts());
        dto.setLocation(mysteryObject.getLocation());
        dto.setHardness(mysteryObject.getHardness());
        dto.setTimePeriod(mysteryObject.getTimePeriod());
        dto.setSmell(mysteryObject.getSmell());
        dto.setTaste(mysteryObject.getTaste());
        dto.setTexture(mysteryObject.getTexture());
        dto.setValue(mysteryObject.getValue());
        dto.setOriginOfAcquisition(mysteryObject.getOriginOfAcquisition());
        dto.setPattern(mysteryObject.getPattern());
        dto.setBrand(mysteryObject.getBrand());
        dto.setPrint(mysteryObject.getPrint());
        dto.setFunctionality(mysteryObject.getFunctionality());
        dto.setImageLicenses(mysteryObject.getImageLicenses());
        dto.setMarkings(mysteryObject.getMarkings());
        dto.setHandmade(mysteryObject.getHandmade());
        dto.setOneOfAKind(mysteryObject.getOneOfAKind());
        dto.setSizeX(mysteryObject.getSizeX());
        dto.setSizeY(mysteryObject.getSizeY());
        dto.setSizeZ(mysteryObject.getSizeZ());
        dto.setWeight(mysteryObject.getWeight());
        dto.setItem_condition(mysteryObject.getItem_condition());
        dto.setImageUrl(mysteryObject.getImageUrl());
        
        // Convert sub-parts recursively
        if (mysteryObject.getSubParts() != null && !mysteryObject.getSubParts().isEmpty()) {
            dto.setSubParts(mysteryObject.getSubParts().stream()
                    .map(MysteryObjectDto::fromEntity)
                    .collect(Collectors.toList()));
        }
        
        return dto;
    }
    
    // Convert DTO to entity
    public MysteryObject toEntity() {
        MysteryObject entity = new MysteryObject();
        entity.setId(this.id);
        entity.setDescription(this.description);
        entity.setMaterial(this.material);
        entity.setWrittenText(this.writtenText);
        entity.setColor(this.color);
        entity.setMainColor(this.mainColor);
        entity.setShape(this.shape);
        entity.setDescriptionOfParts(this.descriptionOfParts);
        entity.setLocation(this.location);
        entity.setHardness(this.hardness);
        entity.setTimePeriod(this.timePeriod);
        entity.setSmell(this.smell);
        entity.setTaste(this.taste);
        entity.setTexture(this.texture);
        entity.setValue(this.value);
        entity.setOriginOfAcquisition(this.originOfAcquisition);
        entity.setPattern(this.pattern);
        entity.setBrand(this.brand);
        entity.setPrint(this.print);
        entity.setFunctionality(this.functionality);
        entity.setImageLicenses(this.imageLicenses);
        entity.setMarkings(this.markings);
        entity.setHandmade(this.handmade);
        entity.setOneOfAKind(this.oneOfAKind);
        entity.setSizeX(this.sizeX);
        entity.setSizeY(this.sizeY);
        entity.setSizeZ(this.sizeZ);
        entity.setWeight(this.weight);
        entity.setItem_condition(this.item_condition);
        entity.setImageUrl(this.imageUrl);
        
        // Sub-parts will be handled separately to avoid circular references
        
        return entity;
    }
}
