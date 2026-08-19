package hr.terraforming.mars.terraformingmars.service;

import hr.terraforming.mars.terraformingmars.enums.Award;
import hr.terraforming.mars.terraformingmars.model.GameBoard;
import hr.terraforming.mars.terraformingmars.model.Player;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
public class ScoringService {

    private ScoringService() {
        throw new IllegalStateException("Service class - use static methods");
    }

    public static List<Player> calculateFinalScores(List<Player> players, GameBoard board) {
        for (Player player : players) {
            player.calculateTilePoints();
        }

        evaluateAwards(players, board);

        List<Player> rankedPlayers = new ArrayList<>(players);
        rankedPlayers.sort(
                Comparator.comparingInt(Player::getFinalScore).reversed()
                        .thenComparing(Player::getMC, Comparator.reverseOrder())
        );

        for (int i = 0; i < rankedPlayers.size(); i++) {
            Player player = rankedPlayers.get(i);
            log.info("#{} - {} with {} points (MC: {})",
                    i + 1, player.getName(), player.getFinalScore(), player.getMC());
        }

        return rankedPlayers;
    }

    private static void evaluateAwards(List<Player> players, GameBoard board) {
        boolean isTwoPlayerGame = players.size() == 2;

        for (Award award : board.getFundedAwards().keySet()) {
            evaluateSingleAward(award, players, board, isTwoPlayerGame);
        }
    }

    private static void evaluateSingleAward(Award award, List<Player> players, GameBoard board, boolean isTwoPlayerGame) {
        List<AwardCandidate> candidates = players.stream()
                .map(p -> new AwardCandidate(p, award.evaluateScore(p, board)))
                .sorted(Comparator.comparingInt(AwardCandidate::score).reversed())
                .toList();

        int maxScore = candidates.getFirst().score();
        if (maxScore == 0) {
            return;
        }

        List<AwardCandidate> firstPlace = candidates.stream()
                .filter(c -> c.score() == maxScore)
                .toList();

        firstPlace.forEach(c -> c.player().addAwardPoints(5));

        if (firstPlace.size() != 1 || isTwoPlayerGame) {
            return;
        }

        int secondPlaceScore = candidates.get(1).score();
        if (secondPlaceScore > 0) {
            candidates.stream()
                    .filter(c -> c.score() == secondPlaceScore)
                    .forEach(c -> c.player().addAwardPoints(2));
        }
    }

    private record AwardCandidate(Player player, int score) {
    }
}