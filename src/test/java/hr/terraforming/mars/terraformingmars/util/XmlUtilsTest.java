package hr.terraforming.mars.terraformingmars.util;

import hr.terraforming.mars.terraformingmars.enums.ActionType;
import hr.terraforming.mars.terraformingmars.model.GameMove;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class XmlUtilsTest {

    @BeforeEach
    @AfterEach
    void cleanUp() {
        XmlUtils.clearGameMoves();
    }

    @Test
    void appendAndReadGameMoves_savesAndLoadsCorrectly() {
        GameMove move = new GameMove("TestPlayer", ActionType.PASS_TURN, "", "Passed turn", LocalDateTime.now());

        XmlUtils.appendGameMove(move);
        List<GameMove> moves = XmlUtils.readGameMoves();

        assertFalse(moves.isEmpty());
        assertEquals("TestPlayer", moves.getFirst().playerName());
        assertEquals(ActionType.PASS_TURN, moves.getFirst().actionType());
    }
}