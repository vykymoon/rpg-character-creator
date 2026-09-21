package com.proyecto.rpg.ui;

import com.proyecto.rpg.model.Character;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.stage.Stage;

/**
 * Pantalla de combate por turnos (placeholder). Por ahora solo maneja la
 * entrada/salida de la pantalla; la lógica de turnos se agrega después.
 */
public class BattleController {

    @FXML private Label enemyLabel;

    private final Character character;

    public BattleController(Character character) {
        this.character = character;
    }

    @FXML
    public void initialize() {
        enemyLabel.setText(character.getName() + " se encuentra con un rival...");
    }

    @FXML
    public void onFight(ActionEvent event) {
        // TODO: lógica de combate por turnos
    }

    @FXML
    public void onFlee(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        SceneNavigator.goTo(stage, "/fxml/gallery.fxml", new GalleryController());
    }
}