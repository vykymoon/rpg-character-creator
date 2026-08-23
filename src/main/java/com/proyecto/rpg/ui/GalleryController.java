package com.proyecto.rpg.ui;

import com.proyecto.rpg.dao.CharacterDAO;
import com.proyecto.rpg.dao.CharacterDAOJson;
import com.proyecto.rpg.model.Character;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;

import java.util.List;
import java.util.Optional;

/**
 * Pantalla de galería: lista los personajes guardados usando
 * CharacterDAO.findAll(), permite ver detalle, eliminar, crear uno
 * nuevo desde cero (wizard) o clonar una plantilla (Prototype).
 * Es la pantalla inicial de la app (MainApp arranca aquí).
 */
public class GalleryController {

    @FXML
    private ListView<Character> characterListView;

    private final CharacterDAO characterDAO = new CharacterDAOJson();

    @FXML
    public void initialize() {
        refreshList();

        characterListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Character character, boolean empty) {
                super.updateItem(character, empty);
                if (empty || character == null) {
                    setText(null);
                } else {
                    String raceName = character.getRace() != null ? character.getRace().getName() : "-";
                    String className = character.getCharacterClass() != null ? character.getCharacterClass().getName() : "-";
                    setText(character.getName() + "   —   " + raceName + " / " + className);
                }
            }
        });
    }

    private void refreshList() {
        List<Character> characters = characterDAO.findAll();
        characterListView.setItems(FXCollections.observableArrayList(characters));
    }

    @FXML
    public void onCreateNew(ActionEvent event) {
        WizardSession session = new WizardSession();
        Step1NameRaceController controller = new Step1NameRaceController(session);
        SceneNavigator.goTo(event, "/fxml/step1_name_race.fxml", controller);
    }

    @FXML
    public void onCloneTemplate(ActionEvent event) {
        TemplateGalleryController controller = new TemplateGalleryController();
        SceneNavigator.goTo(event, "/fxml/template_gallery.fxml", controller);
    }

    @FXML
    public void onViewDetail(ActionEvent event) {
        Character selected = characterListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Selecciona un personaje de la lista primero.");
            return;
        }
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Detalle de " + selected.getName());
        alert.setHeaderText(null);
        alert.setContentText(selected.toString());
        alert.showAndWait();
    }

    @FXML
    public void onDelete(ActionEvent event) {
        Character selected = characterListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Selecciona un personaje de la lista primero.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Eliminar personaje");
        confirm.setHeaderText(null);
        confirm.setContentText("¿Seguro que quieres eliminar a " + selected.getName() + "?");
        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            characterDAO.delete(selected.getId());
            refreshList();
        }
    }

    @FXML
    public void onRefresh(ActionEvent event) {
        refreshList();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Aviso");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}