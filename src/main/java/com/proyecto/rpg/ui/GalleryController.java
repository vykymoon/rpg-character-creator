package com.proyecto.rpg.ui;

import com.proyecto.rpg.dao.CharacterDAO;
import com.proyecto.rpg.dao.CharacterDAOJson;
import com.proyecto.rpg.model.Character;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
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
                    getStyleClass().remove("character-cell");
                } else {
                    String raceName = character.getRace() != null ? character.getRace().getName() : "-";
                    String className = character.getCharacterClass() != null ? character.getCharacterClass().getName() : "-";
                    setText(character.getName() + "   —   " + raceName + " / " + className);
                    if (!getStyleClass().contains("character-cell")) {
                        getStyleClass().add("character-cell");
                    }
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
            DialogUtils.info("Aviso", "Selecciona un personaje de la lista primero.");
            return;
        }
        openDetailWindow(selected, event);
    }

    private void openDetailWindow(Character character, ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/character_detail.fxml"));
            loader.setController(new CharacterDetailController(character));
            Parent root = loader.load();

            Stage detailStage = new Stage();
            detailStage.initStyle(StageStyle.UNDECORATED); // look "in-game", sin barra nativa del SO
            detailStage.initModality(Modality.APPLICATION_MODAL);
            detailStage.initOwner(((Node) event.getSource()).getScene().getWindow());
            detailStage.setTitle("Detalle de " + character.getName());

            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/retro-theme.css").toExternalForm());
            detailStage.setScene(scene);
            detailStage.showAndWait();
        } catch (IOException e) {
            throw new RuntimeException("No se pudo abrir el detalle del personaje", e);
        }
    }

    @FXML
    public void onDelete(ActionEvent event) {
        Character selected = characterListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            DialogUtils.info("Aviso", "Selecciona un personaje de la lista primero.");
            return;
        }

        Optional<ButtonType> result = DialogUtils.confirm(
                "Eliminar personaje",
                "¿Seguro que quieres eliminar a " + selected.getName() + "?");

        if (result.isPresent() && result.get() == ButtonType.OK) {
            characterDAO.delete(selected.getId());
            refreshList();
        }
    }

    @FXML
    public void onRefresh(ActionEvent event) {
        refreshList();
    }
}