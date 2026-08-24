package com.proyecto.rpg.ui;

import com.proyecto.rpg.model.Character;
import com.proyecto.rpg.model.Outfit;
import com.proyecto.rpg.model.Skill;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.media.AudioClip;
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

    @FXML private ImageView avatarImage;

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

        avatarImage.setImage(resolveAvatar());
        playOpenSound();

        setStat(strBar, strValue, character.getStrength());
        setStat(dexBar, dexValue, character.getDexterity());
        setStat(intBar, intValue, character.getIntelligence());
        setStat(vitBar, vitValue, character.getVitality());

        fillChips(skillsBox, character.getSkills(), "Sin habilidades", skill ->
                skill.getName() + "  ·  " + skill.getManaCost() + " maná", "skill-chip");

        fillChips(outfitsBox, character.getOutfits(), "Sin vestuario", outfit ->
                outfit.getName() + "  ·  " + outfit.getSlot(), "outfit-chip");
    }

    /**
     * Busca un sprite de avatar según el nombre de la raza en
     * /sprites/avatars/<raza_normalizada>.png. Si no existe, cae a
     * /sprites/avatars/default.png. Si tampoco existe ese, no revienta
     * la app, solo deja el ImageView vacío.
     */
    private Image resolveAvatar() {
        String raceName = character.getRace() != null ? character.getRace().getName() : "default";
        String path = "/sprites/avatars/" + normalize(raceName) + ".png";

        var stream = getClass().getResourceAsStream(path);
        if (stream == null) {
            stream = getClass().getResourceAsStream("/sprites/avatars/default.png");
        }
        return stream != null ? new Image(stream) : null;
    }

    private String normalize(String name) {
        return name.toLowerCase()
                .replace("á", "a").replace("é", "e").replace("í", "i")
                .replace("ó", "o").replace("ú", "u")
                .replace(" ", "_");
    }

    /**
     * Reproduce un sonido corto al abrir el detalle. Si el archivo no
     * existe todavía, simplemente no suena nada (no lanza error).
     */
    private void playOpenSound() {
        var soundUrl = getClass().getResource("/sounds/open_detail.wav");
        if (soundUrl != null) {
            new AudioClip(soundUrl.toExternalForm()).play();
        }
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