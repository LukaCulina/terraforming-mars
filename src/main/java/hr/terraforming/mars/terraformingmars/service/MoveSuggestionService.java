package hr.terraforming.mars.terraformingmars.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import hr.terraforming.mars.terraformingmars.enums.*;
import hr.terraforming.mars.terraformingmars.jndi.ConfigurationKey;
import hr.terraforming.mars.terraformingmars.jndi.ConfigurationReader;
import hr.terraforming.mars.terraformingmars.model.Card;
import hr.terraforming.mars.terraformingmars.model.GameBoard;
import hr.terraforming.mars.terraformingmars.model.GameManager;
import hr.terraforming.mars.terraformingmars.model.Player;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class MoveSuggestionService {

    private final String apiUrl;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final Gson gson = new Gson();
    private final String apiKey;

    public MoveSuggestionService(String apiKey) {
        this.apiKey = apiKey;
        String model = ConfigurationReader.getStringValue(ConfigurationKey.GEMINI_MODEL);
        this.apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent";
    }

    public CompletableFuture<String> suggestMove(Player player, GameManager gameManager, GameBoard board) {
        return suggestMoveWithRetry(player, gameManager, board, 3, Duration.ofSeconds(2));
    }

    private CompletableFuture<String> suggestMoveWithRetry(Player player, GameManager gameManager, GameBoard board, int retriesLeft, Duration delay) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", apiKey)
                .timeout(Duration.ofSeconds(20))
                .POST(HttpRequest.BodyPublishers.ofString(buildRequestJson(buildPrompt(player, gameManager, board))))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenCompose(response -> {
                    if ((response.statusCode() == 429 || response.statusCode() == 503) && retriesLeft > 0) {
                        return delayedRetry(player, gameManager, board, retriesLeft, delay);
                    }
                    return CompletableFuture.completedFuture(extractSuggestion(response));
                })
                .exceptionallyCompose(ex -> retriesLeft > 0
                        ? delayedRetry(player, gameManager, board, retriesLeft, delay)
                        : CompletableFuture.failedFuture(ex));
    }

    private CompletableFuture<String> delayedRetry(Player player, GameManager gameManager, GameBoard board, int retriesLeft, Duration delay) {
        CompletableFuture<String> result = new CompletableFuture<>();
        CompletableFuture.delayedExecutor(delay.toMillis(), TimeUnit.MILLISECONDS).execute(() ->
                suggestMoveWithRetry(player, gameManager, board, retriesLeft - 1, delay.multipliedBy(2))
                        .whenComplete((res, err) -> {
                            if (err != null) result.completeExceptionally(err);
                            else result.complete(res);
                        })
        );
        return result;
    }

    private String buildPrompt(Player player, GameManager gameManager, GameBoard board) {
        StringBuilder sb = new StringBuilder("You are an expert strategist for Terraforming Mars. Analyze the game state and recommend ONE best next move in English.\n\n");
        appendPlayerOverview(sb, player, gameManager);
        appendCardsOverview(sb, player);
        appendBoardOverview(sb, board, player);
        appendMilestonesAndProjects(sb, board, player);
        appendPlantConversion(sb, player);
        return sb.append("Briefly explain (in 2 to 3 sentences) why this move is mathematically or strategically optimal.").toString();
    }

    private void appendPlayerOverview(StringBuilder sb, Player player, GameManager gameManager) {
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

    private void appendCardsOverview(StringBuilder sb, Player player) {
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

    private void appendBoardOverview(StringBuilder sb, GameBoard board, Player player) {
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

    private void appendMilestonesAndProjects(StringBuilder sb, GameBoard board, Player player) {
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

    private void appendPlantConversion(StringBuilder sb, Player player) {
        int plants = player.resourceProperty(ResourceType.PLANTS).get();
        int plantCost = player.getGreeneryCost();
        String status = (plants >= plantCost) ? " -> READY for greenery conversion\n\n" : " -> insufficient plants\n\n";
        sb.append("=== PLANT CONVERSION ===\nPlants: ").append(plants).append(" / ").append(plantCost).append(status);
    }

    private String buildRequestJson(String prompt) {
        JsonObject textPart = new JsonObject();
        textPart.addProperty("text", prompt);

        JsonArray parts = new JsonArray();
        parts.add(textPart);

        JsonObject content = new JsonObject();
        content.add("parts", parts);

        JsonArray contents = new JsonArray();
        contents.add(content);

        JsonObject body = new JsonObject();
        body.add("contents", contents);
        return gson.toJson(body);
    }

    private String extractSuggestion(HttpResponse<String> response) {
        if (response.statusCode() != 200) {
            return "Error fetching AI suggestion (HTTP " + response.statusCode() + "): " + response.body();
        }

        try {
            JsonObject root = gson.fromJson(response.body(), JsonObject.class);
            return root.getAsJsonArray("candidates")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("content")
                    .getAsJsonArray("parts")
                    .get(0).getAsJsonObject()
                    .get("text").getAsString();
        } catch (Exception e) {
            return "Unable to parse AI response: " + e.getMessage();
        }
    }
}