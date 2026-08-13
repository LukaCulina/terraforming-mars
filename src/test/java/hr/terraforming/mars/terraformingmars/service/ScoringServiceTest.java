package hr.terraforming.mars.terraformingmars.service;

import hr.terraforming.mars.terraformingmars.model.Player;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScoringServiceTest {

    @Test
    void higherFinalScore_ranksFirst() {
        Player strongPlayer = new Player("Strong", 1);
        strongPlayer.increaseTR(20);

        Player weakPlayer = new Player("Weak", 2);
        weakPlayer.increaseTR(5);

        List<Player> ranked = ScoringService.calculateFinalScores(List.of(weakPlayer, strongPlayer));

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

        List<Player> ranked = ScoringService.calculateFinalScores(List.of(poorPlayer, richPlayer));

        assertEquals("Rich", ranked.getFirst().getName());
        assertEquals("Poor", ranked.get(1).getName());
    }
}