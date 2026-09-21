package com.proyecto.rpg.ui;

import com.proyecto.rpg.model.Race;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/** Resuelve la foto de raza (/sprites/avatars/<raza>.jpg, fallback default.png). */
public final class RaceAvatarLoader {

    private static final Map<String, Image> CACHE = new HashMap<>();

    private RaceAvatarLoader() {}

    public static Image load(Race race) {
        String raceName = race != null ? race.getName() : "default";
        String path = "/sprites/avatars/" + normalize(raceName) + ".jpg";

        if (CACHE.containsKey(path)) return CACHE.get(path);

        Image image = readOrNull(path);
        if (image == null) image = readOrNull("/sprites/avatars/default.png");

        CACHE.put(path, image);
        return image;
    }

    public static ImageView iconFor(Race race, double size) {
        ImageView view = new ImageView(load(race));
        view.setFitWidth(size);
        view.setFitHeight(size);
        view.setPreserveRatio(true);
        view.setSmooth(false);
        return view;
    }

    private static Image readOrNull(String path) {
        try (InputStream in = RaceAvatarLoader.class.getResourceAsStream(path)) {
            return in != null ? new Image(in) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static String normalize(String name) {
        return name.toLowerCase()
                .replace("á", "a").replace("é", "e").replace("í", "i")
                .replace("ó", "o").replace("ú", "u")
                .replace(" ", "_");
    }
}