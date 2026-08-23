package com.proyecto.rpg.ui;

import com.proyecto.rpg.builder.CharacterBuilder;
import com.proyecto.rpg.dao.CharacterDAO;
import com.proyecto.rpg.dao.CharacterDAOJson;
import com.proyecto.rpg.model.Character;
import com.proyecto.rpg.model.CharacterClass;
import com.proyecto.rpg.model.Race;
import com.proyecto.rpg.model.Skill;
import com.proyecto.rpg.singleton.CatalogManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.util.List;

/**
 * Controlador del wizard de creación de personajes.
 * Ejemplo mínimo funcional que conecta:
 *   - Singleton (CatalogManager) para poblar los combos
 *   - Factory (ya usado dentro de CatalogManager)
 *   - Builder (CharacterBuilder) para armar el personaje paso a paso
 *   - DAO (CharacterDAOJson) para guardarlo
 *
 * Persona B: este es tu punto de partida para el wizard visual completo
 * (idealmente separado en varias pantallas/pasos en vez de un solo form).
 */
public class WizardController {

    @FXML
    private TextField nameField;

    @FXML
    private ComboBox<Race> raceCombo;

    @FXML
    private ComboBox<CharacterClass> classCombo;

    @FXML
    private ListView<Skill> skillsList;

    @FXML
    private TextArea previewArea;

    @FXML
    private Button createButton;

    private final CharacterDAO characterDAO = new CharacterDAOJson();

    @FXML
    public void initialize() {
        CatalogManager catalog = CatalogManager.getInstance();

        raceCombo.setItems(FXCollections.observableArrayList(catalog.getAvailableRaces()));
        classCombo.setItems(FXCollections.observableArrayList(catalog.getAvailableClasses()));

        skillsList.setItems(FXCollections.observableArrayList(catalog.getAvailableSkills()));
        MultipleSelectionModel<Skill> selectionModel = skillsList.getSelectionModel();
        selectionModel.setSelectionMode(SelectionMode.MULTIPLE);
    }

    @FXML
    public void onCreateCharacter() {
        try {
            String name = nameField.getText();
            Race race = raceCombo.getValue();
            CharacterClass characterClass = classCombo.getValue();
            List<Skill> selectedSkills = skillsList.getSelectionModel().getSelectedItems();

            CharacterBuilder builder = new CharacterBuilder()
                    .withName(name)
                    .withRace(race)
                    .withClass(characterClass);

            for (Skill skill : selectedSkills) {
                builder.addSkill(skill);
            }

            Character character = builder.build();
            characterDAO.save(character);

            previewArea.setText(character.toString());
            showAlert(Alert.AlertType.INFORMATION, "Personaje creado",
                    "Se guardó correctamente: " + character.getName());

        } catch (IllegalStateException e) {
            showAlert(Alert.AlertType.WARNING, "Faltan datos", e.getMessage());
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error inesperado", e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
