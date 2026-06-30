package hr.terraforming.mars.terraformingmars.network;

import hr.terraforming.mars.terraformingmars.controller.game.ChooseCardsController;
import hr.terraforming.mars.terraformingmars.controller.game.FinalGreeneryController;
import hr.terraforming.mars.terraformingmars.controller.game.GameScreenController;
import hr.terraforming.mars.terraformingmars.enums.ActionType;
import hr.terraforming.mars.terraformingmars.factory.CardFactory;
import hr.terraforming.mars.terraformingmars.factory.CorporationFactory;
import hr.terraforming.mars.terraformingmars.manager.GameFlowManager;
import hr.terraforming.mars.terraformingmars.model.*;
import hr.terraforming.mars.terraformingmars.network.message.*;
import hr.terraforming.mars.terraformingmars.util.ScreenUtils;
import hr.terraforming.mars.terraformingmars.view.ScreenNavigator;
import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Slf4j
public record ClientMessageDispatcher(GameClientThread client, List<GameStateListener> listeners) {

    public void dispatch(Object message, GameState lastGameState) {
        if (message instanceof GameState state) {
            handleGameState(state);
        } else if (message instanceof CorporationOfferMessage msg) {
            handleCorporationOffer(msg, lastGameState);
        } else if (message instanceof CardOfferMessage msg) {
            handleCardOffer(msg, lastGameState);
        } else if (message instanceof ProductionPhaseMessage msg) {
            handleProductionPhaseMessage(msg, lastGameState);
        } else if (message instanceof FinalGreeneryOfferMessage msg) {
            handleFinalGreeneryOffer(msg, lastGameState);
        } else if (message instanceof GameOverMessage) {
            handleGameOver(lastGameState);
        }
    }

    private void handleGameState(GameState state) {
        Platform.runLater(() -> {
            synchronized (listeners) {
                for (GameStateListener listener : listeners) {
                    listener.onGameStateReceived(state);
                }
            }
        });
    }

    private void handleCorporationOffer(CorporationOfferMessage msg, GameState lastGameState) {
        Platform.runLater(() -> {
            String myName = ApplicationConfiguration.getInstance().getMyPlayerName();

            if (!myName.equals(msg.playerName()) || lastGameState == null) {
                return;
            }

            List<Corporation> offer = msg.corporationNames().stream()
                    .map(CorporationFactory::getCorporationByName)
                    .toList();

            GameManager gm = lastGameState.gameManager();
            Player me = gm.getPlayerByName(myName);

            ScreenNavigator.showChooseCorporationScreen(me, offer, gm);
        });
    }

    private void handleCardOffer(CardOfferMessage msg, GameState lastGameState) {
        Platform.runLater(() -> {
            String myName = ApplicationConfiguration.getInstance().getMyPlayerName();

            if (!myName.equals(msg.playerName()) || lastGameState == null) {
                return;
            }

            List<Card> offer = msg.cardNames().stream()
                    .map(CardFactory::getCardByName)
                    .toList();

            GameManager gm = lastGameState.gameManager();
            Player me = gm.getPlayerByName(myName);

            boolean isResearch = gm.getGeneration() > 1 || offer.size() <= 4;

            if (isResearch) {
                showResearchModal(me, offer, gm);
            } else {
                ScreenNavigator.showInitialCardDraftScreen(me, offer, gm);
            }
        });
    }

    private void showResearchModal(Player player, List<Card> offer, GameManager gm) {
        ScreenUtils.showAsModal(
                ScreenNavigator.getMainStage(),
                "ChooseCards.fxml",
                "Research Phase",
                (ChooseCardsController c) -> c.setup(player, offer, null, gm, true)
        );
    }

    private void handleProductionPhaseMessage(ProductionPhaseMessage msg, GameState lastGameState) {
        Platform.runLater(() -> {
            if (lastGameState == null) {
                log.error("Cannot show Production Phase - lastGameState is null");
                return;
            }


            var controller = ApplicationConfiguration.getInstance().getActiveGameController();
            if (controller == null) {
                log.error("Cannot show Production Phase - controller is null");
                return;
            }


            GameFlowManager gameFlowManager = controller.getActionManager().getGameFlowManager();
            if (gameFlowManager.getProductionPhaseManager() != null) {
                gameFlowManager.getProductionPhaseManager()
                        .handleProductionPhaseMessage(msg);
            } else {
                log.error("ProductionPhaseManager is null!");
            }
        });
    }

    private void handleFinalGreeneryOffer(FinalGreeneryOfferMessage msg, GameState lastGameState) {
        Platform.runLater(() -> {
            String myPlayerName = ApplicationConfiguration.getInstance().getMyPlayerName();

            if (!myPlayerName.equals(msg.playerName())) {
                return;
            }

            if (lastGameState == null) {
                log.error("Cannot open Final Greenery - lastGameState is null");
                return;
            }

            GameManager gameManager = lastGameState.gameManager();
            Player currentPlayer = gameManager.getPlayerByName(myPlayerName);

            if (currentPlayer == null) {
                log.error("Cannot find player {} in GameManager", myPlayerName);
                return;
            }

            var controller = ApplicationConfiguration.getInstance().getActiveGameController();

            if (controller == null) {
                log.error("Cannot open Final Greenery - controller is null");
                return;
            }

            log.debug("Client received FinalGreeneryOffer, opening modal for {}", myPlayerName);
            showFinalGreeneryModal(currentPlayer, gameManager, controller, myPlayerName);
        });
    }

    private void showFinalGreeneryModal(Player player, GameManager gm,
                                        GameScreenController controller, String playerName) {
        ScreenUtils.showAsModal(
                controller.getSceneWindow(),
                "FinalGreenery.fxml",
                "Final Greenery Conversion",
                (FinalGreeneryController c) -> c.setupSinglePlayer(
                        player, gm, controller,
                        () -> sendFinalGreeneryCompletion(playerName)
                )
        );
    }

    private void sendFinalGreeneryCompletion(String playerName) {
        log.debug("Client {} finished Final Greenery", playerName);

        GameMove completionMove = new GameMove(
                playerName,
                ActionType.FINISH_FINAL_GREENERY,
                "Final Greenery Complete",
                LocalDateTime.now(ZoneOffset.UTC)
        );
        client.sendMove(completionMove);
    }

    private void handleGameOver(GameState lastGameState) {
        Platform.runLater(() -> {
            if (lastGameState == null) {
                log.error("No game state available for Game Over");
                return;
            }

            GameManager gm = lastGameState.gameManager();
            List<Player> rankedPlayers = gm.calculateFinalScores();

            ScreenNavigator.showGameOverScreen(rankedPlayers);
        });
    }
}