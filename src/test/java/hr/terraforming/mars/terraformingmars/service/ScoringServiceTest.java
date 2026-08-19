package hr.terraforming.mars.terraformingmars.service;

import hr.terraforming.mars.terraformingmars.enums.Award;
import hr.terraforming.mars.terraformingmars.enums.ResourceType;
import hr.terraforming.mars.terraformingmars.model.GameBoard;
import hr.terraforming.mars.terraformingmars.model.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScoringServiceTest {

    private GameBoard board;

    @BeforeEach
    void setUp() {
        board = new GameBoard();
    }

    @Test
    void higherFinalScore_ranksFirst() {
        Player strongPlayer = new Player("Strong", 1);
        strongPlayer.increaseTR(20);

        Player weakPlayer = new Player("Weak", 2);
        weakPlayer.increaseTR(5);

        List<Player> ranked = ScoringService.calculateFinalScores(List.of(weakPlayer, strongPlayer), board);

        assertEquals("Strong", ranked.getFirst().getName());
        assertEquals("Weak", ranked.get(1).getName());
    }

    @Test
    void tiedFinalScore_higherMcRanksFirst() {
        Player richPlayer = new Player("Rich", 1);
        richPlayer.increaseTR(5);
        richPlayer.addMC(50);

        Player poorPlayer = new Player("Poor", 2);
        poorPlayer.increaseTR(5);
        poorPlayer.addMC(10);

        List<Player> ranked = ScoringService.calculateFinalScores(List.of(poorPlayer, richPlayer), board);

        assertEquals("Rich", ranked.getFirst().getName());
        assertEquals("Poor", ranked.get(1).getName());
    }

    @Test
    void evaluateAwards_friendlyTie_awardsPointsCorrectly() {
        Player p1 = new Player("Player 1", 1);
        Player p2 = new Player("Player 2", 2);
        Player p3 = new Player("Player 3", 3);

        p1.increaseProduction(ResourceType.MEGA_CREDITS, 10);
        p2.increaseProduction(ResourceType.MEGA_CREDITS, 10);
        p3.increaseProduction(ResourceType.MEGA_CREDITS, 5);

        board.canFundAward(Award.BANKER, p3);

        ScoringService.calculateFinalScores(List.of(p1, p2, p3), board);

        assertEquals(5, p1.getAwardPoints(), "Player 1 should get 5 points for tieing 1st place.");
        assertEquals(5, p2.getAwardPoints(), "Player 2 should get 5 points for tieing 1st place.");

        assertEquals(0, p3.getAwardPoints(), "Player 3 should get 0 points because 2nd place is skipped.");
    }
}