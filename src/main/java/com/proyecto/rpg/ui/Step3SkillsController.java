package com.proyecto.rpg.ui;

import com.proyecto.rpg.model.Skill;
import com.proyecto.rpg.singleton.CatalogManager;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;

import java.util.List;

/**
 * Paso 3 del wizard: selección múltiple de habilidades.
 * Este paso es opcional: un personaje puede no llevar ninguna habilidad.
 */
public class Step3SkillsController {

    @FXML
    private ListView<Skill> skillsList;

    @FXML
    private Label hintLabel;

    private final WizardSession session;

    public Step3SkillsController(WizardSession session) {
        this.session = session;
    }

    @FXML
    public void initialize() {
        // Solo lo disponible para la raza (Paso 1) y la clase (Paso 2).
        List<Skill> disponibles = CatalogManager.getInstance().getAvailableSkills().stream()
                .filter(skill -> skill.isAvailableFor(session.getRace(), session.getCharacterClass()))
                .toList();
        skillsList.setItems(FXCollections.observableArrayList(disponibles));

        int ocultas = CatalogManager.getInstance().getAvailableSkills().size() - disponibles.size();
        if (ocultas > 0 && hintLabel != null) {
            hintLabel.setText(ocultas + " habilidad(es) no aparecen: requieren otra raza o clase.");
        }
        skillsList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        for (Skill skill : session.getSelectedSkills()) {
            skillsList.getSelectionModel().select(skill);
        }
    }

    @FXML
    public void onBack(ActionEvent event) {
        session.setSelectedSkills(skillsList.getSelectionModel().getSelectedItems());
        Step2ClassController prev = new Step2ClassController(session);
        SceneNavigator.goTo(event, "/fxml/step2_class.fxml", prev);
    }

    @FXML
    public void onNext(ActionEvent event) {
        session.setSelectedSkills(skillsList.getSelectionModel().getSelectedItems());

        Step4OutfitController next = new Step4OutfitController(session);
        SceneNavigator.goTo(event, "/fxml/step4_outfit.fxml", next);
    }
}