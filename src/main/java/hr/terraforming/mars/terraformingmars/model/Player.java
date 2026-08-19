package hr.terraforming.mars.terraformingmars.model;

import hr.terraforming.mars.terraformingmars.enums.Milestone;
import hr.terraforming.mars.terraformingmars.enums.ResourceType;
import hr.terraforming.mars.terraformingmars.enums.TagType;
import hr.terraforming.mars.terraformingmars.enums.TileType;
import javafx.beans.property.IntegerProperty;
import javafx.scene.paint.Color;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Slf4j
public class Player implements Serializable {

    private static final List<Color> PLAYER_COLORS = List.of(
            Color.rgb(220, 60, 60), Color.rgb(60, 150, 220), Color.rgb(80, 180, 80),
            Color.rgb(240, 180, 50), Color.rgb(100, 100, 100)
    );
    @Getter
    private final int playerNumber;
    private final PlayerState state;
    @Getter
    @Setter
    private String name;
    @Getter
    private Corporation corporation;
    @Setter
    private transient GameBoard board;

    public Player(String name, int playerNumber) {
        this.name = name;
        this.playerNumber = playerNumber;
        this.state = new PlayerState();
    }

    public Color getPlayerColor() {
        return PLAYER_COLORS.get((playerNumber - 1) % PLAYER_COLORS.size());
    }

    public void setCorporation(Corporation corporation) {
        this.corporation = corporation;
        mcProperty().set(corporation.startingMC());
        corporation.startingResources().forEach(this::addResource);
        corporation.startingProduction().forEach(this::increaseProduction);
    }

    public IntegerProperty mcProperty() {
        return state.mcProperty();
    }

    public IntegerProperty trProperty() {
        return state.trProperty();
    }

    public IntegerProperty resourceProperty(ResourceType type) {
        return state.resourceProperty(type);
    }

    public IntegerProperty productionProperty(ResourceType type) {
        return state.productionProperty(type);
    }

    public int getMC() {
        return state.mcProperty().get();
    }

    public int getTR() {
        return state.trProperty().get();
    }

    public int getProduction(ResourceType type) {
        return state.productionProperty(type).get();
    }

    public Map<ResourceType, IntegerProperty> getProductionMap() {
        return state.getProductionMap();
    }

    public List<Card> getHand() {
        return state.getHand();
    }

    public List<Card> getPlayed() {
        return state.getPlayed();
    }

    public void addCardsToHand(List<Card> cardsToAdd) {
        getHand().addAll(cardsToAdd);
    }

    public void addMC(int amount) {
        mcProperty().set(getMC() + amount);
    }

    public boolean canSpendMC(int amount) {
        if (getMC() >= amount) {
            mcProperty().set(getMC() - amount);
            return true;
        }
        return false;
    }

    public void addResource(ResourceType type, int amount) {
        state.resourceProperty(type).set(state.resourceProperty(type).get() + amount);
    }

    public void increaseProduction(ResourceType type, int amount) {
        state.productionProperty(type).set(state.productionProperty(type).get() + amount);

        if (corporation != null && corporation.name().equals("Helion") && type == ResourceType.ENERGY && amount > 0) {
            addMC(2);
        }
    }

    public void increaseTR(int amount) {
        trProperty().set(getTR() + amount);

        if (corporation != null && corporation.name().equals("United Nations Mars Initiative")) {
            addMC(3 * amount);
        }
    }

    public int getEffectiveCardCost(Card card) {
        return state.getCardCost(card, corporation);
    }

    public boolean canPlayCard(Card card) {
        if (card == null || !getHand().contains(card)) return false;

        int finalCost = state.getCardCost(card, corporation);

        if (getMC() < finalCost) return false;

        return card.getRequirement().test(this, board);
    }

    public void playCard(Card card, GameManager gameManager) {
        if (canPlayCard(card)) {
            int finalCost = state.getCardCost(card, corporation);
            canSpendMC(finalCost);
            getHand().remove(card);
            getPlayed().add(card);
            card.play(this, gameManager);

            if (corporation != null) {
                String corpName = corporation.name();

                if (corpName.equals("Interplanetary Cinematics") && card.getTags().contains(TagType.SPACE)) {
                    addMC(2);
                } else if (corpName.equals("Saturn Systems") && card.getTags().contains(TagType.JOVIAN)) {
                    increaseProduction(ResourceType.MEGA_CREDITS, 1);
                }
            }
        }
    }

    public int getGreeneryCost() {
        return (getCorporation() != null && "Ecoline".equals(getCorporation().name())) ? 7 : 8;
    }

    public void spendPlantsForGreenery() {
        int cost = getGreeneryCost();

        if (resourceProperty(ResourceType.PLANTS).get() >= cost) {
            resourceProperty(ResourceType.PLANTS).set(resourceProperty(ResourceType.PLANTS).get() - cost);
        } else {
            log.warn("{} failed to place greenery: insufficient plants (has {}, needs {}).",
                    name, resourceProperty(ResourceType.PLANTS).get(), cost);
        }
    }

    public int countTags(TagType tagToCount) {
        return (int) getPlayed().stream()
                .flatMap(card -> card.getTags().stream())
                .filter(tag -> tag == tagToCount)
                .count();
    }

    public void addClaimedMilestone(Milestone milestone) {
        state.getClaimedMilestones().add(milestone);
    }

    public int getMilestonePoints() {
        return state.getClaimedMilestones().size() * 5;
    }

    public void addAwardPoints(int points) {
        state.awardPointsProperty().set(state.awardPointsProperty().get() + points);
    }

    public int getAwardPoints() {
        return state.awardPointsProperty().get();
    }

    public long getOwnedCityCount() {
        if (board == null) return 0;
        return board.getTiles().stream().filter(t -> t.getOwner() == this && t.getType() == TileType.CITY).count();
    }

    public long getOwnedGreeneryCount() {
        if (board == null) return 0;
        return board.getTiles().stream().filter(t -> t.getOwner() == this && t.getType() == TileType.GREENERY).count();
    }

    public void calculateTilePoints() {
        if (board == null) return;
        long greeneryPoints = getOwnedGreeneryCount();
        long cityPoints = board.getTiles().stream()
                .filter(t -> t.getOwner() == this && t.getType() == TileType.CITY)
                .mapToLong(city -> board.getAdjacentTiles(city).stream()
                        .filter(adj -> adj.getType() == TileType.GREENERY).count())
                .sum();

        int totalTilePoints = (int) (greeneryPoints + cityPoints);
        state.tilePointsProperty().set(totalTilePoints);

        log.info("{} scored {} points from tiles.", name, totalTilePoints);
    }

    public int getTilePoints() {
        return state.tilePointsProperty().get();
    }

    public int getFinalScore() {
        int cardPoints = getPlayed().stream().mapToInt(Card::getVictoryPoints).sum();
        return getTR() + getMilestonePoints() + getAwardPoints() + getTilePoints() + cardPoints;
    }

    public void resetForNewGame() {
        if (corporation != null) {
            state.reset(corporation);
        } else {
            log.error("Cannot reset player state, corporation is null for player {}.", name);
        }
    }
}