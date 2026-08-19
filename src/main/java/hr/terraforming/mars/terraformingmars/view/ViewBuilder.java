package hr.terraforming.mars.terraformingmars.view;

import hr.terraforming.mars.terraformingmars.controller.game.GameScreenController;
import hr.terraforming.mars.terraformingmars.enums.Award;
import hr.terraforming.mars.terraformingmars.enums.Milestone;
import hr.terraforming.mars.terraformingmars.enums.StandardProject;
import hr.terraforming.mars.terraformingmars.manager.ActionManager;
import hr.terraforming.mars.terraformingmars.model.GameManager;
import hr.terraforming.mars.terraformingmars.model.Player;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Duration;

public record ViewBuilder(GameScreenController controller, ActionManager actionManager,
                          GameManager gameManager) {

    public void createPlayerButtons(HBox playerListBar) {
        playerListBar.getChildren().clear();

        for (Player player : gameManager.getPlayers()) {
            Button playerButton = new Button(player.getName());
            playerButton.getStyleClass().add("player-select-button");
            playerButton.setOnAction(_ -> controller.showPlayerBoard(player));
            playerListBar.getChildren().add(playerButton);
        }
    }

    public void createMilestoneButtons(VBox milestoneBox) {
        milestoneBox.getChildren().clear();
        Label milestoneLabel = new Label("Milestones");
        milestoneLabel.getStyleClass().add("project-milestone");
        milestoneBox.getChildren().add(milestoneLabel);

        for (Milestone milestone : Milestone.values()) {
            Button milestoneButton = new Button(milestone.getName());
            milestoneButton.prefWidthProperty().bind(milestoneBox.widthProperty().multiply(0.8));
            milestoneButton.getStyleClass().add("milestone-button");
            milestoneButton.setUserData(milestone);
            milestoneButton.setFocusTraversable(false);
            milestoneButton.setOnAction(_ -> actionManager.handleClaimMilestone(milestone));

            Tooltip tooltip = new Tooltip(milestone.getDescription() + "\n(Price: 8 MC)");
            tooltip.getStyleClass().add("tooltip");
            tooltip.setShowDelay(Duration.millis(300));
            milestoneButton.setTooltip(tooltip);

            milestoneBox.getChildren().add(milestoneButton);
        }
    }

    public void createAwardButtons(VBox awardsBox) {
        awardsBox.getChildren().clear();
        Label awardsLabel = new Label("Awards");
        awardsLabel.getStyleClass().add("project-award");
        awardsBox.getChildren().add(awardsLabel);

        for (Award award : Award.values()) {
            Button awardButton = new Button(award.getName());
            awardButton.prefWidthProperty().bind(awardsBox.widthProperty().multiply(0.8));
            awardButton.getStyleClass().add("award-button");
            awardButton.setUserData(award);
            awardButton.setFocusTraversable(false);
            awardButton.setOnAction(_ -> actionManager.handleFundAward(award));

            Tooltip tooltip = new Tooltip(award.getDescription());
            tooltip.setShowDelay(Duration.millis(300));
            awardButton.setTooltip(tooltip);

            awardsBox.getChildren().add(awardButton);
        }
    }

    public void createStandardProjectButtons(FlowPane standardProjectsFlow) {
        standardProjectsFlow.getChildren().clear();

        for (StandardProject project : StandardProject.values()) {
            Button projectButton = new Button(project.getName());
            projectButton.setPrefWidth(110);
            projectButton.getStyleClass().add("project-button");
            projectButton.setUserData(project);

            Text icon = new Text(project.getIcon());
            icon.getStyleClass().add("icon");
            projectButton.setGraphic(icon);
            Tooltip tooltip = new Tooltip(project.getDescription() + "\n(Price: " + project.getCost() + " MC)");
            tooltip.getStyleClass().add("tooltip");
            tooltip.setShowDelay(Duration.millis(300));
            projectButton.setTooltip(tooltip);

            projectButton.setOnAction(e -> actionManager.handleStandardProject((StandardProject) ((Button) e.getSource()).getUserData()));
            standardProjectsFlow.getChildren().add(projectButton);
        }
    }
}