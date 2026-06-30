package hr.terraforming.mars.terraformingmars.view;

import javafx.animation.PauseTransition;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

public class ScreenResizer {

    private ScreenResizer() {
        throw new IllegalStateException("Utility class");
    }

    public static void attachResizeListeners(AnchorPane pane, HexBoardDrawer drawer) {
        PauseTransition resizePause = new PauseTransition(Duration.millis(100));

        Runnable triggerRedraw = () -> {
            resizePause.stop();
            resizePause.setOnFinished(_ -> drawer.drawBoard());
            resizePause.playFromStart();
        };

        addThresholdListener(pane, triggerRedraw);
    }

    private static void addThresholdListener(Pane pane, Runnable onResize) {
        pane.widthProperty().addListener((_, oldV, newV) -> {
            if (Math.abs(newV.doubleValue() - oldV.doubleValue()) > 10) {
                onResize.run();
            }
        });

        pane.heightProperty().addListener((_, oldV, newV) -> {
            if (Math.abs(newV.doubleValue() - oldV.doubleValue()) > 10) {
                onResize.run();
            }
        });
    }

    public record FontMapping(String styleClass, double scaleFactor) {
    }
}