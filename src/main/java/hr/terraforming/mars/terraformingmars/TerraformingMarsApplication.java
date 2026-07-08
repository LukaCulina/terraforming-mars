package hr.terraforming.mars.terraformingmars;

import hr.terraforming.mars.terraformingmars.config.ResourceConfig;
import hr.terraforming.mars.terraformingmars.factory.CardFactory;
import hr.terraforming.mars.terraformingmars.util.ScreenUtils;
import hr.terraforming.mars.terraformingmars.util.XmlUtils;
import hr.terraforming.mars.terraformingmars.view.ScreenNavigator;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;

public class TerraformingMarsApplication extends Application {
    private static final String FONT_SIZE = "-fx-font-size: ";

    @SuppressWarnings("unused")
    static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage stage) {

        XmlUtils.clearGameMoves();

        ResourceConfig config = new ResourceConfig(
                "/hr/terraforming/mars/terraformingmars/fxml/",
                "/hr/terraforming/mars/terraformingmars/css/styles.css",
                "/hr/terraforming/mars/terraformingmars/data/cards.json"
        );

        ScreenUtils.setConfig(config);
        ScreenUtils.setStageWidthSupplier(stage::getWidth);
        CardFactory.setConfig(config);
        CardFactory.loadAllCards();

        stage.setTitle("Terraforming Mars");

        stage.setMinWidth(750);
        stage.setMinHeight(650);

        stage.setMaximized(true);
        stage.setResizable(true);
        stage.setHeight(Screen.getPrimary().getVisualBounds().getHeight());
        stage.setWidth(Screen.getPrimary().getVisualBounds().getWidth());

        stage.setFullScreenExitHint("");

        ScreenNavigator.setMainStage(stage);

        ScreenNavigator.showStartMenu();

        stage.show();

        PauseTransition fontResizeDebounce = new PauseTransition(Duration.millis(150));
        fontResizeDebounce.setOnFinished(_ -> {
            if (stage.getScene() != null) {
                double fontSize = Math.max(10, stage.getWidth() * 0.007);
                stage.getScene().getRoot().setStyle(FONT_SIZE + fontSize + "px;");
            }
        });

        stage.widthProperty().addListener((_, _, _) -> fontResizeDebounce.playFromStart());
    }
}