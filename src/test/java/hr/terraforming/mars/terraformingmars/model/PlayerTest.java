package hr.terraforming.mars.terraformingmars.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlayerTest {

    private Player player;

    @BeforeEach
    void setUp() {
        player = new Player("Broke Player", 1);
        GameBoard board = new GameBoard();
        player.setBoard(board);
    }

    @Test
    void canPlayCard_insufficientMC_returnsFalse() {
        player.mcProperty().set(5);

        Card expensiveCard = new Card.Builder("Expensive Project", 20).build();
        player.getHand().add(expensiveCard);

        assertFalse(player.canPlayCard(expensiveCard),
                "Player should not be able to play a card they cannot afford.");
    }

    @Test
    void canSpendMC_deductsCorrectAmount_whenSufficientFunds() {
        player.mcProperty().set(10);

        boolean success = player.canSpendMC(8);

        assertTrue(success, "Transaction should be successful.");
        assertEquals(2, player.getMC(), "Player should have exactly 2 MC left after spending 8.");
    }
}