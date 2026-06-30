package hr.terraforming.mars.terraformingmars.manager;

import hr.terraforming.mars.terraformingmars.controller.game.GameScreenController;
import hr.terraforming.mars.terraformingmars.enums.ActionType;
import hr.terraforming.mars.terraformingmars.enums.GamePhase;
import hr.terraforming.mars.terraformingmars.model.*;
import hr.terraforming.mars.terraformingmars.network.NetworkBroadcaster;
import hr.terraforming.mars.terraformingmars.util.XmlUtils;
import hr.terraforming.mars.terraformingmars.view.ScreenNavigator;
import javafx.application.Platform;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class GameFlowManager {
    private static final String SYSTEM = "System";

    private final GameScreenController controller;
    @Getter
    private FinalGreeneryPhaseManager finalGreeneryManager;
    @Getter
    @Setter
    private ProductionPhaseManager productionPhaseManager;

    public GameFlowManager(GameScreenController controller) {
        this.controller = controller;
    }

    private GameManager getGameManager() {
        return controller.getGameManager();
    }

    private GameBoard getGameBoard() {
        return controller.getGameBoard();
    }

    public void startProductionPhase() {
        log.info("All players have passed. Starting Production Phase.");
        getGameManager().setCurrentPhase(GamePhase.PRODUCTION);
        saveProductionPhaseForReplay(getGameManager().getGeneration());

        getGameManager().doProduction();

        controller.refreshGameScreen();

        if (getGameBoard().isFinalGeneration()) {
            log.info("This was the last generation. Starting final greenery conversion phase.");

            getGameManager().resetDraftPhase();

            finalGreeneryManager = new FinalGreeneryPhaseManager(
                    getGameManager(),
                    controller.getSceneWindow(),
                    controller,
                    this::finishGame
            );
            finalGreeneryManager.start();
        } else {
            if (productionPhaseManager != null) {
                productionPhaseManager.reset();
                productionPhaseManager.showProductionPhaseScreen();
            } else {
                log.error("ProductionPhaseManager is null! Skipping modal.");
                startNewGeneration();
            }
        }
    }

    private void startNewGeneration() {
        log.info("Production phase is over. Starting a new generation.");

        getGameManager().startNewGeneration();
        getGameManager().resetDraftPhase();

        if (getGameManager().getGeneration() > 1) {
            executePlayerOrderPhase();
        }

        controller.setViewedPlayer(getGameManager().getFirstPlayer());
        controller.refreshGameScreen();

        var config = ApplicationConfiguration.getInstance();
        var playerType = config.getPlayerType();

        switch (playerType) {
            case HOST -> {
                log.info("Starting distributed research phase.");
                config.getGameServer().distributeResearchCards();
            }
            case CLIENT -> log.info("Waiting for research cards from host");
            case LOCAL -> {
                log.info("Starting local research phase manager.");
                new ResearchPhaseManager(getGameManager(), controller.getSceneWindow(), controller, this::finishResearchPhase
                ).start();
            }
            default -> log.warn("Unknown or null PlayerType in startNewGeneration(): {}", playerType);
        }
    }

    private void executePlayerOrderPhase() {
        Player previousFirstPlayer = getGameManager().getFirstPlayer();
        getGameManager().rotateFirstPlayer();
        Player newFirstPlayer = getGameManager().getFirstPlayer();

        log.info("First player changed: {} → {}", previousFirstPlayer.getName(), newFirstPlayer.getName());

        GameMove playerOrderMove = new GameMove(SYSTEM, ActionType.PLAYER_ORDER, newFirstPlayer.getName(),
                "First player is now " + newFirstPlayer.getName(), LocalDateTime.now(ZoneOffset.UTC)
        );
        XmlUtils.appendGameMove(playerOrderMove);
    }

    public void startResearchPhase() {
        log.info("Production phase complete. Starting Research Phase.");
        startNewGeneration();
    }

    public void finishResearchPhase() {
        if (getGameManager().getCurrentPhase() == GamePhase.ACTIONS) {
            log.warn("Already in Actions phase, skipping duplicate beginActionPhase()");
            return;
        }

        saveResearchPhaseForReplay(getGameManager());

        log.info("Research phase complete. Starting Action Phase.");

        getGameManager().beginActionPhase();

        controller.refreshGameScreen();

        var config = ApplicationConfiguration.getInstance();

        if (config.getPlayerType() == hr.terraforming.mars.terraformingmars.enums.PlayerType.HOST) {
            NetworkBroadcaster broadcaster = config.getBroadcaster();

            if (broadcaster != null) {
                log.info("Broadcasting game state after research complete");
                broadcaster.broadcast();
            }
        }
    }

    private void finishGame() {
        log.info("Final Greenery phase complete. Calculating final scores.");

        List<Player> rankedPlayers = getGameManager().calculateFinalScores();

        Platform.runLater(() -> ScreenNavigator.showGameOverScreen(rankedPlayers));
    }


    private void saveProductionPhaseForReplay(int generation) {
        GameMove productionMove = new GameMove(SYSTEM, ActionType.OPEN_PRODUCTION_PHASE_MODAL, String.valueOf(generation),
                "Production Phase - Generation " + generation, LocalDateTime.now(ZoneOffset.UTC)
        );

        XmlUtils.appendGameMove(productionMove);
        log.debug("Production Phase saved for replay.");
    }

    private void saveResearchPhaseForReplay(GameManager gameManager) {
        Map<String, Object> researchData = new HashMap<>();

        for (Player player : gameManager.getPlayers()) {
            Map<String, Object> playerData = new HashMap<>();
            playerData.put("hand", player.getHand().stream().map(Card::getName).toList());
            researchData.put(player.getName(), playerData);
        }

        String jsonDetails = new com.google.gson.Gson().toJson(researchData);

        GameMove researchMove = new GameMove(SYSTEM, ActionType.RESEARCH_COMPLETE, jsonDetails, LocalDateTime.now(ZoneOffset.UTC)
        );

        XmlUtils.appendGameMove(researchMove);
        log.debug("RESEARCH_PHASE_COMPLETE snapshot saved to XML.");
    }
}