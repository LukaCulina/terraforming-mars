package hr.terraforming.mars.terraformingmars.controller.game;

import hr.terraforming.mars.terraformingmars.model.ProductionReport;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import lombok.Setter;

import java.util.List;

public class ProductionPhaseController {

    @FXML
    private Label titleLabel;

    @FXML
    private VBox summariesContainer;

    @FXML
    private Button continueButton;

    @Setter
    private Runnable onContinueAction;

    public void loadProductionSummaries(List<ProductionReport> summaries, int generation) {
        titleLabel.setText("Production - Generation " + generation);

        for (ProductionReport summary : summaries) {
            VBox playerBox = createPlayerSummary(summary);
            summariesContainer.getChildren().add(playerBox);
        }
    }

    private VBox createPlayerSummary(ProductionReport summary) {
        VBox box = new VBox(8);
        box.getStyleClass().add("player-summary-box");

        Label header = new Label(summary.getPlayerName() + " - " + summary.getCorporationName());
        header.getStyleClass().add("player-header");
        box.getChildren().add(header);

        for (var entry : summary.getChanges().entrySet()) {
            Label change = new Label("  " + entry.getKey() + ": " + entry.getValue());
            change.getStyleClass().add("resource-change-label");
            box.getChildren().add(change);
        }

        return box;
    }

    public void replayShowProductionSummary(List<ProductionReport> summaries, int generation) {
        loadProductionSummaries(summaries, generation);

        if (continueButton != null) {
            continueButton.setVisible(false);
            continueButton.setManaged(false);
        }
        PauseTransition autoClose = new PauseTransition(Duration.seconds(1.5));
        autoClose.setOnFinished(_ -> closeWindow());
        autoClose.play();
    }

    private void closeWindow() {
        Stage stage = (Stage) titleLabel.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void onContinue() {
        if (onContinueAction != null) {
            onContinueAction.run();
        }

        ((Stage) titleLabel.getScene().getWindow()).close();
    }
}
