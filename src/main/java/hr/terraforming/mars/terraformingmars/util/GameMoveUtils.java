package hr.terraforming.mars.terraformingmars.util;

import hr.terraforming.mars.terraformingmars.enums.ActionType;
import hr.terraforming.mars.terraformingmars.model.Card;
import hr.terraforming.mars.terraformingmars.model.GameManager;
import hr.terraforming.mars.terraformingmars.model.GameMove;
import hr.terraforming.mars.terraformingmars.model.Player;
import hr.terraforming.mars.terraformingmars.thread.GetLastGameMoveThread;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.util.Duration;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Slf4j
public class GameMoveUtils {

    private static final String GAME_MOVE_HISTORY_FILE_NAME = "gameMoves/moves.dat";

    private GameMoveUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static void saveNewGameMove(GameMove newGameMove) {
        List<GameMove> gameMoveList = loadAllGameMoves();
        gameMoveList.add(newGameMove);

        File file = new File(GAME_MOVE_HISTORY_FILE_NAME);
        File parentDir = file.getParentFile();

        if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
            log.error("Failed to create directory: {}", parentDir.getAbsolutePath());
            return;
        }

        try (ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(file))) {
            outputStream.writeObject(gameMoveList);
        } catch (IOException e) {
            log.error("Failed to save game moves to file '{}'", GAME_MOVE_HISTORY_FILE_NAME, e);
        }
    }

    public static Optional<GameMove> getLastGameMove() {
        List<GameMove> gameMoveList = loadAllGameMoves();
        if (gameMoveList.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(gameMoveList.getLast());
    }

    @SuppressWarnings("unchecked")
    private static List<GameMove> loadAllGameMoves() {
        File file = new File(GAME_MOVE_HISTORY_FILE_NAME);
        if (!file.exists()) {
            return new ArrayList<>();
        }

        try (ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(file))) {
            return (List<GameMove>) inputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            log.error("Error loading game moves: '{}'", e.getMessage());
            return new ArrayList<>();
        }
    }

    public static void deleteMoveHistoryFile() {
        File file = new File(GAME_MOVE_HISTORY_FILE_NAME);
        if (file.exists()) {
            try {
                Files.delete(file.toPath());
                log.info("Game move history file deleted.");
            } catch (IOException e) {
                log.error("Failed to delete game move history file.", e);
            }
        }
    }

    public static Timeline createLastMoveTimeline(Label lastMoveLabel) {
        Timeline lastMoveTimeline = new Timeline(new KeyFrame(Duration.seconds(0.5), _ -> {
            GetLastGameMoveThread thread = new GetLastGameMoveThread(lastMoveLabel);
            Thread t = new Thread(thread);
            t.setDaemon(true);
            t.start();
        }));
        lastMoveTimeline.setCycleCount(Animation.INDEFINITE);
        return lastMoveTimeline;
    }

    public static void updateLastMoveLabel(Label label, GameMove move) {
        if (label == null) {
            log.warn("Label is null, cannot update!");
            return;
        }

        if (move != null) {
            StringBuilder sb = new StringBuilder();
            sb.append(move.playerName()).append(" ");
            sb.append(move.message());

            if (move.row() != null) {
                sb.append(" at (")
                        .append(move.row())
                        .append(", ")
                        .append(move.col())
                        .append(")");
            }

            label.setText(sb.toString());
            log.debug("Last move label updated: {}", sb);
        } else {
            label.setText("");
            log.debug("Last move label cleared");
        }
    }

    public static void saveInitialSetupMove(GameManager gameManager) {
        try {
            Map<String, Object> setupData = new HashMap<>();
            for (Player player : gameManager.getPlayers()) {
                Map<String, Object> playerData = new HashMap<>();

                playerData.put("corporation",
                        player.getCorporation() != null ? player.getCorporation().name() : "N/A");

                playerData.put("hand", player.getHand().stream().map(Card::getName).toList());
                setupData.put(player.getName(), playerData);
            }

            String jsonDetails = new com.google.gson.Gson().toJson(setupData);

            GameMove initialMove = new GameMove(
                    "System",
                    ActionType.INITIAL_SETUP,
                    jsonDetails,
                    LocalDateTime.now(ZoneOffset.UTC)
            );

            XmlUtils.appendGameMove(initialMove);
            log.debug("Initial setup move successfully saved to XML!");

        } catch (Exception e) {
            log.error("Fatal error occurred during initial state saving.");
            new Alert(Alert.AlertType.ERROR, "Error saving initial state for replay. See console for details.\n\n" + e.getMessage()).showAndWait();
        }
    }
}
