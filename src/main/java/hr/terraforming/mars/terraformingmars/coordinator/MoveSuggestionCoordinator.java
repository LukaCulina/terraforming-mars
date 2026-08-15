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
import javafx.scene.control.TextInputDialog;
import javafx.stage.Window;

import java.util.Optional;

public class MoveSuggestionCoordinator {
    private final MoveSuggestionService moveSuggestionService = new MoveSuggestionService();

    public void requestSuggestion(Button moveSuggestionButton, GameManager gameManager, GameBoard gameBoard, Window ownerWindow) {
        if (moveSuggestionButton.isDisabled()) return;

        if (!moveSuggestionService.hasValidApiKey()) {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Gemini API Key Required");
            dialog.setHeaderText("To use the AI suggestion feature, you need a Google Gemini API Key.");
            dialog.setContentText("Please enter your API Key:");

            Optional<String> result = dialog.showAndWait();
            if (result.isPresent() && !result.get().isBlank()) {
                moveSuggestionService.setApiKey(result.get().trim());
            } else {
                return;
            }
        }

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
