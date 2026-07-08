package hr.terraforming.mars.terraformingmars.util;

import hr.terraforming.mars.terraformingmars.config.ResourceConfig;
import hr.terraforming.mars.terraformingmars.exception.FxmlLoadException;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.function.Consumer;

@Slf4j
public class ScreenUtils {

    private static final int LOADING_PANE_SIZE = 100;
    private static final int TRANSITION_DURATION = 200;
    private static final double WIDTH_PERCENT = 0.7;
    private static final double HEIGHT_PERCENT = 0.8;
    private static final double INITIAL_DELAY_SECONDS = 0.2;
    private static final double BACKGROUND_BLUR_RADIUS = 4.0;
    private static final int MODAL_FADE_DURATION = 150;
    private static ResourceConfig config;
    @Setter
    private static java.util.function.DoubleSupplier stageWidthSupplier;

    private ScreenUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static void setConfig(ResourceConfig resourceConfig) {
        if (config != null) {
            throw new IllegalStateException("ScreenUtils configuration has already been set.");
        }
        config = resourceConfig;
    }

    private static void ensureConfigured() {
        if (config == null) {
            throw new IllegalStateException("ScreenUtils has not been configured. Please call setConfig() at startup.");
        }
    }

    public static <T> FxmlResult<T> loadFxml(String fxmlFile) {
        ensureConfigured();
        try {
            String fullFxmlPath = config.fxmlBasePath() + fxmlFile;
            FXMLLoader loader = new FXMLLoader(ScreenUtils.class.getResource(fullFxmlPath));
            Parent root = loader.load();
            T controller = loader.getController();
            return new FxmlResult<>(root, controller);
        } catch (IOException e) {
            throw new FxmlLoadException("Failed to load FXML: " + fxmlFile, e);
        }
    }

    public static Scene createScene(Parent root) {
        ensureConfigured();
        Scene scene = new Scene(root);

        var css = ScreenUtils.class.getResource(config.cssPath());
        if (css == null) {
            throw new IllegalStateException("CSS file not found: " + config.cssPath());
        }
        scene.getStylesheets().add(css.toExternalForm());

        if (stageWidthSupplier != null) {
            double fontSize = Math.max(10, stageWidthSupplier.getAsDouble() * 0.007);
            root.setStyle("-fx-font-size: " + fontSize + "px;");
        }

        return scene;
    }

    public static <T> void showAsScreen(Stage stage, String fxmlFile, String title, Consumer<T> controllerAction) {
        FxmlResult<T> result = loadFxml(fxmlFile);
        if (controllerAction != null) {
            controllerAction.accept(result.controller());
        }
        Scene scene = createScene(result.root());
        stage.setScene(scene);
        stage.setTitle(title);
    }

    public static <T> void showAsModal(
            Window owner,
            String fxmlFile,
            String title,
            Consumer<T> controllerAction) {

        Runnable showModalTask = () -> {
            Stage loadingStage = createLoadingStage(owner);
            PauseTransition delay = new PauseTransition(Duration.millis(TRANSITION_DURATION));
            delay.setOnFinished(_ -> loadingStage.show());

            Task<FxmlResult<T>> loadTask = new Task<>() {
                @Override
                protected FxmlResult<T> call() {
                    return loadFxml(fxmlFile);
                }
            };

            loadTask.setOnSucceeded(_ -> {
                delay.stop();
                loadingStage.close();

                FxmlResult<T> result = loadTask.getValue();
                if (controllerAction != null) {
                    controllerAction.accept(result.controller());
                }

                Stage stage = new Stage();
                stage.setTitle(title);
                stage.initModality(Modality.APPLICATION_MODAL);
                stage.initOwner(owner);
                stage.setResizable(true);
                stage.setOnCloseRequest(Event::consume);

                Scene scene = createScene(result.root());
                stage.setScene(scene);
                stage.setWidth(owner.getWidth() * WIDTH_PERCENT);
                stage.setHeight(owner.getHeight() * HEIGHT_PERCENT);

                double centerX = owner.getX() + (owner.getWidth() - stage.getWidth()) / 2;
                double centerY = owner.getY() + (owner.getHeight() - stage.getHeight()) / 2;
                stage.setX(centerX);
                stage.setY(centerY);

                applyBackgroundBlur(owner, stage);
                applyFadeAnimation(result.root(), stage);

                stage.showAndWait();
            });

            loadTask.setOnFailed(_ -> {
                delay.stop();
                loadingStage.close();
                log.error("Failed to load FXML file asynchronously: {}", fxmlFile, loadTask.getException());
                Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, "Could not load screen: " + fxmlFile).showAndWait());
            });

            delay.play();
            new Thread(loadTask).start();
        };

        PauseTransition initialDelay = new PauseTransition(Duration.seconds(INITIAL_DELAY_SECONDS));
        initialDelay.setOnFinished(_ -> showModalTask.run());
        initialDelay.play();
    }

    private static void applyBackgroundBlur(Window owner, Stage modalStage) {
        if (owner instanceof Stage ownerStage) {
            Scene ownerScene = ownerStage.getScene();
            if (ownerScene != null) {
                Node ownerRoot = ownerScene.getRoot();
                GaussianBlur blur = new GaussianBlur(BACKGROUND_BLUR_RADIUS);
                ownerRoot.setEffect(blur);
                modalStage.setOnHidden(_ -> ownerRoot.setEffect(null));
            }
        }
    }

    private static void applyFadeAnimation(Parent root, Stage stage) {
        root.setOpacity(0);
        FadeTransition fade = new FadeTransition(Duration.millis(MODAL_FADE_DURATION), root);
        fade.setFromValue(0);
        fade.setToValue(1.0);
        fade.setInterpolator(Interpolator.EASE_OUT);
        stage.setOnShown(_ -> fade.play());
    }

    private static Stage createLoadingStage(Window owner) {
        ProgressIndicator progressIndicator = new ProgressIndicator();
        VBox loadingPane = new VBox(progressIndicator);
        loadingPane.setAlignment(Pos.CENTER);
        loadingPane.getStyleClass().add("loading-pane");

        Scene loadingScene = new Scene(loadingPane, LOADING_PANE_SIZE, LOADING_PANE_SIZE);
        loadingScene.setFill(null);

        Stage stage = new Stage();
        stage.setScene(loadingScene);
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setResizable(false);
        stage.setTitle("Loading...");

        return stage;
    }

    public record FxmlResult<T>(Parent root, T controller) {
    }
}