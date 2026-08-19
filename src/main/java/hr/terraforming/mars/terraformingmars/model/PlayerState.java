package hr.terraforming.mars.terraformingmars.model;

import hr.terraforming.mars.terraformingmars.enums.Milestone;
import hr.terraforming.mars.terraformingmars.enums.ResourceType;
import hr.terraforming.mars.terraformingmars.enums.TagType;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import lombok.Getter;

import java.io.*;
import java.util.*;

public class PlayerState implements Serializable {

    private final Map<ResourceType, Integer> resourceValues = new EnumMap<>(ResourceType.class);
    private final Map<ResourceType, Integer> productionValues = new EnumMap<>(ResourceType.class);
    @Getter
    private final List<Milestone> claimedMilestones = new ArrayList<>();
    @Getter
    private final List<Card> hand = new ArrayList<>();
    @Getter
    private final List<Card> played = new ArrayList<>();
    private int mcValue;
    private int trValue;
    private int tilePointsValue;
    private int awardPointsValue = 0;

    private transient IntegerProperty tilePoints;
    private transient IntegerProperty mc;
    private transient IntegerProperty tr;
    private transient IntegerProperty awardPoints;
    private transient Map<ResourceType, IntegerProperty> resources;
    private transient Map<ResourceType, IntegerProperty> production;

    public PlayerState() {
        trValue = 20;
        initializeTransientProperties();
    }

    private void initializeTransientProperties() {
        resources = new EnumMap<>(ResourceType.class);
        production = new EnumMap<>(ResourceType.class);

        tr = new SimpleIntegerProperty(trValue);
        mc = new SimpleIntegerProperty(mcValue);
        tilePoints = new SimpleIntegerProperty(tilePointsValue);
        awardPoints = new SimpleIntegerProperty(awardPointsValue);

        for (ResourceType type : ResourceType.values()) {
            resources.put(type, new SimpleIntegerProperty(resourceValues.getOrDefault(type, 0)));
            production.put(type, new SimpleIntegerProperty(productionValues.getOrDefault(type, 0)));
        }
    }

    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        initializeTransientProperties();
    }

    @Serial
    private void writeObject(ObjectOutputStream out) throws IOException {
        trValue = tr.get();
        mcValue = mc.get();
        tilePointsValue = tilePoints.get();

        resources.forEach((key, value) -> resourceValues.put(key, value.get()));
        production.forEach((key, value) -> productionValues.put(key, value.get()));

        awardPointsValue = awardPoints.get();
        out.defaultWriteObject();
    }

    public IntegerProperty tilePointsProperty() {
        return tilePoints;
    }

    public IntegerProperty mcProperty() {
        return mc;
    }

    public IntegerProperty trProperty() {
        return tr;
    }

    public IntegerProperty awardPointsProperty() {
        return awardPoints;
    }

    public IntegerProperty resourceProperty(ResourceType type) {
        return resources.get(type);
    }

    public IntegerProperty productionProperty(ResourceType type) {
        return production.get(type);
    }

    public Map<ResourceType, IntegerProperty> getProductionMap() {
        return Collections.unmodifiableMap(production);
    }

    public int getCardCost(Card card, Corporation corporation) {
        if (corporation == null) {
            return card.getCost();
        }

        int discount = calculateCorporationDiscount(corporation, card);
        return Math.max(0, card.getCost() - discount);
    }

    private int calculateCorporationDiscount(Corporation corporation, Card card) {
        String corporationName = corporation.name();
        List<TagType> tags = card.getTags();

        return switch (corporationName) {
            case "Credicor" -> 1;
            case "Mining Guild" -> tags.contains(TagType.BUILDING) ? 2 : 0;
            case "Phobolog" -> tags.contains(TagType.SPACE) ? 4 : 0;
            case "Teractor" -> tags.contains(TagType.EARTH) ? 3 : 0;
            case "Inventrix" -> tags.contains(TagType.SCIENCE) ? 2 : 0;
            case "Thorgate" -> tags.contains(TagType.ENERGY) ? 3 : 0;
            default -> 0;
        };
    }

    public void reset(Corporation corporation) {
        tr.set(20);
        mc.set(corporation.startingMC());
        tilePoints.set(0);

        for (IntegerProperty resourceProp : resources.values()) {
            resourceProp.set(0);
        }

        for (IntegerProperty productionProp : production.values()) {
            productionProp.set(0);
        }

        if (corporation.startingProduction() != null) {
            corporation.startingProduction().forEach((resourceType, amount) ->
                    productionProperty(resourceType).set(amount)
            );
        }

        hand.clear();
        played.clear();
        claimedMilestones.clear();
    }
}
