package hr.terraforming.mars.terraformingmars.manager;

import hr.terraforming.mars.terraformingmars.controller.game.GameScreenController;
import hr.terraforming.mars.terraformingmars.coordinator.FinalGreeneryCoordinator;
import hr.terraforming.mars.terraformingmars.enums.*;
import hr.terraforming.mars.terraformingmars.model.*;
import hr.terraforming.mars.terraformingmars.network.NetworkBroadcaster;
import hr.terraforming.mars.terraformingmars.service.PlacementService;
import javafx.application.Platform;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Slf4j
public class PlacementManager {
    private final GameScreenController gameScreenController;
    private final GameBoard gameBoard;
    private final GameManager gameManager;
    private final ActionManager actionManager;
    private final FinalGreeneryCoordinator finalGreeneryCoordinator;
    private GameMove moveInProgress;
    private PlacementMode placementMode = PlacementMode.NONE;
    private StandardProject projectToPlace = null;
    private Card cardToPlace = null;
    @Getter
    private TileType tileTypeToPlace = null;
    private Player finalGreeneryPlayer = null;
    private Runnable onPlacementCompleteCallback = null;

    public PlacementManager(GameScreenController gameScreenController, GameManager gameManager, GameBoard gameBoard, ActionManager actionManager) {
        this.gameScreenController = gameScreenController;
        this.gameManager = gameManager;
        this.gameBoard = gameBoard;
        this.actionManager = actionManager;
        this.finalGreeneryCoordinator = new FinalGreeneryCoordinator(this);
    }

    public boolean isPlacementMode() {
        return placementMode != PlacementMode.NONE;
    }

    public void enterPlacementModeForPlant(GameMove move) {
        enterPlacementMode(PlacementMode.PLANT_CONVERSION, TileType.GREENERY, move, null, null, null, null);
    }

    public void enterPlacementModeForProject(StandardProject project, GameMove move) {
        enterPlacementMode(PlacementMode.STANDARD_PROJECT, project.getTileType(), move, null, project, null, null);
    }

    public void enterPlacementModeForCard(Card card, GameMove move) {
        enterPlacementMode(PlacementMode.CARD, card.getTileToPlace(), move, card, null, null, null);
    }

    public void enterPlacementModeForFinalGreenery(Player player, Runnable callback) {
        enterPlacementMode(PlacementMode.FINAL_GREENERY, TileType.GREENERY, null, null, null, player, callback);
    }

    public void startFinalGreeneryPlacement(Player player, Runnable onComplete) {
        gameScreenController.setGameControlsEnabled(false);
        gameScreenController.setCancelButtonVisible(false);
        finalGreeneryCoordinator.startFinalGreeneryPlacement(player, onComplete);
    }

    private void enterPlacementMode(PlacementMode placementMode, TileType tileTypeToPlace, GameMove moveInProgress,
                                    Card cardToPlace, StandardProject projectToPlace, Player finalGreeneryPlayer, Runnable onPlacementCompleteCallback) {

        this.placementMode = placementMode;
        this.tileTypeToPlace = tileTypeToPlace;
        this.moveInProgress = moveInProgress;
        this.cardToPlace = cardToPlace;
        this.projectToPlace = projectToPlace;
        this.finalGreeneryPlayer = finalGreeneryPlayer;
        this.onPlacementCompleteCallback = onPlacementCompleteCallback;

        gameScreenController.drawBoard();
        if (placementMode != PlacementMode.FINAL_GREENERY) {
            gameScreenController.setGameControlsEnabled(false);
            gameScreenController.setCancelButtonVisible(true);
        }
    }

    public void executePlacement(Tile selectedTile) {
        Player placementOwner = getPlacementOwner();
        String myName = ApplicationConfiguration.getInstance().getMyPlayerName();
        boolean isLocalGame = ApplicationConfiguration.getInstance().getPlayerType() == PlayerType.LOCAL;

        if (placementOwner == null || (!isLocalGame && !placementOwner.getName().equals(myName))) {
            cancelPlacement();
            return;
        }

        boolean wasFinalGreenery = (placementMode == PlacementMode.FINAL_GREENERY);

        PlacementService placementService = new PlacementService(gameBoard);
        PlacementService.PlacementContext context = new PlacementService.PlacementContext(
                placementMode, selectedTile, placementOwner, gameManager,
                tileTypeToPlace, cardToPlace, projectToPlace
        );

        placementService.placeByMode(context);

        if (!wasFinalGreenery) {
            actionManager.performAction();
        }

        recordMoves(selectedTile, placementOwner);
        finishPlacement(wasFinalGreenery);
    }

    private void recordMoves(Tile tile, Player owner) {
        if (moveInProgress != null) {
            actionManager.saveMove(moveInProgress);
        }

        String message = switch (tileTypeToPlace) {
            case GREENERY -> "placed a greenery tile";
            case OCEAN -> "placed an ocean tile";
            case CITY -> "placed a city tile";
            default -> "placed a tile";
        };

        GameMove placeTileMove = new GameMove(
                owner.getName(),
                ActionType.PLACE_TILE,
                tileTypeToPlace.name(),
                message,
                tile.getRow(),
                tile.getCol(),
                tileTypeToPlace,
                LocalDateTime.now(ZoneOffset.UTC)
        );

        actionManager.saveMove(placeTileMove);
    }

    public void cancelPlacement() {
        if (finalGreeneryCoordinator.isActive()) {
            finalGreeneryCoordinator.cancel();
        }

        if (onPlacementCompleteCallback != null) {
            Platform.runLater(onPlacementCompleteCallback);
        }

        resetAllState();
        gameScreenController.setGameControlsEnabled(true);
        gameScreenController.drawBoard();
    }

    private void finishPlacement(boolean wasFinalGreenery) {
        resetPlacementState();
        if (!wasFinalGreenery) {
            gameScreenController.setGameControlsEnabled(true);
        }

        gameScreenController.drawBoard();

        var config = ApplicationConfiguration.getInstance();
        NetworkBroadcaster broadcaster = config.getBroadcaster();
        if (broadcaster != null) {
            broadcaster.broadcast();
        }

        if (wasFinalGreenery && onPlacementCompleteCallback != null) {
            Platform.runLater(onPlacementCompleteCallback);
        }
    }

    private void resetPlacementState() {
        placementMode = PlacementMode.NONE;
        projectToPlace = null;
        cardToPlace = null;
        tileTypeToPlace = null;
        moveInProgress = null;
        gameScreenController.setCancelButtonVisible(false);
    }

    private void resetAllState() {
        resetPlacementState();
        finalGreeneryPlayer = null;
        onPlacementCompleteCallback = null;
    }

    public Player getPlacementOwner() {
        if (moveInProgress != null && moveInProgress.playerName() != null) {
            Player movePlayer = gameManager.getPlayerByName(moveInProgress.playerName());
            if (movePlayer != null) {
                return movePlayer;
            }
        }

        if (placementMode == PlacementMode.FINAL_GREENERY) {
            return finalGreeneryPlayer;
        }

        return gameManager.getCurrentPlayer();
    }
}