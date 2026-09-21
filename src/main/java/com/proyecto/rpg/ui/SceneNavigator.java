package com.proyecto.rpg.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.stage.Stage;

import java.io.IOException;

public final class SceneNavigator {

    private SceneNavigator() {
    }

    public static void goTo(ActionEvent event, String fxmlPath, Object controller) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        goTo(stage, fxmlPath, controller);
    }

    public static void goTo(Stage stage, String fxmlPath, Object controller) {
        try {
            FXMLLoader loader = new FXMLLoader(SceneNavigator.class.getResource(fxmlPath));
            loader.setController(controller);
            Parent root = loader.load();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo cargar la pantalla: " + fxmlPath, e);
        }
    }
}