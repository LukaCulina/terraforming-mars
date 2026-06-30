package hr.terraforming.mars.terraformingmars.controller.game;

import hr.terraforming.mars.terraformingmars.enums.ResourceType;
import hr.terraforming.mars.terraformingmars.model.GameManager;
import hr.terraforming.mars.terraformingmars.model.Player;
import hr.terraforming.mars.terraformingmars.view.ScreenNavigator;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.util.Duration;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class FinalGreeneryController {

    @FXML
    private Label playerNameLabel;

    @FXML
    private Label plantsLabel;

    @FXML
    private Label greeneryCostLabel;

    @FXML
    private Button convertButton;

    @FXML
    private Button finishButton;

    private GameManager gameManager;
    private List<Player> players;
    private int currentPlayerIndex = 0;
    private Player currentPlayer;
    private GameScreenController gameScreenController;
    private Runnable onComplete;
    private Stage stage;

    public void setup(GameManager gameManager, GameScreenController gameScreenController) {
        this.players = gameManager.getPlayers();
        this.gameScreenController = gameScreenController;
        this.gameManager = gameManager;
        currentPlayerIndex = 0;
        Platform.runLater(() -> this.stage = (Stage) convertButton.getScene().getWindow());
        showCurrentPlayer();
    }

    public void setupSinglePlayer(Player player, GameManager gameManager,
                                  GameScreenController gameScreenController,
                                  Runnable onComplete) {
        this.currentPlayer = player;
        this.gameManager = gameManager;
        this.gameScreenController = gameScreenController;
        this.onComplete = onComplete;

        Platform.runLater(() -> {
            stage = (Stage) convertButton.getScene().getWindow();
            updateUI();
        });
    }

    private void showCurrentPlayer() {
        if (currentPlayerIndex >= players.size()) {
            onFinalGreeneryPhaseComplete();
            closeWindow();
            return;
        }

        currentPlayer = players.get(currentPlayerIndex);
        updateUI();
    }

    private void onFinalGreeneryPhaseComplete() {
        log.info("Final greenery conversion phase is complete. Proceeding to calculate final scores.");
        List<Player> rankedPlayers = gameManager.calculateFinalScores();
        Platform.runLater(() -> ScreenNavigator.showGameOverScreen(rankedPlayers));
    }

    private void updateUI() {
        if (currentPlayer == null) return;

        int plants = currentPlayer.resourceProperty(ResourceType.PLANTS).get();
        int cost = currentPlayer.getGreeneryCost();

        playerNameLabel.setText(currentPlayer.getName());
        plantsLabel.setText("🌿 Plants: " + plants);
        greeneryCostLabel.setText("(Cost: " + cost + ")");

        convertButton.setDisable(plants < cost);
    }

    @FXML
    private void handleConvertGreenery() {
        if (currentPlayer == null) return;

        int plants = currentPlayer.resourceProperty(ResourceType.PLANTS).get();
        int cost = currentPlayer.getGreeneryCost();
        int greeneryCount = plants / cost;

        log.info("Player {} converting {} plants into {} greenery tiles",
                currentPlayer.getName(), plants, greeneryCount);

        closeWindow();

        gameScreenController.getPlacementManager().startFinalGreeneryPlacement(
                currentPlayer,
                () -> {
                    log.info("All greenery tiles placed for {}", currentPlayer.getName());

                    if (onComplete != null) {
                        onComplete.run();
                    } else {
                        currentPlayerIndex++;
                        Platform.runLater(this::showCurrentPlayer);
                    }
                }
        );
    }

    @FXML
    private void handleFinish() {
        log.info("{} has finished their greenery conversion.", currentPlayer.getName());

        if (onComplete != null) {
            closeWindow();
            onComplete.run();
            return;
        }

        currentPlayerIndex++;
        showCurrentPlayer();
    }

    private void closeWindow() {
        if (stage != null) {
            stage.close();
        } else if (playerNameLabel != null && playerNameLabel.getScene() != null) {
            Stage windowToClose = (Stage) playerNameLabel.getScene().getWindow();

            if (windowToClose != null) {
                windowToClose.close();
            }
        }
    }

    public void replayShowFinalGreenery(String playerName, int plants, int cost) {
        playerNameLabel.setText(playerName);
        plantsLabel.setText("Plants: " + plants);
        greeneryCostLabel.setText("Cost: " + cost);

        convertButton.setVisible(false);
        finishButton.setVisible(false);

        PauseTransition autoClose = new PauseTransition(Duration.seconds(2));
        autoClose.setOnFinished(_ -> closeWindow());
        autoClose.play();
    }
}
