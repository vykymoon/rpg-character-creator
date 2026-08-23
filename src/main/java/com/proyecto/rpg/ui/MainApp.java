package com.proyecto.rpg.ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

/**
 * Punto de entrada de la aplicación. Carga el wizard de creación de
 * personajes como pantalla inicial.
 *
 * Para correr la app: mvn javafx:run
 */
public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                Objects.requireNonNull(getClass().getResource("/fxml/wizard.fxml"))
        );
        Parent root = loader.load();

        Scene scene = new Scene(root, 720, 520);
        stage.setTitle("RPG Character Creator");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
