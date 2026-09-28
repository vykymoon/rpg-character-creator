package com.proyecto.rpg.ui;

import com.proyecto.rpg.dao.CharacterDAO;
import com.proyecto.rpg.dao.CharacterDAOJson;
import com.proyecto.rpg.model.Character;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class GalleryController {

    @FXML
    private ListView<Character> characterListView;

    private final CharacterDAO characterDAO = new CharacterDAOJson();

    @FXML
    public void initialize() {
        refreshList();

        Label empty = new Label("Aún no tienes personajes.\n¡Crea el primero!");
        characterListView.setPlaceholder(empty);

        characterListView.setCellFactory(list -> new ListCell<>() {
            private final ImageView avatar = new ImageView();
            private final Label nameLabel = new Label();
            private final Label subLabel = new Label();
            private final VBox texts = new VBox(6, nameLabel, subLabel);
            private final HBox row = new HBox(14, avatar, texts);

            {
                avatar.setFitWidth(48);
                avatar.setFitHeight(48);
                nameLabel.getStyleClass().add("cell-name");
                subLabel.getStyleClass().add("cell-sub");
                texts.setAlignment(Pos.CENTER_LEFT);
                avatar.setPreserveRatio(true);
                avatar.setSmooth(false);
                row.setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(Character character, boolean empty) {
                super.updateItem(character, empty);
                if (empty || character == null) {
                    setGraphic(null);
                    getStyleClass().remove("character-cell");
                    return;
                }

                String raceName = character.getRace() != null ? character.getRace().getName() : "-";
                String className = character.getCharacterClass() != null ? character.getCharacterClass().getName() : "-";

                avatar.setImage(RaceAvatarLoader.load(character.getRace()));
                nameLabel.setText(character.getName());
                subLabel.setText(raceName + "  ·  " + className);

                if (!getStyleClass().contains("character-cell")) {
                    getStyleClass().add("character-cell");
                }
                setGraphic(row);
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

    // --- MODO UN JUGADOR NORMAL ---
    @FXML
    public void onPlay(ActionEvent event) {
        Character selected = characterListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            DialogUtils.info("Aviso", "Selecciona un personaje de la lista primero.");
            return;
        }
        SceneNavigator.goTo(event, "/fxml/play.fxml", new PlayController(selected));
    }

    // --- NUEVO: INICIAR SERVIDOR LAN ---
    @FXML
    public void onStartServer(ActionEvent event) {
        // Ejecutamos el servidor en un hilo nuevo para no congelar la pantalla del menú
        new Thread(() -> {
            try {
                // NOTA: Ajusta "ServerApp" al nombre exacto de la clase principal de tu servidor si se llama distinto
                com.proyecto.rpg.adapters.server.ServerMain.main(new String[]{});
            } catch (Exception ex) {
                System.err.println("Error al iniciar el servidor: " + ex.getMessage());
            }
        }).start();

        DialogUtils.info("Servidor", "Servidor LAN iniciado.\nYa puedes conectar el multijugador.");
    }

    // --- NUEVO: MODO MULTIJUGADOR ---
    @FXML
    public void onPlayMultiplayer(ActionEvent event) {
        try {
            // Cerramos la ventana del menú actual
            Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            currentStage.close();

            // Iniciamos tu GameApp (el que tiene el mapa del bosque que arreglamos)
            Stage multiStage = new Stage();
            new GameApp().start(multiStage);
        } catch (Exception ex) {
            DialogUtils.warning("Error", "No se pudo iniciar el modo multijugador.");
            ex.printStackTrace();
        }
    }

    private void openDetailWindow(Character character, ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/character_detail.fxml"));
            loader.setController(new CharacterDetailController(character));
            Parent root = loader.load();

            Stage detailStage = new Stage();
            detailStage.initStyle(StageStyle.UNDECORATED);
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