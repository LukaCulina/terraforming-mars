package hr.terraforming.mars.terraformingmars.controller.game;

import hr.terraforming.mars.terraformingmars.manager.GameSessionManager;
import hr.terraforming.mars.terraformingmars.model.ApplicationConfiguration;
import hr.terraforming.mars.terraformingmars.model.Card;
import hr.terraforming.mars.terraformingmars.model.Player;
import hr.terraforming.mars.terraformingmars.util.DialogUtils;
import hr.terraforming.mars.terraformingmars.view.ScreenNavigator;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.List;

@Slf4j
public class GameOverController {

    @FXML
    private Label winnerLabel;

    @FXML
    private GridPane scoresGrid;

    public void setFinalScores(List<Player> players) {
        players.sort(Comparator.comparingInt(Player::getFinalScore).reversed()
                .thenComparing(Player::getMC, Comparator.reverseOrder()));

        Player winner = players.getFirst();
        winnerLabel.setText("Winner: " + winner.getName() + " with " + winner.getFinalScore() + " victory points!");

        for (int i = 0; i < players.size(); i++) {
            addPlayerRow(i + 1, players.get(i));
        }
    }

    private void addPlayerRow(int rank, Player player) {
        int rowIndex = scoresGrid.getRowCount();
        int col = 0;

        int cardPoints = player.getPlayed().stream().mapToInt(Card::getVictoryPoints).sum();

        scoresGrid.add(createNormalLabel(String.valueOf(rank)), col++, rowIndex);
        scoresGrid.add(createNormalLabel(player.getName()), col++, rowIndex);
        scoresGrid.add(createNormalLabel(String.valueOf(player.getTR())), col++, rowIndex);
        scoresGrid.add(createNormalLabel(String.valueOf(player.getMilestonePoints())), col++, rowIndex);
        scoresGrid.add(createNormalLabel(String.valueOf(player.getTilePoints())), col++, rowIndex);
        scoresGrid.add(createNormalLabel(String.valueOf(cardPoints)), col++, rowIndex);
        scoresGrid.add(createHeaderLabel(String.valueOf(player.getFinalScore())), col, rowIndex);
    }

    private Label createHeaderLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("header-label");
        label.setMaxWidth(Double.MAX_VALUE);

        return label;
    }

    private Label createNormalLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("normal-label");
        label.setMaxWidth(Double.MAX_VALUE);

        return label;
    }

    @FXML
    private void showReplay() {
        log.info("Starting replay from Game Over screen");

        GameScreenController controller = ApplicationConfiguration.getInstance().getActiveGameController();

        if (controller != null && controller.getReplayManager() != null) {
            ScreenNavigator.showGameScreen(controller);
            controller.getReplayManager().startReplay();
        } else {
            DialogUtils.showDialog(Alert.AlertType.ERROR, "Replay Error", "Game controller or replay manager is not available.");
        }
    }

    @FXML
    private void goToMainMenu() {
        log.info("Returning to Main Menu from Game Over screen");

        GameSessionManager.resetForNewGame();
        ScreenNavigator.showStartMenu();
    }
}