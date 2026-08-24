package com.proyecto.rpg.ui;

import com.proyecto.rpg.model.Outfit;
import com.proyecto.rpg.singleton.CatalogManager;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Paso 4 del wizard: selección de vestuario/equipamiento visual.
 * Este paso es opcional: un personaje puede no llevar vestuario todavía.
 *
 * Cada fila muestra el sprite de la pieza. Si el PNG no existe,
 * SpriteLoader devuelve un recuadro con la inicial en vez de fallar.
 */
public class Step4OutfitController {

    private static final double ICON_SIZE = 32;

    @FXML
    private ListView<Outfit> outfitsList;

    @FXML
    private Label hintLabel;

    private final WizardSession session;

    public Step4OutfitController(WizardSession session) {
        this.session = session;
    }

    @FXML
    public void initialize() {
        // Solo lo disponible para la raza (Paso 1) y la clase (Paso 2).
        List<Outfit> disponibles = CatalogManager.getInstance().getAvailableOutfits().stream()
                .filter(outfit -> outfit.isAvailableFor(session.getRace(), session.getCharacterClass()))
                .toList();
        outfitsList.setItems(FXCollections.observableArrayList(disponibles));

        int ocultos = CatalogManager.getInstance().getAvailableOutfits().size() - disponibles.size();
        if (ocultos > 0 && hintLabel != null) {
            hintLabel.setText(ocultos + " prenda(s) no aparecen: requieren otra raza o clase.");
        }
        outfitsList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        outfitsList.setCellFactory(list -> new OutfitCell());

        for (Outfit outfit : session.getSelectedOutfits()) {
            outfitsList.getSelectionModel().select(outfit);
        }
    }

    @FXML
    public void onBack(ActionEvent event) {
        session.setSelectedOutfits(outfitsList.getSelectionModel().getSelectedItems());
        Step3SkillsController prev = new Step3SkillsController(session);
        SceneNavigator.goTo(event, "/fxml/step3_skills.fxml", prev);
    }

    @FXML
    public void onNext(ActionEvent event) {
        session.setSelectedOutfits(outfitsList.getSelectionModel().getSelectedItems());

        Step5SummaryController next = new Step5SummaryController(session);
        SceneNavigator.goTo(event, "/fxml/step5_summary.fxml", next);
    }

    /**
     * Fila de la lista: sprite a la izquierda, nombre y slot a la derecha.
     * Se reutiliza la misma celda al hacer scroll, por eso hay que limpiar
     * el gráfico cuando la fila queda vacía.
     */
    private static class OutfitCell extends ListCell<Outfit> {

        @Override
        protected void updateItem(Outfit outfit, boolean empty) {
            super.updateItem(outfit, empty);

            if (empty || outfit == null) {
                setText(null);
                setGraphic(null);
                return;
            }

            // Sin setStyle(): en JavaFX el estilo inline le gana a la hoja
            // de estilos, así que estas etiquetas quedarían fuera del tema
            // retro. Con clases CSS las controla retro-theme.css.
            Label name = new Label(outfit.getName());
            name.getStyleClass().add("outfit-name");

            Label slot = new Label(outfit.getSlot());
            slot.getStyleClass().add("outfit-slot");

            VBox texts = new VBox(2, name, slot);
            texts.setAlignment(Pos.CENTER_LEFT);

            HBox row = new HBox(12, SpriteLoader.iconFor(outfit, ICON_SIZE), texts);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("outfit-row");

            setText(null);
            setGraphic(row);
        }
    }
}
