package com.proyecto.rpg.ui;

import com.proyecto.rpg.model.Character;
import com.proyecto.rpg.model.Outfit;
import com.proyecto.rpg.model.Skill;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Controller de la ventana modal "Detalle de personaje". Reemplaza el
 * Alert genérico que usaba GalleryController.onViewDetail(). Se
 * instancia a mano (mismo patrón que los controllers del wizard) porque
 * necesita recibir el Character seleccionado por constructor.
 */
public class CharacterDetailController {

    @FXML private Label nameLabel;
    @FXML private Label raceLabel;
    @FXML private Label classLabel;

    @FXML private ProgressBar strBar;
    @FXML private ProgressBar dexBar;
    @FXML private ProgressBar intBar;
    @FXML private ProgressBar vitBar;

    @FXML private Label strValue;
    @FXML private Label dexValue;
    @FXML private Label intValue;
    @FXML private Label vitValue;

    @FXML private VBox skillsBox;
    @FXML private VBox outfitsBox;

    // Escala usada para pintar las barras de stats. Súbelo si tus razas/clases
    // suelen dar totales por encima de 20.
    private static final int STAT_MAX = 20;

    private final Character character;

    public CharacterDetailController(Character character) {
        this.character = character;
    }

    @FXML
    public void initialize() {
        nameLabel.setText(character.getName());
        raceLabel.setText(character.getRace() != null ? character.getRace().getName() : "-");
        classLabel.setText(character.getCharacterClass() != null ? character.getCharacterClass().getName() : "-");

        setStat(strBar, strValue, character.getStrength());
        setStat(dexBar, dexValue, character.getDexterity());
        setStat(intBar, intValue, character.getIntelligence());
        setStat(vitBar, vitValue, character.getVitality());

        fillChips(skillsBox, character.getSkills(), "Sin habilidades", skill ->
                skill.getName() + "  ·  " + skill.getManaCost() + " maná", "skill-chip");

        fillChips(outfitsBox, character.getOutfits(), "Sin vestuario", outfit ->
                outfit.getName() + "  ·  " + outfit.getSlot(), "outfit-chip");
    }

    private void setStat(ProgressBar bar, Label valueLabel, int value) {
        double progress = Math.max(0, Math.min(1.0, value / (double) STAT_MAX));
        bar.setProgress(progress);
        valueLabel.setText(String.valueOf(value));
    }

    private <T> void fillChips(VBox container, java.util.List<T> items, String emptyText,
                                java.util.function.Function<T, String> textMapper, String styleClass) {
        if (items.isEmpty()) {
            Label empty = new Label(emptyText);
            empty.getStyleClass().add("empty-chip");
            container.getChildren().add(empty);
            return;
        }
        for (T item : items) {
            Label chip = new Label(textMapper.apply(item));
            chip.getStyleClass().add(styleClass);
            chip.setMaxWidth(Double.MAX_VALUE);
            container.getChildren().add(chip);
        }
    }

    @FXML
    public void onClose(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }
}
