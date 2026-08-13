package hr.terraforming.mars.terraformingmars.controller.game;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class MoveSuggestionController {

    @FXML
    private Label suggestionLabel;

    public void setSuggestion(String suggestion) {
        if (suggestion == null) {
            suggestionLabel.setText("");
            return;
        }
        String cleanText = suggestion.replaceAll("[*#_`]", "");
        suggestionLabel.setText(cleanText);
    }

    @FXML
    private void close() {
        Stage stage = (Stage) suggestionLabel.getScene().getWindow();
        if (stage != null) {
            stage.close();
        }
    }
}