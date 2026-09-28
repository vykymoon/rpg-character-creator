package com.proyecto.rpg.ui;

import com.proyecto.rpg.model.Character;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.io.InputStream;

/**
 * Pantalla de combate por turnos (placeholder). Por ahora solo maneja la
 * entrada/salida de la pantalla; la lógica de turnos se agrega después.
 *
 * El enemigo se define solo con el nombre de su PNG dentro de
 * /sprites/battle/: la imagen se carga de ahí y el nombre que se muestra
 * sale del nombre del archivo (enemy.png -> "Un enemy").
 */
public class BattleController {

    private static final String SPRITES_DIR = "/sprites/battle/";
    private static final String DEFAULT_ENEMY_SPRITE = "enemy.png";

    @FXML private ImageView enemyImage;
    @FXML private Label enemyLabel;

    private final Character character;
    private final String enemySprite;

    public BattleController(Character character) {
        this(character, DEFAULT_ENEMY_SPRITE);
    }

    /** Para cuando haya más enemigos: basta con pasar el nombre de su PNG. */
    public BattleController(Character character, String enemySprite) {
        this.character = character;
        this.enemySprite = enemySprite;
    }

    @FXML
    public void initialize() {
        Image image = loadEnemyImage();
        if (image != null) {
            enemyImage.setImage(image);
        } else {
            // Sin PNG no dejamos un hueco en el layout.
            enemyImage.setVisible(false);
            enemyImage.setManaged(false);
        }
        enemyLabel.setText("Un " + enemyName());
    }

    @FXML
    public void onFight(ActionEvent event) {
        // TODO: lógica de combate por turnos
    }

    @FXML
    public void onFlee(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        SceneNavigator.goTo(stage, "/fxml/gallery.fxml", new GalleryController());
    }

    /** "enemy.png" -> "enemy"; "lobo_negro.png" -> "lobo negro". */
    private String enemyName() {
        String name = enemySprite;
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            name = name.substring(0, dot);
        }
        return name.replace('_', ' ').replace('-', ' ');
    }

    private Image loadEnemyImage() {
        try (InputStream in = BattleController.class.getResourceAsStream(SPRITES_DIR + enemySprite)) {
            if (in == null) return null;
            Image image = new Image(in);
            return image.isError() ? null : image;
        } catch (Exception e) {
            return null;
        }
    }
}