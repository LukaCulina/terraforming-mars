package hr.terraforming.mars.terraformingmars.service;

import hr.terraforming.mars.terraformingmars.model.GameBoard;
import hr.terraforming.mars.terraformingmars.model.GameManager;
import hr.terraforming.mars.terraformingmars.model.GameState;
import hr.terraforming.mars.terraformingmars.util.DialogUtils;
import hr.terraforming.mars.terraformingmars.util.GameMoveUtils;
import hr.terraforming.mars.terraformingmars.util.XmlUtils;
import javafx.scene.control.Alert;
import lombok.extern.slf4j.Slf4j;

import java.io.*;

@Slf4j
public class GameStateService {

    public static final String SAVE_GAME_FILENAME = "gameSave.dat";

    private static File getSaveGameFile() {
        String userHome = System.getProperty("user.home");
        File directory = new File(userHome, ".terraformingmars");

        if (!directory.exists() && !directory.mkdirs()) {
            log.warn("Could not create directory: {}", directory.getAbsolutePath());
        }
        return new File(directory, SAVE_GAME_FILENAME);
    }

    public void clearGameData() {
        GameMoveUtils.deleteMoveHistoryFile();
        XmlUtils.clearGameMoves();
    }

    public void saveGame(GameManager gameManager, GameBoard gameBoard) {
        GameState gameState = new GameState(gameManager, gameBoard);

        File file = getSaveGameFile();
        File parentDir = file.getParentFile();
        if (!parentDir.exists()) {
            if (parentDir.mkdirs()) {
                log.info("Created directory: {}", parentDir.getAbsolutePath());
            } else {
                log.error("Failed to create directory: {}", parentDir.getAbsolutePath());
                return;
            }
        }

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(gameState);
            DialogUtils.showDialog(Alert.AlertType.INFORMATION, "Save Game Successful!", "The game has been successfully saved!");
            log.info("Game state saved to {}", file.getAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to save game state to {}", file.getAbsolutePath(), e);
        }
    }

    public GameState loadGame() {
        File file = getSaveGameFile();

        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                GameState loadedState = (GameState) ois.readObject();
                log.info("Game state loaded from {}", file.getAbsolutePath());
                return loadedState;
            } catch (IOException | ClassNotFoundException e) {
                log.error("Failed to load game state from {}", file.getAbsolutePath(), e);
            }
        } else {
            log.warn("Save file not found at: {}", file.getAbsolutePath());
        }
        return null;
    }
}