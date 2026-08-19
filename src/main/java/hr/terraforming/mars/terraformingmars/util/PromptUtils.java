package hr.terraforming.mars.terraformingmars.util;

import hr.terraforming.mars.terraformingmars.enums.*;
import hr.terraforming.mars.terraformingmars.model.Card;
import hr.terraforming.mars.terraformingmars.model.GameBoard;
import hr.terraforming.mars.terraformingmars.model.GameManager;
import hr.terraforming.mars.terraformingmars.model.Player;
import hr.terraforming.mars.terraformingmars.service.CostService;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PromptUtils {

    private PromptUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static String generatePrompt(Player player, GameManager gameManager, GameBoard board) {
        StringBuilder sb = new StringBuilder("You are an expert strategist for Terraforming Mars. Analyze the game state and recommend ONE best next move in English.\n\n");

        appendPlayerOverview(sb, player, gameManager);
        appendCardsOverview(sb, player);
        appendBoardOverview(sb, board, player);
        appendMilestonesAndProjects(sb, board, player);
        appendPlantConversion(sb, player);

        return sb.append("Briefly explain (in 2 to 3 sentences) why this move is mathematically or strategically optimal.").toString();
    }

    private static void appendPlayerOverview(StringBuilder sb, Player player, GameManager gameManager) {
        String corp = player.getCorporation() != null ? player.getCorporation().name() + " — " + player.getCorporation().abilityDescription() : "None";
        int rem = 2 - gameManager.getActionsTakenThisTurn();

        sb.append("=== CORPORATION ===\n").append(corp).append("\n\n")
                .append("=== ACTIONS REMAINING ===\nRemaining: ").append(rem).append(" / 2\n")
                .append(rem >= 2 ? "Player can execute BOTH actions this turn.\n\n" : "\n")
                .append("=== RESOURCES & PRODUCTION ===\nMC: ").append(player.getMC()).append(" (+").append(player.getProduction(ResourceType.MEGA_CREDITS)).append("/gen)\n");

        for (ResourceType type : ResourceType.values()) {
            if (type != ResourceType.MEGA_CREDITS) {
                sb.append(type.name()).append(": ").append(player.resourceProperty(type).get())
                        .append(" (+").append(player.getProduction(type)).append("/gen)\n");
            }
        }
        sb.append("TR: ").append(player.getTR()).append("\n\n");
    }

    private static void appendCardsOverview(StringBuilder sb, Player player) {
        String played = player.getPlayed().isEmpty() ? "None" : player.getPlayed().stream().map(Card::getName).collect(Collectors.joining(", "));
        String tags = Arrays.stream(TagType.values()).map(t -> t.name() + "=" + player.countTags(t)).collect(Collectors.joining(", "));

        sb.append("=== PLAYED CARDS & TAGS ===\nPlayed: ").append(played).append("\nTags: ").append(tags).append("\n\n=== HAND ===\n");

        for (Card card : player.getHand()) {
            String cTags = card.getTags().stream().map(Enum::name).collect(Collectors.joining("/"));
            String status = player.canPlayCard(card) ? "" : " [UNPLAYABLE - requirements/MC]";
            sb.append("- ").append(card.getName()).append(" (").append(player.getEffectiveCardCost(card))
                    .append(" MC, tags: ").append(cTags).append("): ").append(card.getDescription()).append(status).append("\n");
        }
        sb.append("\n");
    }

    private static void appendBoardOverview(StringBuilder sb, GameBoard board, Player player) {
        sb.append("=== BOARD STATE ===\nOxygen: ").append(board.getOxygenLevel()).append("% / ").append(GameBoard.MAX_OXYGEN)
                .append("% | Temp: ").append(board.getTemperature()).append("°C / ").append(GameBoard.MAX_TEMPERATURE)
                .append("°C | Oceans: ").append(board.getOceansPlaced()).append(" / ").append(GameBoard.MAX_OCEANS)
                .append("\nFinal Generation: ").append(board.isFinalGeneration()).append("\n\n=== TILE PLACEMENT SPOTS ===\n");

        for (TileType type : List.of(TileType.OCEAN, TileType.GREENERY, TileType.CITY)) {
            long count = board.getTiles().stream().filter(tile -> board.isValidPlacement(type, tile, player)).count();
            sb.append(type.name()).append(": ").append(count).append(" valid spots\n");
        }
        sb.append("\n");
    }

    private static void appendMilestonesAndProjects(StringBuilder sb, GameBoard board, Player player) {
        sb.append("=== MILESTONES ===\n");
        Map<Milestone, Player> claimed = board.getClaimedMilestones();
        for (Milestone m : Milestone.values()) {
            if (claimed.containsKey(m)) {
                sb.append("- ").append(m.getName()).append(": claimed by ").append(claimed.get(m).getName()).append("\n");
            } else {
                boolean eligible = m.canClaim(player) && player.getMC() >= 8 && claimed.size() < GameBoard.MAX_MILESTONES;
                sb.append("- ").append(m.getName()).append(" (8 MC): ").append(eligible ? "CLAIMABLE" : "unavailable").append("\n");
            }
        }

        sb.append("\n=== STANDARD PROJECTS ===\n");
        for (StandardProject proj : StandardProject.values()) {
            int cost = CostService.getFinalProjectCost(proj, player);
            boolean ok = player.getMC() >= cost && (proj != StandardProject.AQUIFER || board.canPlaceOcean()) && (proj != StandardProject.SELL_PATENTS || !player.getHand().isEmpty());
            sb.append("- ").append(proj.getName()).append(" (").append(cost).append(" MC): ").append(ok ? "available" : "unavailable").append("\n");
        }
        sb.append("\n");
    }

    private static void appendPlantConversion(StringBuilder sb, Player player) {
        int plants = player.resourceProperty(ResourceType.PLANTS).get();
        int plantCost = player.getGreeneryCost();
        String status = (plants >= plantCost) ? " -> READY for greenery conversion\n\n" : " -> insufficient plants\n\n";
        sb.append("=== PLANT CONVERSION ===\nPlants: ").append(plants).append(" / ").append(plantCost).append(status);
    }
}
