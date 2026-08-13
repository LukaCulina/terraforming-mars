package hr.terraforming.mars.terraformingmars.coordinator;

import hr.terraforming.mars.terraformingmars.controller.game.PlayerBoardController;
import hr.terraforming.mars.terraformingmars.enums.PlayerType;
import hr.terraforming.mars.terraformingmars.manager.ActionManager;
import hr.terraforming.mars.terraformingmars.manager.GameScreenManager;
import hr.terraforming.mars.terraformingmars.manager.PlacementManager;
import hr.terraforming.mars.terraformingmars.model.ApplicationConfiguration;
import hr.terraforming.mars.terraformingmars.model.GameManager;
import hr.terraforming.mars.terraformingmars.model.Player;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;

import java.util.List;

public class GameScreenCoordinator {

    public void refreshGameScreen(Player viewedPlayer, GameManager gameManager, PlacementManager placementManager,
                                  PlayerBoardController playerBoardController, ActionManager actionManager,
                                  GameScreenManager gameScreenManager) {

        if (gameManager == null || viewedPlayer == null || gameScreenManager == null) return;

        String myName = ApplicationConfiguration.getInstance().getMyPlayerName();

        boolean isMyTurn = gameManager.getCurrentPlayer().getName().equals(myName);

        if (ApplicationConfiguration.getInstance().getPlayerType() == PlayerType.LOCAL) {
            isMyTurn = true;
        }

        boolean isPlacing = (placementManager != null && placementManager.isPlacementMode());

        gameScreenManager.updateGeneralUI(viewedPlayer, isPlacing, isMyTurn);

        if (playerBoardController != null) {
            playerBoardController.setPlayer(viewedPlayer, actionManager);
        }

        gameScreenManager.getHexBoardDrawer().drawBoard();
    }


    public void updatePlayerHighlight(Player currentPlayer, HBox playerListBar) {
        for (Node node : playerListBar.getChildren()) {
            node.getStyleClass().remove("current-player-highlight");

            Object userData = node.getUserData();

            if (userData != null && userData.equals(currentPlayer.getName())) {
                node.getStyleClass().add("current-player-highlight");
            }
        }
    }

    public void setGameControlsEnabled(boolean isEnabled, PlayerBoardController playerBoardController,
                                       Button passTurnButton, Button moveSuggestionButton, Button convertHeatButton,
                                       Button convertPlantsButton) {

        List.of(passTurnButton, moveSuggestionButton, convertHeatButton, convertPlantsButton)
                .forEach(node -> {
                    if (node != null) node.setDisable(!isEnabled);
                });

        if (playerBoardController != null) {
            playerBoardController.setHandInteractionEnabled(isEnabled);
        }
    }
}