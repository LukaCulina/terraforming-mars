package hr.terraforming.mars.terraformingmars.manager;

import hr.terraforming.mars.terraformingmars.controller.game.GameScreenController;
import hr.terraforming.mars.terraformingmars.enums.*;
import hr.terraforming.mars.terraformingmars.exception.GameStateException;
import hr.terraforming.mars.terraformingmars.model.*;
import hr.terraforming.mars.terraformingmars.service.CostService;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Slf4j
public class ExecutionManager {

    private static final int MILESTONE_COST = 8;
    private static final int CONVERSION_COST = 8;
    private final GameScreenController controller;
    private final ActionManager actionManager;
    private final GameFlowManager gameFlowManager;

    public ExecutionManager(GameScreenController controller, ActionManager actionManager, GameFlowManager gameFlowManager) {
        this.controller = controller;
        this.actionManager = actionManager;
        this.gameFlowManager = gameFlowManager;
    }

    private GameManager getGameManager() {
        return controller.getGameManager();
    }

    private GameBoard getGameBoard() {
        return controller.getGameBoard();
    }

    private boolean isLocalPlayerMove(Player player) {
        if (ApplicationConfiguration.getInstance().getPlayerType() == PlayerType.LOCAL) {
            return true;
        }
        String myName = ApplicationConfiguration.getInstance().getMyPlayerName();
        return player.getName().equals(myName);
    }

    public void handlePassTurn(boolean isAutoPass) {
        if (getGameManager().getCurrentPhase() != GamePhase.ACTIONS) return;

        String currentPlayerName = getGameManager().getCurrentPlayer().getName();
        boolean allPlayersPassed = getGameManager().passTurn();

        ActionType actionType = isAutoPass ? ActionType.AUTO_PASS : ActionType.PASS_TURN;
        String message = isAutoPass ? "auto-passed after 2 actions" : "passed their turn";
        GameMove move = new GameMove(currentPlayerName, actionType, "", message, LocalDateTime.now(ZoneOffset.UTC)
        );
        actionManager.saveMove(move);

        if (!isAutoPass) {
            log.info("{} manually passed turn", currentPlayerName);
        } else {
            log.debug("{} auto-passed after 2 actions", currentPlayerName);
        }

        if (allPlayersPassed) {
            PlayerType playerType = ApplicationConfiguration.getInstance().getPlayerType();
            if (playerType == PlayerType.HOST || playerType == PlayerType.LOCAL) {
                gameFlowManager.startProductionPhase();
            } else {
                log.info("All players passed, waiting for server to change phase.");
            }
        } else {
            controller.setViewedPlayer(getGameManager().getCurrentPlayer());
            controller.refreshGameScreen();
        }
    }

    public void handlePlayCard(Card card) {
        Player currentPlayer = getGameManager().getCurrentPlayer();

        if (!currentPlayer.canPlayCard(card)) {
            throw new GameStateException(
                    "Player '" + currentPlayer.getName() + "' cannot play card '" + card.getName() + "': requirements not met or insufficient MegaCredits"
            );
        }

        GameMove move = new GameMove(currentPlayer.getName(), ActionType.PLAY_CARD, card.getName()
                , "played card: " + card.getName(), LocalDateTime.now(ZoneOffset.UTC));

        if (card.getTileToPlace() != null) {
            if (isLocalPlayerMove(currentPlayer)) {
                controller.getPlacementManager().enterPlacementModeForCard(card, move);
            } else {
                currentPlayer.playCard(card, getGameManager());
            }
        } else {
            currentPlayer.playCard(card, getGameManager());
            actionManager.performAction();
            actionManager.saveMove(move);
        }
    }

    public void handleClaimMilestone(Milestone milestone) {
        Player currentPlayer = getGameManager().getCurrentPlayer();

        if (getGameBoard().canClaimMilestone(milestone, currentPlayer)) {
            currentPlayer.canSpendMC(MILESTONE_COST);
            actionManager.performAction();
            GameMove move = new GameMove(currentPlayer.getName(), ActionType.CLAIM_MILESTONE, milestone.name(),
                    "claimed milestone: " + milestone.name(), LocalDateTime.now(ZoneOffset.UTC));
            actionManager.saveMove(move);
        } else {
            log.warn("Failed attempt by {} to claim milestone '{}'.",
                    currentPlayer.getName(), milestone.getName());
        }
    }

    public void handleStandardProject(StandardProject project) {
        Player currentPlayer = getGameManager().getCurrentPlayer();
        int finalCost = CostService.getFinalProjectCost(project, currentPlayer);

        if (currentPlayer.getMC() < finalCost) {
            throw new GameStateException(
                    "Player '" + currentPlayer.getName() + "' cannot use standard project '" + project.getName() + "': insufficient MC (need " + finalCost
                            + ", have " + currentPlayer.getMC() + ")"
            );
        }

        GameMove move = new GameMove(getGameManager().getCurrentPlayer().getName(),
                ActionType.USE_STANDARD_PROJECT, project.name(), "used standard project: " + project.name(), LocalDateTime.now(ZoneOffset.UTC));

        if (project.requiresTilePlacement()) {
            if (isLocalPlayerMove(currentPlayer)) {
                controller.getPlacementManager().enterPlacementModeForProject(project, move);
            } else {
                currentPlayer.canSpendMC(finalCost);
            }
        } else {
            if (project == StandardProject.SELL_PATENTS) {
                if (currentPlayer.getHand().isEmpty()) {
                    throw new GameStateException(currentPlayer.getName() + " tried to sell patents but has no cards in hand.");
                }
                if (isLocalPlayerMove(currentPlayer)) {
                    actionManager.handleSellPatents();
                }
            } else {
                currentPlayer.canSpendMC(finalCost);
                project.execute(currentPlayer, getGameBoard());
                actionManager.performAction();
                actionManager.saveMove(move);
            }
        }
    }

    public void handleConvertHeat() {
        Player currentPlayer = getGameManager().getCurrentPlayer();

        if (currentPlayer.resourceProperty(ResourceType.HEAT).get() < CONVERSION_COST) {
            throw new GameStateException("Player '" + currentPlayer.getName() + "' cannot convert heat: insufficient resources.");
        }

        currentPlayer.resourceProperty(ResourceType.HEAT).set(
                currentPlayer.resourceProperty(ResourceType.HEAT).get() - CONVERSION_COST
        );

        getGameBoard().canIncreaseTemperature();
        currentPlayer.increaseTR(1);
        actionManager.performAction();

        GameMove move = new GameMove(
                currentPlayer.getName(),
                ActionType.CONVERT_HEAT,
                "",
                "raised the temperature",
                LocalDateTime.now(ZoneOffset.UTC));

        actionManager.saveMove(move);
    }

    public void handleConvertPlants() {
        Player currentPlayer = getGameManager().getCurrentPlayer();
        int requiredPlants = currentPlayer.getGreeneryCost();

        if (currentPlayer.resourceProperty(ResourceType.PLANTS).get() < requiredPlants) {
            throw new GameStateException("Player '" + currentPlayer.getName() + "' cannot convert plants: insufficient resources.");
        }

        GameMove convertPlantsMove = new GameMove(
                currentPlayer.getName(),
                ActionType.CONVERT_PLANTS,
                "",
                "converted " + requiredPlants + " plants to greenery",
                LocalDateTime.now(ZoneOffset.UTC)
        );

        if (isLocalPlayerMove(currentPlayer)) {
            controller.getPlacementManager().enterPlacementModeForPlant(convertPlantsMove);
        } else {
            currentPlayer.spendPlantsForGreenery();
        }
    }
}