package hr.terraforming.mars.terraformingmars.manager;

import hr.terraforming.mars.terraformingmars.model.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TurnManagerTest {

    private Player p1;
    private Player p2;
    private Player p3;
    private TurnManager turnManager;

    @BeforeEach
    void setUp() {
        p1 = new Player("Player 1", 1);
        p2 = new Player("Player 2", 2);
        p3 = new Player("Player 3", 3);
        turnManager = new TurnManager(List.of(p1, p2, p3));
    }

    @Test
    void firstPlayer_isFirstInList() {
        assertEquals(p1, turnManager.getCurrentPlayer());
        assertEquals(p1, turnManager.getFirstPlayer());
    }

    @Test
    void passTurn_advancesToNextPlayer() {
        boolean allPassed = turnManager.passTurn();

        assertFalse(allPassed);
        assertEquals(p2, turnManager.getCurrentPlayer());
    }

    @Test
    void passTurn_skipsAlreadyPassedPlayers() {
        turnManager.passTurn();
        turnManager.setCurrentPlayerByName("Player 3");
        turnManager.passTurn();

        assertEquals(p2, turnManager.getCurrentPlayer());
    }

    @Test
    void passTurn_returnsTrueWhenAllPlayersPassed() {
        turnManager.passTurn();
        turnManager.passTurn();
        boolean allPassed = turnManager.passTurn();
        assertTrue(allPassed);
    }

    @Test
    void incrementActionsTaken_tracksCountPerTurn() {
        assertEquals(0, turnManager.getActionsTakenThisTurn());

        turnManager.incrementActionsTaken();
        turnManager.incrementActionsTaken();

        assertEquals(2, turnManager.getActionsTakenThisTurn());
    }

    @Test
    void rotateFirstPlayer_movesToNextInOrder() {
        turnManager.rotateFirstPlayer();
        assertEquals(p2, turnManager.getFirstPlayer());

        turnManager.rotateFirstPlayer();
        assertEquals(p3, turnManager.getFirstPlayer());

        turnManager.rotateFirstPlayer();
        assertEquals(p1, turnManager.getFirstPlayer());
    }

    @Test
    void beginActionPhase_setsCurrentPlayerToFirstPlayer() {
        turnManager.rotateFirstPlayer();
        turnManager.setCurrentPlayerByName("Player 3");

        turnManager.beginActionPhase();

        assertEquals(p2, turnManager.getCurrentPlayer());
        assertEquals(0, turnManager.getActionsTakenThisTurn());
    }

    @Test
    void setCurrentPlayerByName_unknownName_throwsException() {
        assertThrows(hr.terraforming.mars.terraformingmars.exception.GameStateException.class,
                () -> turnManager.setCurrentPlayerByName("NonExistentPlayer"));
    }

    @Test
    void reset_clearsPassedPlayersAndActionCount() {
        turnManager.incrementActionsTaken();
        turnManager.passTurn();

        turnManager.reset();

        assertEquals(0, turnManager.getActionsTakenThisTurn());
        assertEquals(p1, turnManager.getCurrentPlayer());
    }
}