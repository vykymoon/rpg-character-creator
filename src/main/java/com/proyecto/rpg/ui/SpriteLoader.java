package com.proyecto.rpg.ui;

import com.proyecto.rpg.model.Outfit;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Carga los sprites de vestuario desde el classpath y devuelve un nodo
 * listo para poner en la UI.
 *
 * La regla importante: si el PNG no existe o está corrupto, esto NO
 * revienta ni deja un hueco vacío — devuelve un recuadro de color con la
 * inicial de la pieza. Así el vestuario se puede seguir usando aunque
 * falte el arte definitivo, que es exactamente la situación del proyecto.
 *
 * Uso: contenedor.getChildren().add(SpriteLoader.iconFor(outfit, 36));
 */
public final class SpriteLoader {

    public static final double DEFAULT_SIZE = 40;

    /**
     * Caché de imágenes ya resueltas. Guarda también los fallos (valor
     * null) para no reintentar leer un archivo inexistente en cada celda
     * que la ListView vuelve a dibujar.
     */
    private static final Map<String, Image> CACHE = new HashMap<>();

    private SpriteLoader() {
        // Clase de utilidad: no se instancia.
    }

    public static Node iconFor(Outfit outfit) {
        return iconFor(outfit, DEFAULT_SIZE);
    }

    public static Node iconFor(Outfit outfit, double size) {
        if (outfit == null) {
            return placeholder("?", null, size);
        }

        Image image = load(outfit.getSpriteRef());
        if (image != null) {
            ImageView view = new ImageView(image);
            view.setFitWidth(size);
            view.setFitHeight(size);
            view.setPreserveRatio(true);
            view.setSmooth(false); // pixel art: sin suavizado se ve nítido
            view.getStyleClass().add("outfit-sprite");
            return view;
        }

        return placeholder(initialOf(outfit.getName()), outfit.getSlot(), size);
    }

    /** true si la pieza tiene un sprite real disponible. */
    public static boolean hasSprite(Outfit outfit) {
        return outfit != null && load(outfit.getSpriteRef()) != null;
    }

    // ------------------------------------------------------------------
    // Interno
    // ------------------------------------------------------------------

    private static Image load(String spriteRef) {
        if (spriteRef == null || spriteRef.isBlank()) {
            return null;
        }

        String path = spriteRef.startsWith("/") ? spriteRef : "/" + spriteRef;

        if (CACHE.containsKey(path)) {
            return CACHE.get(path);
        }

        Image image = null;
        try (InputStream in = SpriteLoader.class.getResourceAsStream(path)) {
            if (in != null) {
                Image loaded = new Image(in);
                if (!loaded.isError()) {
                    image = loaded;
                }
            }
        } catch (Exception e) {
            // Archivo ilegible o formato no soportado: se usa el fallback.
            image = null;
        }

        CACHE.put(path, image);
        return image;
    }

    /** Recuadro de color con una letra, para cuando no hay sprite. */
    private static Node placeholder(String initial, String slot, double size) {
        Label label = new Label(initial);
        label.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: "
                + (int) Math.max(10, size * 0.45) + "px;");

        StackPane box = new StackPane(label);
        box.getStyleClass().add("outfit-sprite-fallback");
        box.setPrefSize(size, size);
        box.setMinSize(size, size);
        box.setMaxSize(size, size);
        box.setStyle("-fx-background-color: " + colorForSlot(slot) + ";"
                + "-fx-background-radius: 6;"
                + "-fx-border-color: rgba(0,0,0,0.25);"
                + "-fx-border-radius: 6;");
        return box;
    }

    private static String colorForSlot(String slot) {
        if (slot == null) {
            return "#6b7280";
        }
        switch (slot.trim().toLowerCase()) {
            case "armadura":
                return "#7c6a58";
            case "casco":
                return "#5b6472";
            case "capa":
                return "#4f7a5c";
            case "arma":
                return "#8a5a5a";
            case "escudo":
                return "#7a6440";
            case "botas":
                return "#6a5442";
            default:
                return "#6b7280";
        }
    }

    private static String initialOf(String name) {
        if (name == null || name.isBlank()) {
            return "?";
        }
        return name.trim().substring(0, 1).toUpperCase();
    }
}
