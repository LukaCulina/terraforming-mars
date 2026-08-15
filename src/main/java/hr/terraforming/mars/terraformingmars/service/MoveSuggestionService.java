package hr.terraforming.mars.terraformingmars.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import hr.terraforming.mars.terraformingmars.config.ConfigurationKey;
import hr.terraforming.mars.terraformingmars.config.ConfigurationReader;
import hr.terraforming.mars.terraformingmars.model.GameBoard;
import hr.terraforming.mars.terraformingmars.model.GameManager;
import hr.terraforming.mars.terraformingmars.model.Player;
import hr.terraforming.mars.terraformingmars.util.PromptUtils;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class MoveSuggestionService {

    private final String apiUrl;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final Gson gson = new Gson();
    @Setter
    private String apiKey;

    public MoveSuggestionService() {
        this.apiKey = loadApiKey();
        String model = ConfigurationReader.getStringValue(ConfigurationKey.GEMINI_MODEL);
        this.apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent";
    }

    public String loadApiKey() {
        try (java.io.InputStream is = getClass().getResourceAsStream("/secrets.properties")) {
            if (is != null) {
                java.util.Properties props = new java.util.Properties();
                props.load(is);
                String key = props.getProperty("gemini.api.key");
                if (key != null && !key.isBlank()) {
                    return key;
                }
            }
        } catch (Exception _) {
            log.debug("The secrets.properties file was not found or is unreadable. Falling back to environment variables.");
        }

        return System.getenv("GEMINI_API_KEY");
    }

    public boolean hasValidApiKey() {
        return this.apiKey != null && !this.apiKey.isBlank();
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
                .POST(HttpRequest.BodyPublishers.ofString(buildRequestJson(PromptUtils.generatePrompt(player, gameManager, board))))
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