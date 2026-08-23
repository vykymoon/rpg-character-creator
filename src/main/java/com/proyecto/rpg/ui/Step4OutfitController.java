package com.proyecto.rpg.ui;

import com.proyecto.rpg.model.Outfit;
import com.proyecto.rpg.singleton.CatalogManager;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;

/**
 * Paso 4 del wizard: selección de vestuario/equipamiento visual.
 * Este paso es opcional: un personaje puede no llevar vestuario todavía.
 */
public class Step4OutfitController {

    @FXML
    private ListView<Outfit> outfitsList;

    private final WizardSession session;

    public Step4OutfitController(WizardSession session) {
        this.session = session;
    }

    @FXML
    public void initialize() {
        outfitsList.setItems(FXCollections.observableArrayList(
                CatalogManager.getInstance().getAvailableOutfits()));
        outfitsList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        for (Outfit outfit : session.getSelectedOutfits()) {
            outfitsList.getSelectionModel().select(outfit);
        }
    }

    @FXML
    public void onBack(ActionEvent event) {
        session.setSelectedOutfits(outfitsList.getSelectionModel().getSelectedItems());
        Step3SkillsController prev = new Step3SkillsController(session);
        SceneNavigator.goTo(event, "/fxml/step3_skills.fxml", prev);
    }

    @FXML
    public void onNext(ActionEvent event) {
        session.setSelectedOutfits(outfitsList.getSelectionModel().getSelectedItems());

        Step5SummaryController next = new Step5SummaryController(session);
        SceneNavigator.goTo(event, "/fxml/step5_summary.fxml", next);
    }
}