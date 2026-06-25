package hr.terraforming.mars.terraformingmars.factory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import hr.terraforming.mars.terraformingmars.config.ResourceConfig;
import hr.terraforming.mars.terraformingmars.effect.Effect;
import hr.terraforming.mars.terraformingmars.effect.EffectInterpreter;
import hr.terraforming.mars.terraformingmars.enums.ResourceType;
import hr.terraforming.mars.terraformingmars.enums.TagType;
import hr.terraforming.mars.terraformingmars.enums.TileType;
import hr.terraforming.mars.terraformingmars.model.Card;
import hr.terraforming.mars.terraformingmars.model.CardData;
import hr.terraforming.mars.terraformingmars.model.GameBoard;
import hr.terraforming.mars.terraformingmars.model.Player;
import lombok.Setter;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.BiPredicate;

public class CardFactory {

    private static final Map<String, Card> cardRegistry = new HashMap<>();
    private static final String AMOUNT = "amount";
    @Setter
    private static ResourceConfig config;

    private CardFactory() {
    }

    public static void loadAllCards() {
        if (!cardRegistry.isEmpty()) {
            return;
        }

        String path = config.cardsPath();

        try (InputStream stream = CardFactory.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("Cannot find cards.json at path: " + path);
            }

            InputStreamReader reader = new InputStreamReader(stream);
            Gson gson = new Gson();
            Type cardListType = new TypeToken<ArrayList<CardData>>() {
            }.getType();
            List<CardData> allCardData = gson.fromJson(reader, cardListType);

            for (CardData data : allCardData) {
                List<Effect> effects = EffectInterpreter.parseEffects(data.effects());

                BiPredicate<Player, GameBoard> requirement = parseRequirement(data.requirements());

                List<TagType> tagEnums = convertStringsToTagTypes(data.tags());

                TileType tileToPlace = null;
                if (data.tileToPlace() != null && !data.tileToPlace().isEmpty()) {
                    tileToPlace = TileType.valueOf(data.tileToPlace());
                }

                Card card = new Card.Builder(data.name(), data.cost())
                        .description(data.description())
                        .tags(tagEnums.toArray(new TagType[0]))
                        .victoryPoints(data.victoryPoints())
                        .effects(effects)
                        .requirement(requirement)
                        .tileToPlace(tileToPlace)
                        .build();

                cardRegistry.put(card.getName(), card);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Error loading or parsing cards.json", e);
        }
    }

    public static List<Card> getAllCards() {
        return new ArrayList<>(cardRegistry.values());
    }

    public static Card getCardByName(String name) {
        return cardRegistry.get(name);
    }

    static BiPredicate<Player, GameBoard> parseRequirement(List<Map<String, Object>> reqDataList) {
        if (reqDataList == null || reqDataList.isEmpty()) {
            return (_, _) -> true;
        }

        BiPredicate<Player, GameBoard> finalRequirement = (_, _) -> true;

        for (Map<String, Object> data : reqDataList) {
            String type = (String) data.get("type");

            BiPredicate<Player, GameBoard> currentReq = switch (type) {
                case "minProduction" -> {
                    ResourceType resource = ResourceType.valueOf((String) data.get("resource"));
                    int amount = ((Double) data.get(AMOUNT)).intValue();
                    yield (p, _) -> p.getProduction(resource) >= amount;
                }

                case "minTags" -> {
                    TagType tag = TagType.valueOf((String) data.get("tag"));
                    int amount = ((Double) data.get(AMOUNT)).intValue();
                    yield (p, _) -> p.countTags(tag) >= amount;
                }

                case "minOceans" -> {
                    int amount = ((Double) data.get(AMOUNT)).intValue();
                    yield (_, gb) -> gb.getOceansPlaced() >= amount;
                }
                case "minOxygen" -> {
                    int amount = ((Double) data.get(AMOUNT)).intValue();
                    yield (_, gb) -> gb.getOxygenLevel() >= amount;
                }
                case "maxOxygen" -> {
                    int amount = ((Double) data.get(AMOUNT)).intValue();
                    yield (_, gb) -> gb.getOxygenLevel() <= amount;
                }
                case "maxTemperature" -> {
                    int amount = ((Double) data.get(AMOUNT)).intValue();
                    yield (_, gb) -> gb.getTemperature() <= amount;
                }

                default -> throw new IllegalArgumentException("Unknown requirement type: " + type);
            };

            finalRequirement = finalRequirement.and(currentReq);
        }

        return finalRequirement;
    }

    private static List<TagType> convertStringsToTagTypes(List<String> tagsAsStrings) {
        if (tagsAsStrings == null) {
            return Collections.emptyList();
        }
        return tagsAsStrings.stream()
                .map(String::toUpperCase)
                .map(TagType::valueOf)
                .toList();
    }
}
