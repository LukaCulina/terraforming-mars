package hr.terraforming.mars.terraformingmars.model;

import hr.terraforming.mars.terraformingmars.enums.Award;
import hr.terraforming.mars.terraformingmars.enums.Milestone;
import hr.terraforming.mars.terraformingmars.enums.TileType;
import hr.terraforming.mars.terraformingmars.service.PlacementService;
import hr.terraforming.mars.terraformingmars.util.HexGridUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.*;

@Slf4j
public class GameBoard implements Serializable {

    public static final int MAX_MILESTONES = 3;
    public static final int MAX_OXYGEN = 14;
    public static final int MAX_TEMPERATURE = 8;
    public static final int MIN_TEMPERATURE = -30;
    public static final int MAX_OCEANS = 9;
    public static final int MAX_AWARDS = 3;
    private static final Set<String> OCEAN_COORDINATES = Set.of(
            "0,1", "0,3", "0,4",
            "1,5",
            "3,7",
            "4,3", "4,4", "4,5",
            "5,5", "5,6", "5,7",
            "8,4"
    );
    private final Map<Milestone, Player> claimedMilestones = new EnumMap<>(Milestone.class);
    private final Map<Award, Player> fundedAwards = new EnumMap<>(Award.class);

    @Getter
    private final List<Tile> tiles = new ArrayList<>();
    @Getter
    private int oxygenLevel;
    @Getter
    private int temperature;
    @Getter
    private int oceansPlaced;
    @Getter
    private boolean isFinalGeneration = false;
    @Setter
    private transient Runnable onGlobalParametersChanged;
    private transient PlacementService placementService;

    public GameBoard() {
        this.placementService = new PlacementService(this);
        initBoard();
        this.oxygenLevel = 0;
        temperature = MIN_TEMPERATURE;
        this.oceansPlaced = 0;
        claimedMilestones.clear();
    }

    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.placementService = new PlacementService(this);
    }

    private void initBoard() {
        int[] hexesInRow = HexGridUtils.getHexesInRow();
        for (int row = 0; row < hexesInRow.length; row++) {
            int hexesThisRow = hexesInRow[row];
            for (int col = 0; col < hexesThisRow; col++) {
                tiles.add(new Tile(row, col, TileType.LAND, null));
            }
        }
    }

    public boolean isValidPlacement(TileType placementType, Tile tile, Player player) {
        return placementService.isValidPlacement(placementType, tile, player);
    }

    public void placeOcean(Tile onTile, Player forPlayer) {
        placementService.placeOceanTile(onTile, forPlayer);
    }

    public void placeGreenery(Tile onTile, Player forPlayer) {
        placementService.placeGreeneryTile(onTile, forPlayer);
    }

    public void placeCity(Tile onTile, Player forPlayer) {
        placementService.placeCityTile(onTile, forPlayer);
    }

    public void incrementOceanCount() {
        this.oceansPlaced++;
        notifyUI();
        checkIfGameShouldEnd();
    }

    private void checkIfGameShouldEnd() {
        if (isFinalGeneration) return;

        if (getOxygenLevel() >= MAX_OXYGEN &&
                getTemperature() >= MAX_TEMPERATURE &&
                getOceansPlaced() >= MAX_OCEANS) {

            this.isFinalGeneration = true;
            log.info("All global parameters are at maximum. The game will end after this generation.");
        }
    }

    public boolean canPlaceOcean() {
        return getOceansPlaced() < MAX_OCEANS;
    }

    public boolean isOceanCoordinate(int row, int col) {
        return OCEAN_COORDINATES.contains(row + "," + col);
    }

    public int[] getHexesInRow() {
        return HexGridUtils.getHexesInRow();
    }

    public boolean canIncreaseOxygen() {
        if (getOxygenLevel() < MAX_OXYGEN) {
            oxygenLevel++;
            notifyUI();
            checkIfGameShouldEnd();
            return true;
        }
        return false;
    }

    public boolean canIncreaseTemperature() {
        if (temperature >= MAX_TEMPERATURE) {
            return false;
        }

        temperature = Math.min(temperature + 2, MAX_TEMPERATURE);
        notifyUI();
        checkIfGameShouldEnd();
        return true;
    }

    public void notifyUI() {
        if (onGlobalParametersChanged != null) {
            onGlobalParametersChanged.run();
        }
    }

    public boolean canClaimMilestone(Milestone milestone, Player player) {
        if (claimedMilestones.size() >= MAX_MILESTONES) {
            log.warn("Cannot claim milestone: maximum number of milestones ({}) has been reached.", MAX_MILESTONES);
            return false;
        }

        if (claimedMilestones.containsKey(milestone)) {
            log.warn("Milestone '{}' has already been claimed.", milestone.getName());
            return false;
        }

        if (!milestone.canClaim(player)) {
            log.warn("{} does not meet the requirements for milestone '{}'.", player.getName(), milestone.getName());
            return false;
        }
        claimedMilestones.put(milestone, player);
        player.addClaimedMilestone(milestone);

        log.info("{} has claimed the milestone: {}!", player.getName(), milestone.getName());
        return true;
    }

    public int getNextAwardCost() {
        int size = fundedAwards.size();
        if (size == 0) return 8;
        if (size == 1) return 14;
        return 20;
    }

    public boolean canFundAward(Award award, Player player) {
        if (fundedAwards.size() >= MAX_AWARDS) {
            log.warn("Maximum number of awards already funded.");
            return false;
        }
        if (fundedAwards.containsKey(award)) {
            log.warn("Award '{}' has already been funded.", award.getName());
            return false;
        }
        fundedAwards.put(award, player);
        log.info("{} funded the award: {}", player.getName(), award.getName());
        return true;
    }

    public Map<Award, Player> getFundedAwards() {
        return Collections.unmodifiableMap(fundedAwards);
    }

    public Map<Milestone, Player> getClaimedMilestones() {
        return Collections.unmodifiableMap(claimedMilestones);
    }

    public List<Tile> getAdjacentTiles(Tile centerTile) {
        return HexGridUtils.getAdjacentTiles(centerTile, this.tiles);
    }

    public Tile getTileAt(int row, int col) {
        return tiles.stream()
                .filter(t -> t.getRow() == row && t.getCol() == col)
                .findFirst()
                .orElse(null);
    }

    // Služe samo za potrebu bržeg testiranja kraja igre

    public void setTemperature(int temp) {
        this.temperature = temp;
        checkIfGameShouldEnd();
        notifyUI();
    }

    public void setOxygenLevel(int oxygen) {
        this.oxygenLevel = oxygen;
        checkIfGameShouldEnd();
        notifyUI();
    }

    public void setOceansPlaced(int oceans) {
        this.oceansPlaced = oceans;
        checkIfGameShouldEnd();
        notifyUI();
    }
}
