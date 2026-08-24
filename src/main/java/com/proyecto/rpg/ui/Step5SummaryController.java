package com.proyecto.rpg.ui;

import com.proyecto.rpg.builder.CharacterBuilder;
import com.proyecto.rpg.dao.CharacterDAO;
import com.proyecto.rpg.dao.CharacterDAOJson;
import com.proyecto.rpg.model.Character;
import com.proyecto.rpg.model.Outfit;
import com.proyecto.rpg.model.Skill;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;

public class Step5SummaryController {

    @FXML
    private TextArea summaryArea;

    private final WizardSession session;
    private final CharacterDAO characterDAO = new CharacterDAOJson();

    public Step5SummaryController(WizardSession session) {
        this.session = session;
    }

    @FXML
    public void initialize() {
        StringBuilder sb = new StringBuilder();
        sb.append("Nombre: ").append(session.getName()).append("\n");
        sb.append("Raza: ").append(session.getRace() != null ? session.getRace().getName() : "-").append("\n");
        sb.append("Clase: ").append(session.getCharacterClass() != null ? session.getCharacterClass().getName() : "-").append("\n");

        sb.append("Habilidades: ");
        if (session.getSelectedSkills().isEmpty()) {
            sb.append("ninguna");
        } else {
            for (Skill skill : session.getSelectedSkills()) {
                sb.append(skill.getName()).append(", ");
            }
        }
        sb.append("\n");

        sb.append("Vestuario: ");
        if (session.getSelectedOutfits().isEmpty()) {
            sb.append("ninguno");
        } else {
            for (Outfit outfit : session.getSelectedOutfits()) {
                sb.append(outfit.getName()).append(", ");
            }
        }

        summaryArea.setText(sb.toString());
    }

    @FXML
    public void onBack(ActionEvent event) {
        Step4OutfitController prev = new Step4OutfitController(session);
        SceneNavigator.goTo(event, "/fxml/step4_outfit.fxml", prev);
    }

    @FXML
    public void onConfirm(ActionEvent event) {
        try {
            if (characterDAO.existsByName(session.getName())) {
                DialogUtils.warning(
                        "Nombre duplicado",
                        "Ya existe un personaje guardado con el nombre \"" + session.getName() + "\". Usa otro nombre.");
                return;
            }

            CharacterBuilder builder = new CharacterBuilder()
                    .withName(session.getName())
                    .withRace(session.getRace())
                    .withClass(session.getCharacterClass());

            for (Skill skill : session.getSelectedSkills()) {
                builder.addSkill(skill);
            }
            for (Outfit outfit : session.getSelectedOutfits()) {
                builder.addOutfit(outfit);
            }

            Character character = builder.build();
            characterDAO.save(character);

            DialogUtils.info("Personaje creado", "Se guardó correctamente: " + character.getName());

            GalleryController gallery = new GalleryController();
            SceneNavigator.goTo(event, "/fxml/gallery.fxml", gallery);

        } catch (IllegalStateException e) {
            DialogUtils.warning("Faltan datos", e.getMessage());
        }
    }
}