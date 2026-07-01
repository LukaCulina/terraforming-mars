package hr.terraforming.mars.terraformingmars.coordinator;

import javafx.scene.control.ProgressBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class ResponsiveLayoutCoordinator {

    private static final double RESPONSIVE_THRESHOLD = 600.0;

    private final BorderPane gameBoardPane;
    private final VBox topWrapper;
    private final VBox bottomWrapper;
    private final VBox standardProjectsBox;
    private final StackPane temperaturePane;
    private final ProgressBar temperatureProgressBar;
    private final ProgressBar oxygenProgressBar;

    private boolean isCompactMode = false;

    public ResponsiveLayoutCoordinator(BorderPane gameBoardPane, VBox topWrapper, VBox bottomWrapper,
                                       VBox standardProjectsBox, StackPane temperaturePane,
                                       ProgressBar temperatureProgressBar, ProgressBar oxygenProgressBar) {
        this.gameBoardPane = gameBoardPane;
        this.topWrapper = topWrapper;
        this.bottomWrapper = bottomWrapper;
        this.standardProjectsBox = standardProjectsBox;
        this.temperaturePane = temperaturePane;
        this.temperatureProgressBar = temperatureProgressBar;
        this.oxygenProgressBar = oxygenProgressBar;
    }

    public void attach() {
        gameBoardPane.widthProperty().addListener((_, _, newWidth) -> {
            double currentWidth = newWidth.doubleValue();

            if (currentWidth < RESPONSIVE_THRESHOLD && !isCompactMode) {
                switchToCompactLayout();
            } else if (currentWidth >= RESPONSIVE_THRESHOLD && isCompactMode) {
                switchToWideLayout();
            }
        });
    }

    private void switchToCompactLayout() {
        isCompactMode = true;

        gameBoardPane.setLeft(null);
        gameBoardPane.setRight(null);
        standardProjectsBox.prefWidthProperty().unbind();
        standardProjectsBox.setPrefWidth(Region.USE_COMPUTED_SIZE);

        topWrapper.getChildren().add(standardProjectsBox);
        bottomWrapper.getChildren().addFirst(temperaturePane);

        temperatureProgressBar.setRotate(0);
        temperatureProgressBar.setPrefHeight(30.0);
        temperatureProgressBar.prefWidthProperty().unbind();
        temperatureProgressBar.prefWidthProperty().bind(oxygenProgressBar.widthProperty());

        oxygenProgressBar.setPrefHeight(30.0);
    }

    private void switchToWideLayout() {
        isCompactMode = false;
        standardProjectsBox.prefWidthProperty().bind(gameBoardPane.widthProperty().multiply(0.15));

        topWrapper.getChildren().remove(standardProjectsBox);
        bottomWrapper.getChildren().remove(temperaturePane);

        gameBoardPane.setLeft(standardProjectsBox);
        gameBoardPane.setRight(temperaturePane);

        temperatureProgressBar.setRotate(-90);
        temperatureProgressBar.setPrefHeight(70.0);
        temperatureProgressBar.prefWidthProperty().unbind();
        temperatureProgressBar.prefWidthProperty().bind(gameBoardPane.widthProperty().multiply(0.6));

        oxygenProgressBar.setPrefHeight(40.0);
    }
}