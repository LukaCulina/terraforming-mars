package hr.terraforming.mars.terraformingmars.service;

import hr.terraforming.mars.terraformingmars.enums.ResourceType;
import hr.terraforming.mars.terraformingmars.model.Player;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProductionServiceTest {

    @Test
    void executeProduction_calculatesIncomeAndConvertsEnergyToHeat() {
        Player player = new Player("Player 1", 1);
        player.increaseTR(5);
        player.increaseProduction(ResourceType.MEGA_CREDITS, 2);
        player.increaseProduction(ResourceType.STEEL, 1);
        player.addResource(ResourceType.ENERGY, 4);

        ProductionService.executeProduction(List.of(player));

        assertEquals(27, player.getMC());
        assertEquals(1, player.resourceProperty(ResourceType.STEEL).get());
        assertEquals(0, player.resourceProperty(ResourceType.ENERGY).get());
        assertEquals(4, player.resourceProperty(ResourceType.HEAT).get());
    }
}