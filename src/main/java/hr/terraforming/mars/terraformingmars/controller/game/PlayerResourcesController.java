package hr.terraforming.mars.terraformingmars.controller.game;

import hr.terraforming.mars.terraformingmars.enums.ResourceType;
import hr.terraforming.mars.terraformingmars.model.Player;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class PlayerResourcesController {

    @FXML
    private Label mcLabel;
    @FXML
    private Label steelLabel;
    @FXML
    private Label titaniumLabel;
    @FXML
    private Label plantsLabel;
    @FXML
    private Label energyLabel;
    @FXML
    private Label heatLabel;
    @FXML
    private Label mcProductionLabel;
    @FXML
    private Label steelProductionLabel;
    @FXML
    private Label titaniumProductionLabel;
    @FXML
    private Label plantsProductionLabel;
    @FXML
    private Label energyProductionLabel;
    @FXML
    private Label heatProductionLabel;

    public void updateResources(Player player) {
        if (player == null) return;

        mcLabel.textProperty().bind(player.mcProperty().asString());
        steelLabel.textProperty().bind(player.resourceProperty(ResourceType.STEEL).asString());
        titaniumLabel.textProperty().bind(player.resourceProperty(ResourceType.TITANIUM).asString());
        plantsLabel.textProperty().bind(player.resourceProperty(ResourceType.PLANTS).asString());
        energyLabel.textProperty().bind(player.resourceProperty(ResourceType.ENERGY).asString());
        heatLabel.textProperty().bind(player.resourceProperty(ResourceType.HEAT).asString());

        mcProductionLabel.textProperty().bind(player.productionProperty(ResourceType.MEGA_CREDITS).asString());
        steelProductionLabel.textProperty().bind(player.productionProperty(ResourceType.STEEL).asString());
        titaniumProductionLabel.textProperty().bind(player.productionProperty(ResourceType.TITANIUM).asString());
        plantsProductionLabel.textProperty().bind(player.productionProperty(ResourceType.PLANTS).asString());
        energyProductionLabel.textProperty().bind(player.productionProperty(ResourceType.ENERGY).asString());
        heatProductionLabel.textProperty().bind(player.productionProperty(ResourceType.HEAT).asString());
    }
}
