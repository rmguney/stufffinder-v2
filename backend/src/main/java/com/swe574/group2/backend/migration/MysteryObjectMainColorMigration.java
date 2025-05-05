package com.swe574.group2.backend.migration;

import com.swe574.group2.backend.entity.MysteryObject;
import com.swe574.group2.backend.dao.MysteryObjectRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import java.util.Map;

@Component
public class MysteryObjectMainColorMigration implements CommandLineRunner {

    @Autowired
    private MysteryObjectRepository mysteryObjectRepository;

    // Main color mapping utility
    private static final Map<String, String> MAIN_COLORS = Map.of(
        "red", "#FF0000",
        "green", "#00FF00",
        "blue", "#0000FF",
        "yellow", "#FFFF00",
        "orange", "#FFA500",
        "purple", "#800080",
        "brown", "#A52A2A",
        "black", "#000000",
        "white", "#FFFFFF",
        "gray", "#808080"
    );

    private static int[] hexToRgb(String hex) {
        if (hex == null) return new int[]{0,0,0};
        hex = hex.replace("#", "");
        if (hex.length() == 3) {
            hex = "" + hex.charAt(0) + hex.charAt(0)
                    + hex.charAt(1) + hex.charAt(1)
                    + hex.charAt(2) + hex.charAt(2);
        }
        try {
            int r = Integer.parseInt(hex.substring(0, 2), 16);
            int g = Integer.parseInt(hex.substring(2, 4), 16);
            int b = Integer.parseInt(hex.substring(4, 6), 16);
            return new int[]{r, g, b};
        } catch (Exception e) {
            return new int[]{0,0,0};
        }
    }

    private static String getMainColorHex(String hex) {
        int[] rgb = hexToRgb(hex);
        String closest = null;
        double minDist = Double.MAX_VALUE;
        for (Map.Entry<String, String> entry : MAIN_COLORS.entrySet()) {
            int[] mainRgb = hexToRgb(entry.getValue());
            double dist = Math.pow(rgb[0] - mainRgb[0], 2)
                        + Math.pow(rgb[1] - mainRgb[1], 2)
                        + Math.pow(rgb[2] - mainRgb[2], 2);
            if (dist < minDist) {
                minDist = dist;
                closest = entry.getValue();
            }
        }
        return closest;
    }

    private static void assignMainColorRecursive(MysteryObject obj) {
        if (obj == null) return;
        obj.setMainColor(getMainColorHex(obj.getColor()));
    }

    @Override
    public void run(String... args) {
        List<MysteryObject> allObjects = mysteryObjectRepository.findAll();
        int updated = 0;
        for (MysteryObject obj : allObjects) {
            String before = obj.getMainColor();
            String originalColor = obj.getColor();
            assignMainColorRecursive(obj);
            String after = obj.getMainColor();
            System.out.println("[MIGRATION] id=" + obj.getId() + " color=" + originalColor + " mainColor(before)=" + before + " -> mainColor(after)=" + after);
            if (before == null || !before.equals(after)) {
                updated++;
            }
            mysteryObjectRepository.save(obj);
        }
        System.out.println("[MIGRATION] Migration complete: mainColor set for all MysteryObjects. Total updated: " + updated);
    }
}
