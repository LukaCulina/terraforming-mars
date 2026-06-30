package hr.terraforming.mars.terraformingmars.manager;

import hr.terraforming.mars.terraformingmars.controller.game.GameScreenController;
import hr.terraforming.mars.terraformingmars.controller.game.SellPatentsController;
import hr.terraforming.mars.terraformingmars.enums.*;
import hr.terraforming.mars.terraformingmars.model.ApplicationConfiguration;
import hr.terraforming.mars.terraformingmars.model.Card;
import hr.terraforming.mars.terraformingmars.model.GameManager;
import hr.terraforming.mars.terraformingmars.model.GameMove;
import hr.terraforming.mars.terraformingmars.thread.SaveNewGameMoveThread;
import hr.terraforming.mars.terraformingmars.util.ScreenUtils;
import hr.terraforming.mars.terraformingmars.util.XmlUtils;
import javafx.application.Platform;
import javafx.stage.Window;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
public class ActionManager {

    @Getter
    private final GameScreenController controller;
    @Getter
    private final GameFlowManager gameFlowManager;
    @Getter
    private final GameMoveManager gameMoveManager;
    private final ExecutionManager executionManager;

    public ActionManager(GameScreenController controller,
                         GameFlowManager gameFlowManager) {
        this.controller = controller;
        this.gameFlowManager = gameFlowManager;
        this.gameMoveManager = new GameMoveManager(this);
        this.executionManager = new ExecutionManager(controller, this, gameFlowManager);
        ProductionPhaseManager productionPhaseManager = new ProductionPhaseManager(
                controller,
                gameFlowManager,
                this
        );
        gameFlowManager.setProductionPhaseManager(productionPhaseManager);
    }

    private GameManager getGameManager() {
        return controller.getGameManager();
    }

    public void processMove(GameMove move) {
        gameMoveManager.processMove(move);
    }

    public void saveMove(GameMove move) {
        if (move.actionType() != ActionType.AUTO_PASS) {
            XmlUtils.appendGameMove(move);
            new Thread(new SaveNewGameMoveThread(move)).start();
        }

        controller.onLocalPlayerMove(move);
    }

    public void performAction() {
        getGameManager().incrementActionsTaken();

        PlayerType playerType = ApplicationConfiguration.getInstance().getPlayerType();

        if (playerType == PlayerType.LOCAL || playerType == PlayerType.HOST) {
            controller.refreshGameScreen();
        }

        if (getGameManager().getActionsTakenThisTurn() >= 2) {
            log.info("Player has taken 2 actions. Automatically passing turn.");
            if (getGameManager().getCurrentPhase() == GamePhase.ACTIONS) {
                Platform.runLater(() -> {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException _) {
                        Thread.currentThread().interrupt();
                    }
                    executionManager.handlePassTurn(true);
                });
            } else {
                log.info("Skipping auto-pass - phase: {}", getGameManager().getCurrentPhase());
            }
        }
    }


    public void handlePassTurn() {
        executionManager.handlePassTurn(false);
    }

    public void handlePlayCard(Card card) {
        executionManager.handlePlayCard(card);
    }

    public void handleClaimMilestone(Milestone milestone) {
        executionManager.handleClaimMilestone(milestone);
    }

    public void handleStandardProject(StandardProject project) {
        executionManager.handleStandardProject(project);
    }

    public void handleConvertHeat() {
        executionManager.handleConvertHeat();
    }

    public void handleConvertPlants() {
        executionManager.handleConvertPlants();
    }

    public void handleSellPatents() {
        Consumer<List<Card>> onSaleCompleteAction = soldCards -> {

            int count = soldCards.size();
            String patent = (count == 1) ? "patent" : "patents";
            String message = "sold " + count + " " + patent + " for " + count + " MC";

            String cardNames = soldCards.stream()
                    .map(Card::getName)
                    .collect(Collectors.joining(", "));

            GameMove showModal = new GameMove(
                    getGameManager().getCurrentPlayer().getName(),
                    ActionType.OPEN_SELL_PATENTS_MODAL,
                    cardNames,
                    LocalDateTime.now(ZoneOffset.UTC)
            );

            saveMove(showModal);

            GameMove move = new GameMove(
                    getGameManager().getCurrentPlayer().getName(),
                    ActionType.SELL_PATENTS,
                    cardNames,
                    message,
                    LocalDateTime.now(ZoneOffset.UTC)
            );

            performAction();
            saveMove(move);
        };

        Window owner = controller.getHexBoardPane().getScene().getWindow();

        ScreenUtils.showAsModal(
                owner,
                "SellPatents.fxml",
                "Sell Patents",
                (SellPatentsController c) -> c.setupForSale(getGameManager().getCurrentPlayer(), onSaleCompleteAction)
        );
    }
}