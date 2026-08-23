package com.proyecto.rpg.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Utilidad para navegar entre las pantallas del wizard sin abrir ventanas
 * nuevas: reemplaza el contenido de la ventana actual.
 *
 * Cada pantalla del wizard NO usa fx:controller en su .fxml; en vez de
 * eso, el controlador se crea a mano aquí para poder pasarle la
 * WizardSession compartida por constructor.
 */
public final class SceneNavigator {

    private SceneNavigator() {
    }

    public static void goTo(ActionEvent event, String fxmlPath, Object controller) {
        try {
            FXMLLoader loader = new FXMLLoader(SceneNavigator.class.getResource(fxmlPath));
            loader.setController(controller);
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo cargar la pantalla: " + fxmlPath, e);
        }
    }
}