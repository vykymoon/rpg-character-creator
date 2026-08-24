package com.proyecto.rpg.ui;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Modality;

import java.util.Optional;

/**
 * Centraliza la creación de Alerts para que TODOS lleven el CSS retro
 * aplicado (los Alert de JavaFX abren su propia Stage/Scene y por
 * defecto no heredan el stylesheet de la ventana principal). Reemplaza
 * el patrón repetido "new Alert(...); ...; showAndWait();" que había
 * en GalleryController y Step5SummaryController.
 */
public final class DialogUtils {

    private static final String STYLESHEET =
            DialogUtils.class.getResource("/css/retro-theme.css").toExternalForm();

    private DialogUtils() {
    }

    public static void info(String title, String message) {
        build(Alert.AlertType.INFORMATION, title, message).showAndWait();
    }

    public static void warning(String title, String message) {
        build(Alert.AlertType.WARNING, title, message).showAndWait();
    }

    public static Optional<ButtonType> confirm(String title, String message) {
        return build(Alert.AlertType.CONFIRMATION, title, message).showAndWait();
    }

    private static Alert build(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initModality(Modality.APPLICATION_MODAL);
        alert.getDialogPane().getStylesheets().add(STYLESHEET);
        alert.getDialogPane().getStyleClass().add("retro-dialog");
        return alert;
    }
}
