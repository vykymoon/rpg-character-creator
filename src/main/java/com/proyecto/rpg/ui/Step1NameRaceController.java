package com.proyecto.rpg.ui;

import com.proyecto.rpg.model.Race;
import com.proyecto.rpg.singleton.CatalogManager;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.control.TextField;

/**
 * Paso 1 del wizard: nombre del personaje y raza.
 */
public class Step1NameRaceController {

    @FXML
    private TextField nameField;

    @FXML
    private ComboBox<Race> raceCombo;

    @FXML
    private ImageView racePreview;

    @FXML
    private Label raceDescLabel;

    @FXML
    private Label raceStatsLabel;

    private final WizardSession session;

    public Step1NameRaceController(WizardSession session) {
        this.session = session;
    }

    @FXML
    public void initialize() {
        raceCombo.setItems(FXCollections.observableArrayList(
                CatalogManager.getInstance().getAvailableRaces()));

        if (session.getName() != null) {
            nameField.setText(session.getName());
        }
        if (session.getRace() != null) {
            raceCombo.setValue(session.getRace());
        }

        raceCombo.valueProperty().addListener((obs, old, race) -> updatePreview(race));
        updatePreview(raceCombo.getValue());
    }

    /** Muestra la foto, la descripción y los atributos base de la raza elegida. */
    private void updatePreview(Race race) {
        racePreview.setImage(RaceAvatarLoader.load(race));
        if (race == null) {
            raceDescLabel.setText("Elige una raza para ver sus rasgos.");
            raceStatsLabel.setText("");
            return;
        }
        raceDescLabel.setText(race.getDescription());
        raceStatsLabel.setText(String.format(
                "FUE %d%nDES %d%nINT %d%nVIT %d",
                race.getBaseStrength(), race.getBaseDexterity(),
                race.getBaseIntelligence(), race.getBaseVitality()));
    }

    @FXML
    public void onNext(ActionEvent event) {
        String name = nameField.getText();
        Race race = raceCombo.getValue();

        if (name == null || name.isBlank()) {
            showWarning("Ponle un nombre al personaje antes de continuar.");
            return;
        }
        if (race == null) {
            showWarning("Elige una raza antes de continuar.");
            return;
        }

        session.setName(name);
        session.setRace(race);

        Step2ClassController next = new Step2ClassController(session);
        SceneNavigator.goTo(event, "/fxml/step2_class.fxml", next);
    }

    @FXML
    public void onBackToMenu(ActionEvent event) {
        GalleryController gallery = new GalleryController();
        SceneNavigator.goTo(event, "/fxml/gallery.fxml", gallery);
    }

    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Faltan datos");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}