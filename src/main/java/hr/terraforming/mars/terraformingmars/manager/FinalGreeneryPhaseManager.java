package hr.terraforming.mars.terraformingmars.manager;

import hr.terraforming.mars.terraformingmars.controller.game.FinalGreeneryController;
import hr.terraforming.mars.terraformingmars.controller.game.GameScreenController;
import hr.terraforming.mars.terraformingmars.enums.GamePhase;
import hr.terraforming.mars.terraformingmars.enums.PlayerType;
import hr.terraforming.mars.terraformingmars.enums.ResourceType;
import hr.terraforming.mars.terraformingmars.model.*;
import hr.terraforming.mars.terraformingmars.network.message.FinalGreeneryOfferMessage;
import hr.terraforming.mars.terraformingmars.network.message.GameOverMessage;
import hr.terraforming.mars.terraformingmars.util.ScreenUtils;
import hr.terraforming.mars.terraformingmars.view.FxmlPaths;
import hr.terraforming.mars.terraformingmars.view.ScreenNavigator;
import javafx.application.Platform;
import javafx.stage.Window;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class FinalGreeneryPhaseManager {

    private final GameManager gameManager;
    private final Window ownerWindow;
    private final Runnable onComplete;
    private final GameScreenController controller;
    private int currentPlayerIndex = 0;

    public FinalGreeneryPhaseManager(GameManager gameManager, Window ownerWindow,
                                     GameScreenController controller, Runnable onComplete) {
        this.gameManager = gameManager;
        this.ownerWindow = ownerWindow;
        this.controller = controller;
        this.onComplete = onComplete;
    }

    public void start() {
        gameManager.setCurrentPhase(GamePhase.FINAL_GREENERY);
        currentPlayerIndex = 0;
        Platform.runLater(this::processNextPlayer);
    }

    public void processNextPlayer() {
        if (currentPlayerIndex >= gameManager.getPlayers().size()) {
            finishPhase();
            return;
        }

        handleCurrentPlayerTurn();
    }

    private void finishPhase() {
        log.info("All players have finished Final Greenery!");

        gameManager.setCurrentPhase(GamePhase.GAME_OVER);

        PlayerType playerType = ApplicationConfiguration.getInstance().getPlayerType();

        if (playerType == PlayerType.HOST) {
            List<Player> rankedPlayers = gameManager.calculateFinalScores();

            var server = ApplicationConfiguration.getInstance().getGameServer();

            if (server != null) {
                GameBoard gameBoard = gameManager.getGameBoard();
                server.broadcastGameState(new GameState(gameManager, gameBoard));

                server.broadcastToAll(new GameOverMessage());
            }

            Platform.runLater(() -> ScreenNavigator.showGameOverScreen(rankedPlayers));
        } else if (playerType == PlayerType.LOCAL) {
            onComplete.run();
        }
    }

    private void handleCurrentPlayerTurn() {
        Player currentPlayer = gameManager.getPlayers().get(currentPlayerIndex);
        String myPlayerName = ApplicationConfiguration.getInstance().getMyPlayerName();
        PlayerType playerType = ApplicationConfiguration.getInstance().getPlayerType();

        log.info("Final Greenery turn for: {}", currentPlayer.getName());

        int plants = currentPlayer.resourceProperty(ResourceType.PLANTS).get();
        int cost = currentPlayer.getGreeneryCost();

        if (plants < cost) {
            log.info("Player {} has insufficient plants ({} < {}) - auto-skipping Final Greenery",
                    currentPlayer.getName(), plants, cost);
            finishForCurrentPlayer();
            return;
        }

        if (playerType == PlayerType.LOCAL) {
            showModalForPlayer(currentPlayer);
            return;
        }

        if (playerType != PlayerType.HOST) {
            return;
        }

        if (currentPlayer.getName().equals(myPlayerName)) {
            showModalForPlayer(currentPlayer);
        } else {
            var server = ApplicationConfiguration.getInstance().getGameServer();

            if (server != null) {
                log.debug("Host sending FinalGreeneryOffer to {}", currentPlayer.getName());
                server.sendToPlayer(
                        currentPlayer.getName(),
                        new FinalGreeneryOfferMessage(currentPlayer.getName())
                );
            }
        }
    }

    private void showModalForPlayer(Player player) {
        ScreenUtils.showAsModal(
                ownerWindow,
                FxmlPaths.FINAL_GREENERY,
                "Final Greenery Conversion - " + player.getName(),
                (FinalGreeneryController c) -> c.setupSinglePlayer(
                        player, gameManager, controller, this::finishForCurrentPlayer
                )
        );
    }

    void finishForCurrentPlayer() {
        if (currentPlayerIndex >= gameManager.getPlayers().size()) {
            log.warn("FinishForCurrentPlayer called but Final Greenery already complete");
            return;
        }

        Player currentPlayer = gameManager.getPlayers().get(currentPlayerIndex);

        gameManager.hasMoreDraftPlayers();

        var config = ApplicationConfiguration.getInstance();

        if (config.getPlayerType() == PlayerType.HOST) {
            var broadcaster = config.getBroadcaster();

            if (broadcaster != null) {
                log.debug("Host broadcasting after {} finished", currentPlayer.getName());
                broadcaster.broadcast();
            }
        }

        currentPlayerIndex++;
        Platform.runLater(this::processNextPlayer);
    }
}