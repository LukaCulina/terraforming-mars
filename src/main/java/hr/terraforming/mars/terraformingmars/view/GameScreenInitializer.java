package hr.terraforming.mars.terraformingmars.view;

import hr.terraforming.mars.terraformingmars.controller.game.GameScreenController;
import hr.terraforming.mars.terraformingmars.manager.ActionManager;
import hr.terraforming.mars.terraformingmars.manager.GameScreenManager;
import hr.terraforming.mars.terraformingmars.view.component.ActionPanelComponents;
import hr.terraforming.mars.terraformingmars.view.component.GlobalStatusComponents;
import hr.terraforming.mars.terraformingmars.view.component.PlayerControlComponents;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class GameScreenInitializer {

    private GameScreenInitializer() {
        throw new IllegalStateException("Utility class");
    }

    public static GameScreenManager initialize(GameScreenController controller, ActionManager actionManager) {

        HexBoardDrawer hexBoardDrawer = new HexBoardDrawer(
                controller.getHexBoardPane(),
                controller.getGameBoard(),
                controller.getPlacementManager()
        );

        GlobalStatusComponents statusComponents = new GlobalStatusComponents(
                controller.oxygenProgressBar,
                controller.oxygenLabel,
                controller.temperatureProgressBar,
                controller.temperatureLabel,
                controller.oceansLabel,
                controller.generationLabel,
                controller.phaseLabel
        );

        ActionPanelComponents actionPanel = new ActionPanelComponents(
                controller.milestonesBox,
                controller.standardProjectsBox,
                controller.standardProjectsFlow
        );

        PlayerControlComponents controls = new PlayerControlComponents(
                controller.playerListBar,
                controller.passTurnButton,
                controller.convertHeatButton,
                controller.convertPlantsButton
        );

        GameScreenManager gameScreen = new GameScreenManager(
                controller,
                hexBoardDrawer,
                statusComponents,
                actionPanel,
                controls
        );

        initializeComponents(controller, actionManager, actionPanel, controls);
        initializeBindings(controller, statusComponents, actionPanel, controls);

        return gameScreen;
    }

    private static void initializeComponents(GameScreenController controller, ActionManager actionManager,
                                             ActionPanelComponents actionPanels, PlayerControlComponents playerControls) {
        ViewBuilder componentBuilder = new ViewBuilder(
                controller, actionManager, controller.getGameManager()
        );
        componentBuilder.createPlayerButtons(playerControls.playerListBar());
        componentBuilder.createMilestoneButtons(actionPanels.milestonesBox());
        componentBuilder.createStandardProjectButtons(actionPanels.standardProjectsFlow());

        playerControls.passTurnButton().setOnAction(_ -> actionManager.handlePassTurn());
        playerControls.convertHeatButton().setOnAction(_ -> actionManager.handleConvertHeat());
        playerControls.convertPlantsButton().setOnAction(_ -> actionManager.handleConvertPlants());
    }

    private static void initializeBindings(GameScreenController controller,
                                           GlobalStatusComponents globalStatus,
                                           ActionPanelComponents actionPanels,
                                           PlayerControlComponents playerControls) {
        BorderPane gameBoardPane = controller.gameBoardPane;
        BorderPane playerInterface = controller.playerInterface;
        GridPane bottomGrid = controller.bottomGrid;
        StackPane temperaturePane = controller.temperaturePane;

        playerControls.passTurnButton().prefWidthProperty()
                .bind(playerInterface.widthProperty().multiply(0.6));

        VBox conversionBox = (VBox) playerControls.convertHeatButton().getParent();
        playerControls.convertHeatButton().prefWidthProperty()
                .bind(conversionBox.widthProperty().multiply(0.8));
        playerControls.convertPlantsButton().prefWidthProperty()
                .bind(conversionBox.widthProperty().multiply(0.8));

        actionPanels.standardProjectsBox().prefWidthProperty()
                .bind(gameBoardPane.widthProperty().multiply(0.15));
        actionPanels.milestonesBox().prefWidthProperty()
                .bind(bottomGrid.widthProperty().multiply(0.4));

        globalStatus.oxygenProgressBar().prefWidthProperty()
                .bind(gameBoardPane.widthProperty().multiply(0.92));
        temperaturePane.prefWidthProperty()
                .bind(gameBoardPane.widthProperty().multiply(0.15));
        globalStatus.temperatureProgressBar().prefWidthProperty()
                .bind(gameBoardPane.widthProperty().multiply(0.6));
        globalStatus.oceansLabel().prefWidthProperty()
                .bind(bottomGrid.widthProperty().multiply(0.15));
        globalStatus.oceansLabel().prefHeightProperty()
                .bind(globalStatus.oceansLabel().prefWidthProperty());
    }
}
