package hr.terraforming.mars.terraformingmars.service;

import hr.terraforming.mars.terraformingmars.enums.StandardProject;
import hr.terraforming.mars.terraformingmars.factory.CorporationFactory;
import hr.terraforming.mars.terraformingmars.model.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CostServiceTest {

    @Test
    void tharsisRepublic_getsDiscountOnCityProject() {
        Player player = new Player("Player 1", 1);
        player.setCorporation(CorporationFactory.getCorporationByName("Tharsis Republic"));

        int cost = CostService.getFinalProjectCost(StandardProject.CITY, player);

        assertEquals(StandardProject.CITY.getCost() - 4, cost);
    }

    @Test
    void thorgate_getsDiscountOnPowerPlantProject() {
        Player player = new Player("Player 1", 1);
        player.setCorporation(CorporationFactory.getCorporationByName("Thorgate"));

        int cost = CostService.getFinalProjectCost(StandardProject.POWER_PLANT, player);

        assertEquals(StandardProject.POWER_PLANT.getCost() - 3, cost);
    }

    @Test
    void otherCorporation_noDiscountOnCityProject() {
        Player player = new Player("Player 1", 1);
        player.setCorporation(CorporationFactory.getCorporationByName("Credicor"));

        int cost = CostService.getFinalProjectCost(StandardProject.CITY, player);

        assertEquals(StandardProject.CITY.getCost(), cost);
    }

    @Test
    void tharsisRepublic_noDiscountOnUnrelatedProject() {
        Player player = new Player("Player 1", 1);
        player.setCorporation(CorporationFactory.getCorporationByName("Tharsis Republic"));

        int cost = CostService.getFinalProjectCost(StandardProject.ASTEROID, player);

        assertEquals(StandardProject.ASTEROID.getCost(), cost);
    }

    @Test
    void noCorporation_returnsFullCost() {
        Player player = new Player("Player 1", 1);

        int cost = CostService.getFinalProjectCost(StandardProject.CITY, player);

        assertEquals(StandardProject.CITY.getCost(), cost);
    }
}