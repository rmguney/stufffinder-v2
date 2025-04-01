package com.swe574.group2.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Table(name = "mystery_object")
public class MysteryObject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "description")
    private String description;

    @Column(name = "material")
    private String material;

    @Column(name = "written_text")
    private String writtenText;

    @Column(name = "color")
    private String color;

    @Column(name = "shape")
    private String shape;

    @Column(name = "description_of_parts")
    private String descriptionOfParts;

    @Column(name = "location")
    private String location;

    @Column(name = "hardness")
    private String hardness;

    @Column(name = "time_period")
    private String timePeriod;

    @Column(name = "smell")
    private String smell;

    @Column(name = "taste")
    private String taste;

    @Column(name = "texture")
    private String texture;

    @Column(name = "value")
    private Double value;

    @Column(name = "origin_of_acquisition")
    private String originOfAcquisition;

    @Column(name = "pattern")
    private String pattern;

    @Column(name = "brand")
    private String brand;

    @Column(name = "print")
    private String print;

    @Column(name = "functionality")
    private String functionality;

    @Column(name = "image_licenses")
    private String imageLicenses;

    @Column(name = "markings")
    private String markings;

    @Column(name = "handmade")
    private Boolean handmade;

    @Column(name = "one_of_a_kind")
    private Boolean oneOfAKind;

    @Column(name = "size_x")
    private Double sizeX;

    @Column(name = "size_y")
    private Double sizeY;

    @Column(name = "size_z")
    private Double sizeZ;

    @Column(name = "weight")
    private Double weight;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_condition")
    private Condition item_condition;

    // Enum for Condition
    public enum Condition {
        NEW,
        LIKE_NEW,
        USED,
        DAMAGED,
        ANTIQUE
    }

    @Column(name = "image_url")
    private String imageUrl;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private MysteryObject parent;
    
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private List<MysteryObject> subParts = new ArrayList<>();
    
    // Helper methods for managing relationships
    public void addSubPart(MysteryObject subPart) {
        subParts.add(subPart);
        subPart.setParent(this);
    }
    
    public void removeSubPart(MysteryObject subPart) {
        subParts.remove(subPart);
        subPart.setParent(null);
    }
}
