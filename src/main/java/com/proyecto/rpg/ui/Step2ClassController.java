package com.proyecto.rpg.ui;

import com.proyecto.rpg.model.CharacterClass;
import com.proyecto.rpg.singleton.CatalogManager;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;

/**
 * Paso 2 del wizard: clase de personaje (Guerrero, Mago, Pícaro).
 */
public class Step2ClassController {

    @FXML
    private ComboBox<CharacterClass> classCombo;

    @FXML
    private Label classDescLabel;

    @FXML
    private Label classBonusLabel;

    private final WizardSession session;

    public Step2ClassController(WizardSession session) {
        this.session = session;
    }

    @FXML
    public void initialize() {
        classCombo.setItems(FXCollections.observableArrayList(
                CatalogManager.getInstance().getAvailableClasses()));

        if (session.getCharacterClass() != null) {
            classCombo.setValue(session.getCharacterClass());
        }

        classCombo.valueProperty().addListener((obs, old, c) -> updatePreview(c));
        updatePreview(classCombo.getValue());
    }

    /** Muestra la descripción y los bonos de atributos de la clase elegida. */
    private void updatePreview(CharacterClass c) {
        if (c == null) {
            classDescLabel.setText("Elige una clase para ver qué aporta a tu personaje.");
            classBonusLabel.setText("");
            return;
        }
        classDescLabel.setText(c.getDescription());
        classBonusLabel.setText(String.format(
                "BONOS%n%nFUE +%d%nDES +%d%nINT +%d%nVIT +%d",
                c.getBonusStrength(), c.getBonusDexterity(),
                c.getBonusIntelligence(), c.getBonusVitality()));
    }

    @FXML
    public void onBack(ActionEvent event) {
        Step1NameRaceController prev = new Step1NameRaceController(session);
        SceneNavigator.goTo(event, "/fxml/step1_name_race.fxml", prev);
    }

    @FXML
    public void onNext(ActionEvent event) {
        CharacterClass characterClass = classCombo.getValue();
        if (characterClass == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Falta un dato");
            alert.setHeaderText(null);
            alert.setContentText("Elige una clase antes de continuar.");
            alert.showAndWait();
            return;
        }

        session.setCharacterClass(characterClass);

        Step3SkillsController next = new Step3SkillsController(session);
        SceneNavigator.goTo(event, "/fxml/step3_skills.fxml", next);
    }
}