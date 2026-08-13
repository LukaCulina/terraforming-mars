package hr.terraforming.mars.terraformingmars.model;

import hr.terraforming.mars.terraformingmars.enums.Milestone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameBoardTest {

    private GameBoard board;

    @BeforeEach
    void setUp() {
        board = new GameBoard();
    }

    @Test
    void temperature_increasesInStepsOfTwoDegrees() {
        int before = board.getTemperature();

        board.canIncreaseTemperature();

        assertEquals(before + 2, board.getTemperature());
    }

    @Test
    void temperature_neverExceedsMax() {
        board.setTemperature(GameBoard.MAX_TEMPERATURE);

        boolean increased = board.canIncreaseTemperature();

        assertFalse(increased);
        assertEquals(GameBoard.MAX_TEMPERATURE, board.getTemperature());
    }

    @Test
    void oxygen_neverExceedsMax() {
        board.setOxygenLevel(GameBoard.MAX_OXYGEN);

        boolean increased = board.canIncreaseOxygen();

        assertFalse(increased);
        assertEquals(GameBoard.MAX_OXYGEN, board.getOxygenLevel());
    }

    @Test
    void finalGeneration_triggersWhenAllParametersMaxed() {
        assertFalse(board.isFinalGeneration());

        board.setOxygenLevel(GameBoard.MAX_OXYGEN);
        board.setTemperature(GameBoard.MAX_TEMPERATURE);
        board.setOceansPlaced(GameBoard.MAX_OCEANS);

        assertTrue(board.isFinalGeneration());
    }

    @Test
    void finalGeneration_notTriggeredIfOnlyOneParameterMaxed() {
        board.setOxygenLevel(GameBoard.MAX_OXYGEN);

        assertFalse(board.isFinalGeneration());
    }

    @Test
    void canClaimMilestone_succeedsWhenRequirementMet() {
        Player player = new Player("Player 1", 1);
        player.increaseTR(15);

        boolean claimed = board.canClaimMilestone(Milestone.TERRAFORMER, player);

        assertTrue(claimed);
        assertTrue(board.getClaimedMilestones().containsKey(Milestone.TERRAFORMER));
    }

    @Test
    void canClaimMilestone_failsWhenRequirementNotMet() {
        Player player = new Player("Player 1", 1);

        boolean claimed = board.canClaimMilestone(Milestone.TERRAFORMER, player);

        assertFalse(claimed);
    }

    @Test
    void canClaimMilestone_cannotBeClaimedTwice() {
        Player firstPlayer = new Player("Player 1", 1);
        firstPlayer.increaseTR(15);
        Player secondPlayer = new Player("Player 2", 2);
        secondPlayer.increaseTR(15);

        board.canClaimMilestone(Milestone.TERRAFORMER, firstPlayer);
        boolean secondClaim = board.canClaimMilestone(Milestone.TERRAFORMER, secondPlayer);

        assertFalse(secondClaim);
    }
}