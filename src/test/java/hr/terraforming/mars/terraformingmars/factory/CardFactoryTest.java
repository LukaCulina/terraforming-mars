package hr.terraforming.mars.terraformingmars.factory;

import hr.terraforming.mars.terraformingmars.enums.ResourceType;
import hr.terraforming.mars.terraformingmars.enums.TagType;
import hr.terraforming.mars.terraformingmars.model.Card;
import hr.terraforming.mars.terraformingmars.model.GameBoard;
import hr.terraforming.mars.terraformingmars.model.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;

import static org.junit.jupiter.api.Assertions.*;

class CardFactoryTest {

    private Player player;
    private GameBoard board;

    @BeforeEach
    void setUp() {
        player = new Player("Player 1", 1);
        board = new GameBoard();
    }

    @Test
    void emptyRequirements_alwaysTrue() {
        BiPredicate<Player, GameBoard> req = CardFactory.parseRequirement(null);
        assertTrue(req.test(player, board));

        BiPredicate<Player, GameBoard> reqEmpty = CardFactory.parseRequirement(List.of());
        assertTrue(reqEmpty.test(player, board));
    }

    @Test
    void minProduction_requiresThreshold() {
        List<Map<String, Object>> reqData = List.of(
                Map.of("type", "minProduction", "resource", "ENERGY", "amount", 1.0)
        );
        BiPredicate<Player, GameBoard> req = CardFactory.parseRequirement(reqData);

        assertFalse(req.test(player, board));

        player.increaseProduction(ResourceType.ENERGY, 1);
        assertTrue(req.test(player, board));
    }

    @Test
    void minTags_requiresThreshold() {
        List<Map<String, Object>> reqData = List.of(
                Map.of("type", "minTags", "tag", "SCIENCE", "amount", 2.0)
        );
        BiPredicate<Player, GameBoard> req = CardFactory.parseRequirement(reqData);

        assertFalse(req.test(player, board));

        Card scienceCard1 = new Card.Builder("Sci1", 1).tags(TagType.SCIENCE).build();
        Card scienceCard2 = new Card.Builder("Sci2", 1).tags(TagType.SCIENCE).build();
        player.getPlayed().add(scienceCard1);
        assertFalse(req.test(player, board));

        player.getPlayed().add(scienceCard2);
        assertTrue(req.test(player, board));
    }

    @Test
    void minOceans_checksBoardState() {
        List<Map<String, Object>> reqData = List.of(
                Map.of("type", "minOceans", "amount", 3.0)
        );
        BiPredicate<Player, GameBoard> req = CardFactory.parseRequirement(reqData);

        assertFalse(req.test(player, board));

        board.setOceansPlaced(3);
        assertTrue(req.test(player, board));
    }

    @Test
    void minOxygen_checksBoardState() {
        List<Map<String, Object>> reqData = List.of(
                Map.of("type", "minOxygen", "amount", 5.0)
        );
        BiPredicate<Player, GameBoard> req = CardFactory.parseRequirement(reqData);

        assertFalse(req.test(player, board));

        board.setOxygenLevel(5);
        assertTrue(req.test(player, board));
    }

    @Test
    void maxOxygen_checksBoardState() {
        List<Map<String, Object>> reqData = List.of(
                Map.of("type", "maxOxygen", "amount", 5.0)
        );
        BiPredicate<Player, GameBoard> req = CardFactory.parseRequirement(reqData);

        assertTrue(req.test(player, board));

        board.setOxygenLevel(6);
        assertFalse(req.test(player, board));
    }

    @Test
    void maxTemperature_checksBoardState() {
        List<Map<String, Object>> reqData = List.of(
                Map.of("type", "maxTemperature", "amount", -20.0)
        );
        BiPredicate<Player, GameBoard> req = CardFactory.parseRequirement(reqData);

        assertTrue(req.test(player, board));

        board.setTemperature(-10);
        assertFalse(req.test(player, board));
    }

    @Test
    void multipleRequirements_areChainedWithAnd() {
        List<Map<String, Object>> reqData = List.of(
                Map.of("type", "minOceans", "amount", 2.0),
                Map.of("type", "minOxygen", "amount", 4.0)
        );
        BiPredicate<Player, GameBoard> req = CardFactory.parseRequirement(reqData);

        board.setOceansPlaced(2);
        assertFalse(req.test(player, board));

        board.setOxygenLevel(4);
        assertTrue(req.test(player, board));
    }

    @Test
    void unknownRequirementType_throwsException() {
        List<Map<String, Object>> reqData = List.of(
                Map.of("type", "unknownType", "amount", 1.0)
        );

        assertThrows(IllegalArgumentException.class, () -> CardFactory.parseRequirement(reqData));
    }
}