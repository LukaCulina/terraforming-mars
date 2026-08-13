package hr.terraforming.mars.terraformingmars.model;

import hr.terraforming.mars.terraformingmars.enums.TagType;
import hr.terraforming.mars.terraformingmars.factory.CorporationFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerStateTest {

    private final PlayerState state = new PlayerState();

    @Test
    void credicor_discountsEveryCardByOne() {
        Corporation credicor = CorporationFactory.getCorporationByName("Credicor");
        Card card = new Card.Builder("Test Card", 10).build();

        assertEquals(9, state.getCardCost(card, credicor));
    }

    @Test
    void miningGuild_discountsBuildingTaggedCards() {
        Corporation miningGuild = CorporationFactory.getCorporationByName("Mining Guild");
        Card buildingCard = new Card.Builder("Building Card", 10).tags(TagType.BUILDING).build();
        Card plainCard = new Card.Builder("Plain Card", 10).build();

        assertEquals(8, state.getCardCost(buildingCard, miningGuild));
        assertEquals(10, state.getCardCost(plainCard, miningGuild));
    }

    @Test
    void phobolog_discountsSpaceTaggedCards() {
        Corporation phobolog = CorporationFactory.getCorporationByName("Phobolog");
        Card spaceCard = new Card.Builder("Space Card", 10).tags(TagType.SPACE).build();

        assertEquals(6, state.getCardCost(spaceCard, phobolog));
    }

    @Test
    void teractor_discountsEarthTaggedCards() {
        Corporation teractor = CorporationFactory.getCorporationByName("Teractor");
        Card earthCard = new Card.Builder("Earth Card", 10).tags(TagType.EARTH).build();

        assertEquals(7, state.getCardCost(earthCard, teractor));
    }

    @Test
    void inventrix_discountsScienceTaggedCards() {
        Corporation inventrix = CorporationFactory.getCorporationByName("Inventrix");
        Card scienceCard = new Card.Builder("Science Card", 10).tags(TagType.SCIENCE).build();

        assertEquals(8, state.getCardCost(scienceCard, inventrix));
    }

    @Test
    void thorgate_discountsEnergyTaggedCards() {
        Corporation thorgate = CorporationFactory.getCorporationByName("Thorgate");
        Card energyCard = new Card.Builder("Energy Card", 10).tags(TagType.ENERGY).build();

        assertEquals(7, state.getCardCost(energyCard, thorgate));
    }

    @Test
    void discount_neverDropsCostBelowZero() {
        Corporation phobolog = CorporationFactory.getCorporationByName("Phobolog");
        Card cheapSpaceCard = new Card.Builder("Cheap Space Card", 2).tags(TagType.SPACE).build();

        assertEquals(0, state.getCardCost(cheapSpaceCard, phobolog));
    }

    @Test
    void nullCorporation_returnsRawCost() {
        Card card = new Card.Builder("Test Card", 10).build();

        assertEquals(10, state.getCardCost(card, null));
    }
}