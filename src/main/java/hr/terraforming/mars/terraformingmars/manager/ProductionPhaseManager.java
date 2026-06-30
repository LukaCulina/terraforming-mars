package hr.terraforming.mars.terraformingmars.manager;

import hr.terraforming.mars.terraformingmars.controller.game.GameScreenController;
import hr.terraforming.mars.terraformingmars.controller.game.ProductionPhaseController;
import hr.terraforming.mars.terraformingmars.enums.ActionType;
import hr.terraforming.mars.terraformingmars.enums.PlayerType;
import hr.terraforming.mars.terraformingmars.model.ApplicationConfiguration;
import hr.terraforming.mars.terraformingmars.model.GameMove;
import hr.terraforming.mars.terraformingmars.model.ProductionReport;
import hr.terraforming.mars.terraformingmars.network.message.ProductionPhaseMessage;
import hr.terraforming.mars.terraformingmars.service.ProductionReportService;
import hr.terraforming.mars.terraformingmars.util.ScreenUtils;
import javafx.application.Platform;
import javafx.stage.Window;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
public class ProductionPhaseManager {

    private final GameScreenController controller;
    private final GameFlowManager gameFlowManager;
    private final ActionManager actionManager;
    private final Set<String> readyPlayers = new HashSet<>();

    public ProductionPhaseManager(GameScreenController controller,
                                  GameFlowManager gameFlowManager,
                                  ActionManager actionManager) {
        this.controller = controller;
        this.gameFlowManager = gameFlowManager;
        this.actionManager = actionManager;
    }

    public void showProductionPhaseScreen() {
        log.info("Starting Production Phase for Generation {}", controller.getGameManager().getGeneration());

        List<ProductionReport> summaries = ProductionReportService.generateReports(
                controller.getGameManager()
        );
        int generation = controller.getGameManager().getGeneration();

        PlayerType playerType = ApplicationConfiguration.getInstance().getPlayerType();

        if (playerType == PlayerType.HOST) {
            ProductionPhaseMessage message = new ProductionPhaseMessage(summaries, generation);
            var server = ApplicationConfiguration.getInstance().getGameServer();
            if (server != null) {
                server.broadcastToAll(message);
                log.debug("Broadcasted ProductionPhaseMessage to all clients");
            } else {
                log.error("GameServer is null - cannot broadcast ProductionPhaseMessage!");
            }
        }

        Platform.runLater(() -> showProductionModal(summaries, generation));
    }

    public void handleProductionPhaseMessage(ProductionPhaseMessage message) {
        log.debug("Received production phase message for Generation {}", message.generation());
        Platform.runLater(() -> showProductionModal(message.summaries(), message.generation()));
    }


    private void showProductionModal(List<ProductionReport> summaries, int generation) {
        Window owner = controller.getHexBoardPane().getScene().getWindow();

        ScreenUtils.showAsModal(
                owner,
                "ProductionPhase.fxml",
                "Production Phase - Generation " + generation,
                (ProductionPhaseController c) -> {
                    c.loadProductionSummaries(summaries, generation);
                    c.setOnContinueAction(this::onContinueClicked);
                }
        );
    }

    private void onContinueClicked() {
        PlayerType playerType = ApplicationConfiguration.getInstance().getPlayerType();
        String myName = ApplicationConfiguration.getInstance().getMyPlayerName();

        if (playerType == PlayerType.LOCAL) {
            gameFlowManager.startResearchPhase();
            return;
        }

        if (playerType == PlayerType.HOST) {
            onPlayerContinue(myName);
        }

        GameMove continueMove = new GameMove(
                myName,
                ActionType.FINISH_PRODUCTION_PHASE,
                "",
                "started a new generation",
                LocalDateTime.now(ZoneOffset.UTC)
        );
        actionManager.saveMove(continueMove);
        log.debug("'{}' continued to research phase", myName);
    }

    public void onPlayerContinue(String playerName) {
        readyPlayers.add(playerName);

        if (readyPlayers.size() >= controller.getGameManager().getPlayers().size()) {
            log.info("All players ready - starting Research Phase");
            readyPlayers.clear();
            gameFlowManager.startResearchPhase();
        }
    }

    public void reset() {
        readyPlayers.clear();
    }
}