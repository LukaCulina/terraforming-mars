package hr.terraforming.mars.terraformingmars.controller.game;

import hr.terraforming.mars.terraformingmars.coordinator.*;
import hr.terraforming.mars.terraformingmars.manager.*;
import hr.terraforming.mars.terraformingmars.model.*;
import hr.terraforming.mars.terraformingmars.replay.ReplayManager;
import hr.terraforming.mars.terraformingmars.service.GameStateService;
import hr.terraforming.mars.terraformingmars.util.DialogUtils;
import hr.terraforming.mars.terraformingmars.util.DocumentationUtils;
import hr.terraforming.mars.terraformingmars.util.GameMoveUtils;
import hr.terraforming.mars.terraformingmars.view.FxmlComponentLoader;
import hr.terraforming.mars.terraformingmars.view.FxmlPaths;
import hr.terraforming.mars.terraformingmars.view.ScreenNavigator;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Window;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GameScreenController {

    private final GameStateService gameStateService = new GameStateService();
    private final MoveSuggestionCoordinator moveSuggestionCoordinator = new MoveSuggestionCoordinator();
    @Getter
    @FXML
    public BorderPane gameBoardPane;
    @FXML
    public StackPane temperaturePane;
    @FXML
    public GridPane bottomGrid;
    @FXML
    public HBox playerListBar;
    @FXML
    public VBox standardProjectsBox;
    @FXML
    public FlowPane standardProjectsFlow;
    @FXML
    public ProgressBar oxygenProgressBar;
    @FXML
    public Label oxygenLabel;
    @FXML
    public ProgressBar temperatureProgressBar;
    @FXML
    public Label temperatureLabel;
    @FXML
    public Label generationLabel;
    @FXML
    public Label phaseLabel;
    @FXML
    public Button passTurnButton;
    @FXML
    public Button convertHeatButton;
    @FXML
    public Button convertPlantsButton;
    @FXML
    public Label oceansLabel;
    @FXML
    public VBox milestonesBox;
    @FXML
    public VBox awardsBox;
    @FXML
    public BorderPane playerInterface;
    @Setter
    @Getter
    public GameManager gameManager;
    @FXML
    public Button moveSuggestionButton;
    @Getter
    @FXML
    private AnchorPane hexBoardPane;
    @FXML
    private VBox currentPlayerBoardContainer;
    @Getter
    @FXML
    private Button cancelPlacementButton;
    @FXML
    private Label lastMoveLabel;
    @Getter
    @FXML
    private VBox chatBoxContainer;
    @Getter
    @FXML
    private ListView<String> chatListView;
    @Getter
    @FXML
    private TextField chatInput;
    @Setter
    @Getter
    private GameBoard gameBoard;
    @Setter
    @Getter
    private PlacementManager placementManager;
    @Setter
    @Getter
    private GameScreenManager gameScreenManager;
    @Setter
    @Getter
    private ActionManager actionManager;
    @Setter
    @Getter
    private ChatManager chatManager;
    @Setter
    @Getter
    private ReplayManager replayManager;
    @Setter
    private Player viewedPlayer = null;
    @Getter
    private Timeline moveHistoryTimeline;
    @FXML
    private VBox topWrapper;
    @FXML
    private VBox bottomWrapper;
    @FXML
    private Menu documentationMenu;
    @Getter
    private PlayerBoardController currentPlayerBoardController;
    @Getter
    private NetworkCoordinator networkCoordinator;
    @Getter
    private GameSetupCoordinator setupCoordinator;
    private GameScreenCoordinator gameScreenCoordinator;
    @Getter
    private ResponsiveLayoutCoordinator layoutCoordinator;

    @FXML
    private void initialize() {
        currentPlayerBoardController = FxmlComponentLoader.load(currentPlayerBoardContainer, FxmlPaths.PLAYER_BOARD);
        networkCoordinator = new NetworkCoordinator(this);
        setupCoordinator = new GameSetupCoordinator(this);
        gameScreenCoordinator = new GameScreenCoordinator();
        layoutCoordinator = new ResponsiveLayoutCoordinator(
                gameBoardPane, topWrapper, bottomWrapper,
                standardProjectsBox, temperaturePane,
                temperatureProgressBar, oxygenProgressBar
        );
        layoutCoordinator.attach();

        if (isDevelopmentMode()) {
            addDebugButtons();
        } else {
            documentationMenu.setVisible(false);
            documentationMenu.setDisable(true);
        }
    }

    private boolean isDevelopmentMode() {
        return !Boolean.getBoolean("app.packaged");
    }

    private void broadcastIfHost() {
        var config = ApplicationConfiguration.getInstance();
        if (config.getPlayerType() == hr.terraforming.mars.terraformingmars.enums.PlayerType.HOST) {
            var broadcaster = config.getBroadcaster();
            if (broadcaster != null) {
                broadcaster.broadcast();
            }
        }
    }

    public void setupGame(GameState gameState) {
        setupCoordinator.setupNewGame(gameState);
    }

    public void updateFromNetwork(GameState state) {
        networkCoordinator.handleNetworkUpdate(state);
    }

    public void onLocalPlayerMove(GameMove move) {
        networkCoordinator.broadcastMove(move);
    }

    public void startMoveHistory() {
        if (moveHistoryTimeline == null) {
            moveHistoryTimeline = GameMoveUtils.createLastMoveTimeline(lastMoveLabel);
            moveHistoryTimeline.play();
        }
    }

    public void refreshGameScreen() {
        gameScreenCoordinator.refreshGameScreen(viewedPlayer, gameManager, placementManager,
                currentPlayerBoardController, actionManager, gameScreenManager);
    }

    public void updatePlayerHighlightForCurrentPlayer() {
        if (gameManager != null && gameManager.getCurrentPlayer() != null) {
            gameScreenCoordinator.updatePlayerHighlight(gameManager.getCurrentPlayer(), playerListBar);
        }
    }

    public void setGameControlsEnabled(boolean isEnabled) {
        gameScreenCoordinator.setGameControlsEnabled(isEnabled, currentPlayerBoardController,
                passTurnButton, moveSuggestionButton, convertHeatButton, convertPlantsButton);
    }

    public void showPlayerBoard(Player player) {
        viewedPlayer = player;
        refreshGameScreen();
    }

    public Window getSceneWindow() {
        return hexBoardPane.getScene().getWindow();
    }

    public void drawBoard() {
        refreshGameScreen();
    }

    public void setCancelButtonVisible(boolean visible) {
        if (cancelPlacementButton != null && passTurnButton != null) {
            cancelPlacementButton.setVisible(visible);
            cancelPlacementButton.setManaged(visible);

            passTurnButton.setVisible(!visible);
            passTurnButton.setManaged(!visible);

            if (moveSuggestionButton != null) {
                moveSuggestionButton.setVisible(!visible);
                moveSuggestionButton.setManaged(!visible);
            }
        }
    }

    @FXML
    private void sendChatMessage() {
        if (chatManager != null) {
            chatManager.sendMessage();
        }
    }

    @FXML
    private void replayGame() {
        replayManager.startReplay();
    }

    public void startNewGame() {
        GameSessionManager.resetForNewGame();
        gameStateService.clearGameData();
        ScreenNavigator.showChooseModeScreen();
    }

    public void saveGame() {
        gameStateService.saveGame(gameManager, gameBoard);
    }

    // Testne metode koje omogućuju lagan dolazak do kraja igre

    public void loadGame() {
        GameState loadedState = gameStateService.loadGame();
        if (loadedState != null) {
            setupCoordinator.setupLoadedGame(loadedState);
            viewedPlayer = gameManager.getCurrentPlayer();
            refreshGameScreen();
            DialogUtils.showDialog(Alert.AlertType.INFORMATION, "Load Game Successful!", "The game has been successfully loaded!");
        }
    }

    public void generateHtmlDocumentation() {
        DocumentationUtils.generateDocumentation();
    }

    public void updateLastMoveLabel(GameMove lastGameMove) {
        GameMoveUtils.updateLastMoveLabel(lastMoveLabel, lastGameMove);
    }

    private void addDebugButtons() {
        HBox debugBox = new HBox(5);
        debugBox.setStyle("-fx-padding: 5;");

        Button triggerFinalBtn = new Button("Trigger Final Generation");
        triggerFinalBtn.setOnAction(_ -> debugTriggerFinalGeneration());

        debugBox.getChildren().add(triggerFinalBtn);

        if (bottomGrid != null) {
            bottomGrid.add(debugBox, 0, bottomGrid.getRowCount());
        }
    }

    @FXML
    private void debugTriggerFinalGeneration() {
        if (gameBoard != null) {
            gameBoard.setTemperature(GameBoard.MAX_TEMPERATURE);
            gameBoard.setOxygenLevel(GameBoard.MAX_OXYGEN);
            gameBoard.setOceansPlaced(GameBoard.MAX_OCEANS);
            log.info("All parameters set to MAXIMUM - Final Generation triggered!");
            refreshGameScreen();
            broadcastIfHost();
        }
    }

    @FXML
    private void requestMoveSuggestion() {
        moveSuggestionCoordinator.requestSuggestion(moveSuggestionButton, gameManager, gameBoard, getSceneWindow());
    }
}