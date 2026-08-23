package com.proyecto.rpg.ui;

import com.proyecto.rpg.model.Race;
import com.proyecto.rpg.singleton.CatalogManager;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

/**
 * Paso 1 del wizard: nombre del personaje y raza.
 */
public class Step1NameRaceController {

    @FXML
    private TextField nameField;

    @FXML
    private ComboBox<Race> raceCombo;

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

    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Faltan datos");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}