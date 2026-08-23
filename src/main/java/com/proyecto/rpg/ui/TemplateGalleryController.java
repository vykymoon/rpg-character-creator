package com.proyecto.rpg.ui;

import com.proyecto.rpg.dao.CharacterDAO;
import com.proyecto.rpg.dao.CharacterDAOJson;
import com.proyecto.rpg.model.Character;
import com.proyecto.rpg.prototype.CharacterPrototype;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

/**
 * Pantalla para clonar plantillas de NPCs (patrón Prototype).
 * Muestra las plantillas precargadas, permite elegir una, ponerle un
 * nombre nuevo, clonarla y guardarla como personaje independiente.
 */
public class TemplateGalleryController {

    @FXML
    private ListView<String> templateListView;

    @FXML
    private TextField newNameField;

    private final CharacterPrototype prototype = CharacterPrototype.withDefaultTemplates();
    private final CharacterDAO characterDAO = new CharacterDAOJson();

    @FXML
    public void initialize() {
        templateListView.setItems(FXCollections.observableArrayList(prototype.getTemplates().keySet()));

        templateListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(String key, boolean empty) {
                super.updateItem(key, empty);
                if (empty || key == null) {
                    setText(null);
                } else {
                    Character template = prototype.getTemplates().get(key);
                    setText(template.getName() + "  (" + template.getRace().getName()
                            + " / " + template.getCharacterClass().getName() + ")");
                }
            }
        });
    }

    @FXML
    public void onCloneAndSave(ActionEvent event) {
        String key = templateListView.getSelectionModel().getSelectedItem();
        if (key == null) {
            showWarning("Selecciona una plantilla de la lista primero.");
            return;
        }

        String newName = newNameField.getText();
        if (newName == null || newName.isBlank()) {
            showWarning("Ponle un nombre al nuevo personaje.");
            return;
        }

        Character clone = prototype.clone(key);
        clone.setName(newName);
        characterDAO.save(clone);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Personaje clonado");
        alert.setHeaderText(null);
        alert.setContentText("Se creó " + clone.getName() + " a partir de la plantilla.");
        alert.showAndWait();

        GalleryController gallery = new GalleryController();
        SceneNavigator.goTo(event, "/fxml/gallery.fxml", gallery);
    }

    @FXML
    public void onBack(ActionEvent event) {
        GalleryController gallery = new GalleryController();
        SceneNavigator.goTo(event, "/fxml/gallery.fxml", gallery);
    }

    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Falta un dato");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}