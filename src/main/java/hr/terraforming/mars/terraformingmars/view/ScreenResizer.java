package hr.terraforming.mars.terraformingmars.view;

import javafx.scene.layout.AnchorPane;

public class ScreenResizer {

    private ScreenResizer() {
        throw new IllegalStateException("Utility class");
    }

    public static void attachResizeListeners(AnchorPane pane, HexBoardDrawer drawer) {
        pane.widthProperty().addListener((_, _, _) -> drawer.drawBoard());
        pane.heightProperty().addListener((_, _, _) -> drawer.drawBoard());
    }

    public record FontMapping(String styleClass, double scaleFactor) {
    }
}