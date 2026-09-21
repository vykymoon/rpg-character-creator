package com.proyecto.rpg.ui;

import com.proyecto.rpg.model.Character;
import com.proyecto.rpg.model.Outfit;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 * Panel de inventario estilo RPG: marco de "pergamino/piedra", cabecera
 * con foto de raza + nombre, y grilla de slots para el vestuario
 * equipado. Se abre/cierra con E (ver InputHandler).
 */
public class InventoryOverlay extends VBox {

    private static final Color PANEL_BG = Color.web("#2b2419");
    private static final Color PANEL_BORDER = Color.web("#8a6d3b");
    private static final Color SLOT_BG = Color.web("#3d3423");
    private static final Color SLOT_BORDER = Color.web("#6b5a3a");
    private static final Color TEXT_GOLD = Color.web("#e8d5a0");
    private static final Color TEXT_MUTED = Color.web("#b3a781");

    public InventoryOverlay(Character character) {
        setSpacing(14);
        setAlignment(Pos.TOP_CENTER);
        setPadding(new Insets(22));
        setMaxWidth(340);
        setMaxHeight(440);

        setBackground(new Background(new BackgroundFill(PANEL_BG, new CornerRadii(12), Insets.EMPTY)));
        setBorder(new Border(new BorderStroke(
                PANEL_BORDER, BorderStrokeStyle.SOLID, new CornerRadii(12), new BorderWidths(3))));

        setVisible(false);
        setManaged(false);

        getChildren().addAll(
                buildHeader(character),
                buildDivider(),
                buildOutfitGrid(character)
        );
    }

    private HBox buildHeader(Character character) {
        StackPane avatarFrame = buildAvatarFrame(character);

        Label name = new Label(character.getName());
        name.setTextFill(TEXT_GOLD);
        name.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        String raceText = character.getRace() != null ? character.getRace().getName() : "-";
        String classText = character.getCharacterClass() != null ? character.getCharacterClass().getName() : "-";
        Label subtitle = new Label(raceText + " · " + classText);
        subtitle.setTextFill(TEXT_MUTED);
        subtitle.setStyle("-fx-font-size: 12px;");

        VBox info = new VBox(4, name, subtitle);
        info.setAlignment(Pos.CENTER_LEFT);

        HBox header = new HBox(14, avatarFrame, info);
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    private StackPane buildAvatarFrame(Character character) {
        ImageView avatar = RaceAvatarLoader.iconFor(character.getRace(), 72);
        avatar.setClip(new Rectangle(72, 72));

        StackPane frame = new StackPane(avatar);
        frame.setPrefSize(78, 78);
        frame.setMaxSize(78, 78);
        frame.setBackground(new Background(new BackgroundFill(SLOT_BG, new CornerRadii(8), Insets.EMPTY)));
        frame.setBorder(new Border(new BorderStroke(
                PANEL_BORDER, BorderStrokeStyle.SOLID, new CornerRadii(8), new BorderWidths(2))));
        return frame;
    }

    private Rectangle buildDivider() {
        Rectangle line = new Rectangle(280, 2);
        line.setFill(PANEL_BORDER);
        line.setOpacity(0.5);
        return line;
    }

    private VBox buildOutfitGrid(Character character) {
        Label title = new Label("VESTUARIO");
        title.setTextFill(TEXT_MUTED);
        title.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");

        FlowPane slots = new FlowPane(10, 10);
        slots.setAlignment(Pos.CENTER);

        if (character.getOutfits().isEmpty()) {
            Label empty = new Label("Sin vestuario equipado");
            empty.setTextFill(TEXT_MUTED);
            slots.getChildren().add(empty);
        } else {
            for (Outfit outfit : character.getOutfits()) {
                slots.getChildren().add(buildOutfitSlot(outfit));
            }
        }

        VBox box = new VBox(8, title, slots);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    private StackPane buildOutfitSlot(Outfit outfit) {
        StackPane slot = new StackPane(SpriteLoader.iconFor(outfit, 40));
        slot.setPrefSize(56, 56);
        slot.setMaxSize(56, 56);
        slot.setBackground(new Background(new BackgroundFill(SLOT_BG, new CornerRadii(6), Insets.EMPTY)));
        slot.setBorder(new Border(new BorderStroke(
                SLOT_BORDER, BorderStrokeStyle.SOLID, new CornerRadii(6), new BorderWidths(1.5))));

        javafx.scene.control.Tooltip.install(slot, new javafx.scene.control.Tooltip(
                outfit.getName() + " (" + outfit.getSlot() + ")"));

        return slot;
    }

    public void toggle() {
        boolean show = !isVisible();
        setVisible(show);
        setManaged(show);
    }
}