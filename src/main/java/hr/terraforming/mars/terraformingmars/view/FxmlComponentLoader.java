package hr.terraforming.mars.terraformingmars.view;

import hr.terraforming.mars.terraformingmars.exception.FxmlLoadException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class FxmlComponentLoader {

    private FxmlComponentLoader() {
        throw new IllegalStateException("Utility class");
    }

    public static <T> T load(VBox container, String fxmlPath) {
        try {
            String fullPath = "/hr/terraforming/mars/terraformingmars/fxml/" + fxmlPath;
            FXMLLoader loader = new FXMLLoader(FxmlComponentLoader.class.getResource(fullPath));
            
            Node node = loader.load();

            T controller = loader.getController();

            container.getChildren().setAll(node);

            return controller;

        } catch (IOException e) {
            throw new FxmlLoadException("Failed to load " + fxmlPath, e);
        }
    }
}
