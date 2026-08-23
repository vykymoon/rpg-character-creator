package com.proyecto.rpg.ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Punto de entrada de la aplicación. Arranca en la galería de
 * personajes; desde ahí se accede al wizard de creación.
 *
 * Para correr la app: mvn javafx:run
 */
public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/gallery.fxml"));
        loader.setController(new GalleryController());
        Parent root = loader.load();

        Scene scene = new Scene(root, 480, 460);
        stage.setTitle("RPG Character Creator");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}