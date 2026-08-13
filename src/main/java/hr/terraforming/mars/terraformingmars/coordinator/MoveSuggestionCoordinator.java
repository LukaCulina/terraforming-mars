package hr.terraforming.mars.terraformingmars.coordinator;

import hr.terraforming.mars.terraformingmars.controller.game.MoveSuggestionController;
import hr.terraforming.mars.terraformingmars.model.GameBoard;
import hr.terraforming.mars.terraformingmars.model.GameManager;
import hr.terraforming.mars.terraformingmars.model.Player;
import hr.terraforming.mars.terraformingmars.service.MoveSuggestionService;
import hr.terraforming.mars.terraformingmars.util.DialogUtils;
import hr.terraforming.mars.terraformingmars.util.ScreenUtils;
import hr.terraforming.mars.terraformingmars.view.FxmlPaths;
import javafx.application.Platform;

import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.stage.Window;

public class MoveSuggestionCoordinator {
    private final MoveSuggestionService moveSuggestionService = new MoveSuggestionService(System.getenv("GEMINI_API_KEY"));

    public void requestSuggestion(Button moveSuggestionButton, GameManager gameManager, GameBoard gameBoard, Window ownerWindow) {
        if (moveSuggestionButton.isDisabled()) return;

        Player currentPlayer = gameManager.getCurrentPlayer();
        moveSuggestionButton.setDisable(true);
        moveSuggestionButton.setText("Thinking...");

        moveSuggestionService.suggestMove(currentPlayer, gameManager, gameBoard)
                .whenComplete((suggestion, throwable) -> Platform.runLater(() -> {
                    moveSuggestionButton.setDisable(false);
                    moveSuggestionButton.setText("Suggest Move");

                    if (throwable != null) {
                        DialogUtils.showDialog(Alert.AlertType.ERROR, "Error",
                                "Unable to generate a suggestion: " + throwable.getMessage());
                    } else {
                        ScreenUtils.showAsModal(
                                ownerWindow,
                                FxmlPaths.MOVE_SUGGESTION,
                                "Move Suggestion",
                                (MoveSuggestionController c) -> c.setSuggestion(suggestion)
                        );
                    }
                }));
    }
}
