package hr.terraforming.mars.terraformingmars.effect;

import hr.terraforming.mars.terraformingmars.config.ResourceConfig;
import hr.terraforming.mars.terraformingmars.enums.ResourceType;
import hr.terraforming.mars.terraformingmars.enums.TagType;
import hr.terraforming.mars.terraformingmars.factory.CardFactory;
import hr.terraforming.mars.terraformingmars.model.Card;
import hr.terraforming.mars.terraformingmars.model.GameBoard;
import hr.terraforming.mars.terraformingmars.model.GameManager;
import hr.terraforming.mars.terraformingmars.model.Player;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EffectInterpreterTest {

    private Player player;
    private GameManager gameManager;
    private GameBoard board;

    @BeforeAll
    static void loadCards() {
        CardFactory.setConfig(new ResourceConfig(
                "/hr/terraforming/mars/terraformingmars/fxml/",
                "/hr/terraforming/mars/terraformingmars/css/styles.css",
                "/hr/terraforming/mars/terraformingmars/data/cards.json"
        ));
        CardFactory.loadAllCards();
    }

    @BeforeEach
    void setUp() {
        player = new Player("Player 1", 1);
        board = new GameBoard();
        gameManager = new GameManager(List.of(player), board);
    }

    @Test
    void addResource_megaCredits_addsToMc() {
        List<Effect> effects = EffectInterpreter.parseEffects(List.of(
                Map.of("type", "addResource", "resource", "MEGA_CREDITS", "amount", 5.0)
        ));

        effects.getFirst().execute(player, gameManager);
        assertEquals(5, player.getMC());
    }

    @Test
    void addResource_nonMc_addsToResourceProperty() {
        List<Effect> effects = EffectInterpreter.parseEffects(List.of(
                Map.of("type", "addResource", "resource", "STEEL", "amount", 3.0)
        ));

        effects.getFirst().execute(player, gameManager);
        assertEquals(3, player.resourceProperty(ResourceType.STEEL).get());
    }

    @Test
    void increaseProduction_addsToProductionProperty() {
        List<Effect> effects = EffectInterpreter.parseEffects(List.of(
                Map.of("type", "increaseProduction", "resource", "ENERGY", "amount", 2.0)
        ));

        effects.getFirst().execute(player, gameManager);
        assertEquals(2, player.getProduction(ResourceType.ENERGY));
    }

    @Test
    void increaseProductionPerTag_scalesWithTagCount() {
        Card buildingCard = new Card.Builder("B1", 1).tags(TagType.BUILDING).build();
        player.getPlayed().add(buildingCard);
        player.getPlayed().add(buildingCard);

        List<Effect> effects = EffectInterpreter.parseEffects(List.of(
                Map.of("type", "increaseProductionPerTag", "resource", "MEGA_CREDITS", "tag", "BUILDING")
        ));

        effects.getFirst().execute(player, gameManager);
        assertEquals(2, player.getProduction(ResourceType.MEGA_CREDITS));
    }

    @Test
    void gainMcPerTag_appliesMultiplier() {
        Card scienceCard = new Card.Builder("S1", 1).tags(TagType.SCIENCE).build();
        player.getPlayed().add(scienceCard);
        player.getPlayed().add(scienceCard);

        List<Effect> effects = EffectInterpreter.parseEffects(List.of(
                Map.of("type", "gainMcPerTag", "tag", "SCIENCE", "multiplier", 3.0)
        ));

        effects.getFirst().execute(player, gameManager);
        assertEquals(6, player.getMC());
    }

    @Test
    void gainMcPerTag_includeSelf_countsExtraOne() {
        List<Effect> effects = EffectInterpreter.parseEffects(List.of(
                Map.of("type", "gainMcPerTag", "tag", "EARTH", "multiplier", 2.0, "includeSelf", true)
        ));

        effects.getFirst().execute(player, gameManager);
        assertEquals(2, player.getMC());
    }

    @Test
    void gainMcPerOpponent_scalesWithPlayerCount() {
        Player opponent1 = new Player("Player 2", 2);
        Player opponent2 = new Player("Player 3", 3);
        GameManager threePlayerGame = new GameManager(List.of(player, opponent1, opponent2), board);

        List<Effect> effects = EffectInterpreter.parseEffects(List.of(
                Map.of("type", "gainMcPerOpponent", "amount", 2.0)
        ));

        effects.getFirst().execute(player, threePlayerGame);
        assertEquals(4, player.getMC());
    }

    @Test
    void drawCards_addsToHand() {
        List<Effect> effects = EffectInterpreter.parseEffects(List.of(
                Map.of("type", "drawCards", "amount", 2.0)
        ));

        int handSizeBefore = player.getHand().size();
        effects.getFirst().execute(player, gameManager);

        assertEquals(handSizeBefore + 2, player.getHand().size());
    }

    @Test
    void increaseTemperature_raisesBoardTemperature() {
        int before = board.getTemperature();

        List<Effect> effects = EffectInterpreter.parseEffects(List.of(
                Map.of("type", "increaseTemperature", "amount", 1.0)
        ));

        effects.getFirst().execute(player, gameManager);
        assertEquals(before + 2, board.getTemperature());
    }

    @Test
    void increaseTR_raisesPlayerTr() {
        int before = player.getTR();

        List<Effect> effects = EffectInterpreter.parseEffects(List.of(
                Map.of("type", "increaseTR", "amount", 3.0)
        ));

        effects.getFirst().execute(player, gameManager);
        assertEquals(before + 3, player.getTR());
    }

    @Test
    void nullEffectList_returnsEmptyList() {
        assertTrue(EffectInterpreter.parseEffects(null).isEmpty());
    }

    @Test
    void unknownEffectType_throwsException() {
        List<Map<String, Object>> data = List.of(Map.of("type", "unknownEffect"));
        assertThrows(IllegalArgumentException.class, () -> EffectInterpreter.parseEffects(data));
    }
}