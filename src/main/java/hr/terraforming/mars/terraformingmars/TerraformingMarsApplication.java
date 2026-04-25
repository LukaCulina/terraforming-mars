package hr.terraforming.mars.terraformingmars;

import hr.terraforming.mars.terraformingmars.config.ResourceConfig;
import hr.terraforming.mars.terraformingmars.factory.CardFactory;
import hr.terraforming.mars.terraformingmars.util.ScreenUtils;
import hr.terraforming.mars.terraformingmars.util.XmlUtils;
import hr.terraforming.mars.terraformingmars.view.ScreenNavigator;
import javafx.application.Application;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class TerraformingMarsApplication extends Application {
    @Override
   public void start(Stage stage) {

        XmlUtils.clearGameMoves();

        ResourceConfig config = new ResourceConfig(
                "/hr/terraforming/mars/terraformingmars/",
                "/hr/terraforming/mars/terraformingmars/css/styles.css",
                "/hr/terraforming/mars/terraformingmars/data/cards.json"
        );

        ScreenUtils.setConfig(config);
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
    }
    @SuppressWarnings("unused")
    static void main(String[] args) {
        launch();
    }
}