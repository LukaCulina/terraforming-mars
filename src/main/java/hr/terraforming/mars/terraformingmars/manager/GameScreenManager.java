package hr.terraforming.mars.terraformingmars.manager;

import hr.terraforming.mars.terraformingmars.controller.game.GameScreenController;
import hr.terraforming.mars.terraformingmars.enums.*;
import hr.terraforming.mars.terraformingmars.model.GameBoard;
import hr.terraforming.mars.terraformingmars.model.GameManager;
import hr.terraforming.mars.terraformingmars.model.Player;
import hr.terraforming.mars.terraformingmars.service.CostService;
import hr.terraforming.mars.terraformingmars.view.HexBoardDrawer;
import hr.terraforming.mars.terraformingmars.view.component.ActionPanelComponents;
import hr.terraforming.mars.terraformingmars.view.component.GlobalStatusComponents;
import hr.terraforming.mars.terraformingmars.view.component.PlayerControlComponents;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Button;
import lombok.Getter;

import java.util.Map;

public class GameScreenManager {

    @Getter
    private final HexBoardDrawer hexBoardDrawer;
    private final GameScreenController controller;
    private final GlobalStatusComponents globalStatus;
    private final ActionPanelComponents actionPanels;
    private final PlayerControlComponents playerControls;

    public GameScreenManager(GameScreenController controller,
                             HexBoardDrawer hexBoardDrawer, GlobalStatusComponents globalStatus,
                             ActionPanelComponents actionPanels, PlayerControlComponents playerControls) {
        this.controller = controller;
        this.hexBoardDrawer = hexBoardDrawer;
        this.globalStatus = globalStatus;
        this.actionPanels = actionPanels;
        this.playerControls = playerControls;
    }

    private GameManager getGameManager() {
        return controller.getGameManager();
    }

    private GameBoard getGameBoard() {
        return controller.getGameBoard();
    }

    public void updateHexBoardDrawer() {
        if (hexBoardDrawer != null) {
            hexBoardDrawer.setGameBoard(getGameBoard());
        }
    }

    public void updateGeneralUI(Player viewedPlayer, boolean isPlacing, boolean isMyTurn) {
        updateGlobalParameters();
        updateStandardProjectButtonsState(isPlacing, isMyTurn);
        updateMilestoneButtonsState(isPlacing, isMyTurn);
        updateAwardButtonsState(isPlacing, isMyTurn);
        updatePlayerButtonsHighlight(viewedPlayer);
        updateConvertButtonsState(isPlacing, isMyTurn);
        drawBoard();

        GamePhase phase = getGameManager().getCurrentPhase();
        if (phase == GamePhase.ACTIONS || phase == GamePhase.FINAL_GREENERY) {
            Platform.runLater(() -> {
                if (controller.getGameBoardPane().getScene() != null) {
                    controller.getGameBoardPane().getScene().getRoot().setDisable(false);
                }
            });
        }
    }

    private void drawBoard() {
        if (hexBoardDrawer != null) {
            hexBoardDrawer.drawBoard();
        }
    }

    private void updateGlobalParameters() {
        globalStatus.generationLabel().setText("Generation: " + getGameManager().getGeneration());
        globalStatus.phaseLabel().setText("Phase: " + getGameManager().getCurrentPhase().toString());

        double oxygenProgress = (double) getGameBoard().getOxygenLevel() / GameBoard.MAX_OXYGEN;
        globalStatus.oxygenProgressBar().setProgress(oxygenProgress);
        globalStatus.oxygenLabel().setText(String.format("%d%%", getGameBoard().getOxygenLevel()));

        double tempProgress = (double) (getGameBoard().getTemperature() - GameBoard.MIN_TEMPERATURE) / (GameBoard.MAX_TEMPERATURE - GameBoard.MIN_TEMPERATURE);
        globalStatus.temperatureProgressBar().setProgress(tempProgress);
        globalStatus.temperatureLabel().setText(String.format("%d°C", getGameBoard().getTemperature()));

        if (globalStatus.oceansLabel() != null) {
            int oceans = getGameBoard().getOceansPlaced();
            int maxOceans = GameBoard.MAX_OCEANS;
            globalStatus.oceansLabel().setText(String.format("%d / %d", oceans, maxOceans));
        }
    }

    private void updateStandardProjectButtonsState(boolean isPlacing, boolean isMyTurn) {
        Player currentPlayer = getGameManager().getCurrentPlayer();

        boolean isActionPhase = getGameManager().getCurrentPhase() == GamePhase.ACTIONS;
        boolean canPerformAction = getGameManager().getActionsTakenThisTurn() < 2;

        boolean areControlsEnabled = isMyTurn && isActionPhase && canPerformAction && !isPlacing;

        actionPanels.standardProjectsFlow().getChildren().forEach(node -> {
            if (node instanceof Button button) {
                StandardProject project = (StandardProject) button.getUserData();
                if (project != null) {
                    int finalCost = CostService.getFinalProjectCost(project, currentPlayer);
                    boolean hasEnoughMC = currentPlayer.getMC() >= finalCost;

                    boolean shouldDisable = !areControlsEnabled || !hasEnoughMC;

                    if (project == StandardProject.AQUIFER) {
                        shouldDisable = shouldDisable || !getGameBoard().canPlaceOcean();
                    } else if (project == StandardProject.SELL_PATENTS) {
                        boolean hasCardsInHand = !currentPlayer.getHand().isEmpty();
                        shouldDisable = shouldDisable || !hasCardsInHand;
                    }

                    button.setDisable(shouldDisable);
                }
            }
        });
    }

    private void updateMilestoneButtonsState(boolean isPlacing, boolean isMyTurn) {
        Player currentPlayer = getGameManager().getCurrentPlayer();
        boolean isActionPhase = getGameManager().getCurrentPhase() == GamePhase.ACTIONS;
        Map<Milestone, Player> claimed = getGameBoard().getClaimedMilestones();

        actionPanels.milestonesBox().getChildren().forEach(node -> {
            if (node instanceof Button button) {
                Milestone milestone = (Milestone) button.getUserData();
                if (milestone != null) {
                    if (claimed.containsKey(milestone)) {
                        Player owner = claimed.get(milestone);
                        button.setText(milestone.getName() + " (" + owner.getName() + ")");
                        button.setDisable(true);
                        button.getStyleClass().add("project-milestone-claimed");

                    } else {
                        boolean canAfford = currentPlayer.getMC() >= 8;
                        boolean requirementMet = milestone.canClaim(currentPlayer);
                        button.setDisable(!isMyTurn || isPlacing || !isActionPhase || !canAfford || !requirementMet || claimed.size() >= GameBoard.MAX_MILESTONES);
                        button.setText(milestone.getName());
                        button.setStyle("");
                    }
                }
            }
        });
    }

    private void updateAwardButtonsState(boolean isPlacing, boolean isMyTurn) {
        Player currentPlayer = getGameManager().getCurrentPlayer();
        boolean isActionPhase = getGameManager().getCurrentPhase() == GamePhase.ACTIONS;
        Map<Award, Player> funded = getGameBoard().getFundedAwards();
        int currentCost = getGameBoard().getNextAwardCost();

        actionPanels.awardsBox().getChildren().forEach(node -> {
            if (node instanceof Button button) {
                updateSingleAwardButton(button, isMyTurn, isPlacing, isActionPhase, currentPlayer, funded, currentCost);
            }
        });
    }

    private void updateSingleAwardButton(Button button, boolean isMyTurn, boolean isPlacing, boolean isActionPhase,
                                         Player currentPlayer, Map<Award, Player> funded, int currentCost) {
        Award award = (Award) button.getUserData();
        if (award == null) {
            return;
        }

        if (funded.containsKey(award)) {
            Player owner = funded.get(award);
            button.setText(award.getName() + " (" + owner.getName() + ")");
            button.setDisable(true);
            button.getStyleClass().add("project-award-funded");
            return;
        }

        boolean maxAwardsReached = funded.size() >= GameBoard.MAX_AWARDS;
        boolean canAfford = currentPlayer.getMC() >= currentCost;

        button.setDisable(!isMyTurn || isPlacing || !isActionPhase || !canAfford || maxAwardsReached);

        if (!maxAwardsReached) {
            button.setText(award.getName() + " (" + currentCost + " MC)");
        } else {
            button.setText(award.getName());
        }
    }

    private void updatePlayerButtonsHighlight(Player viewedPlayer) {
        Player currentPlayer = getGameManager().getCurrentPlayer();

        for (Node node : playerControls.playerListBar().getChildren()) {
            if (node instanceof Button button) {
                String buttonPlayerName = button.getText();
                button.getStyleClass().removeAll("player-button-active", "player-button-viewed");

                if (currentPlayer.getName().equals(buttonPlayerName)) {
                    button.getStyleClass().add("player-button-active");
                }
                if (viewedPlayer != null && viewedPlayer.getName().equals(buttonPlayerName)) {
                    button.getStyleClass().add("player-button-viewed");
                }
            }
        }
    }

    private void updateConvertButtonsState(boolean isPlacing, boolean isMyTurn) {
        Player currentPlayer = getGameManager().getCurrentPlayer();

        boolean isActionPhase = getGameManager().getCurrentPhase() == GamePhase.ACTIONS;
        boolean canPerformAction = getGameManager().getActionsTakenThisTurn() < 2;
        boolean areControlsEnabled = isMyTurn && isActionPhase && canPerformAction && !isPlacing;

        Button convertHeatBtn = playerControls.convertHeatButton();
        Button convertPlantsBtn = playerControls.convertPlantsButton();

        if (convertHeatBtn != null) {
            boolean hasEnoughHeat = currentPlayer.resourceProperty(ResourceType.HEAT).get() >= 8;
            boolean isTemperatureMaxed = getGameBoard().getTemperature() >= GameBoard.MAX_TEMPERATURE;
            convertHeatBtn.setDisable(!areControlsEnabled || !hasEnoughHeat || isTemperatureMaxed);
        }

        if (convertPlantsBtn != null) {
            boolean hasEnoughPlants = currentPlayer.resourceProperty(ResourceType.PLANTS).get() >= currentPlayer.getGreeneryCost();
            convertPlantsBtn.setDisable(!areControlsEnabled || !hasEnoughPlants);
        }
    }
}