package hr.terraforming.mars.terraformingmars.service;

import hr.terraforming.mars.terraformingmars.enums.TileType;
import hr.terraforming.mars.terraformingmars.model.GameBoard;
import hr.terraforming.mars.terraformingmars.model.Player;
import hr.terraforming.mars.terraformingmars.model.Tile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlacementServiceTest {

    private GameBoard board;
    private Player player;
    private Player opponent;

    @BeforeEach
    void setUp() {
        board = new GameBoard();
        player = new Player("Player 1", 1);
        opponent = new Player("Player 2", 2);
    }

    private Tile findFreeLandTile() {
        return board.getTiles().stream()
                .filter(t -> t.getType() == TileType.LAND && t.getOwner() == null)
                .filter(t -> !board.isOceanCoordinate(t.getRow(), t.getCol()))
                .findFirst()
                .orElseThrow();
    }

    private Tile findLandTileWithFreeNonOceanNeighbor() {
        return board.getTiles().stream()
                .filter(t -> t.getType() == TileType.LAND && t.getOwner() == null)
                .filter(t -> !board.isOceanCoordinate(t.getRow(), t.getCol()))
                .filter(t -> board.getAdjacentTiles(t).stream()
                        .anyMatch(n -> n.getType() == TileType.LAND && n.getOwner() == null
                                && !board.isOceanCoordinate(n.getRow(), n.getCol())))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void greenery_firstPlacement_validAnywhereOnLand() {
        assertTrue(board.isValidPlacement(TileType.GREENERY, findFreeLandTile(), player));
    }

    @Test
    void greenery_invalidOnOceanCoordinate() {
        Tile oceanTile = board.getTiles().stream()
                .filter(t -> board.isOceanCoordinate(t.getRow(), t.getCol()))
                .findFirst().orElseThrow();

        assertFalse(board.isValidPlacement(TileType.GREENERY, oceanTile, player));
    }

    @Test
    void greenery_mustBeAdjacentToOwnedTile_whenFreeSpotExists() {
        Tile owned = findLandTileWithFreeNonOceanNeighbor();
        owned.setOwner(player);

        Tile adjacentFree = board.getAdjacentTiles(owned).stream()
                .filter(t -> t.getType() == TileType.LAND && t.getOwner() == null)
                .filter(t -> !board.isOceanCoordinate(t.getRow(), t.getCol()))
                .findFirst().orElseThrow();

        assertTrue(board.isValidPlacement(TileType.GREENERY, adjacentFree, player));

        Tile farAway = board.getTiles().stream()
                .filter(t -> t.getType() == TileType.LAND && t.getOwner() == null)
                .filter(t -> !board.isOceanCoordinate(t.getRow(), t.getCol()))
                .filter(t -> !board.getAdjacentTiles(owned).contains(t))
                .findFirst().orElseThrow();

        assertFalse(board.isValidPlacement(TileType.GREENERY, farAway, player));
    }

    @Test
    void city_validWhenNoAdjacentCity() {
        assertTrue(board.isValidPlacement(TileType.CITY, findFreeLandTile(), player));
    }

    @Test
    void city_invalidWhenAdjacentToExistingCity() {
        Tile existingCity = findLandTileWithFreeNonOceanNeighbor();
        existingCity.setOwner(opponent);
        existingCity.setType(TileType.CITY);

        Tile adjacent = board.getAdjacentTiles(existingCity).stream()
                .filter(t -> t.getType() == TileType.LAND && t.getOwner() == null)
                .filter(t -> !board.isOceanCoordinate(t.getRow(), t.getCol()))
                .findFirst().orElseThrow();

        assertFalse(board.isValidPlacement(TileType.CITY, adjacent, player));
    }

    @Test
    void city_invalidOnOceanCoordinate() {
        Tile oceanTile = board.getTiles().stream()
                .filter(t -> board.isOceanCoordinate(t.getRow(), t.getCol()))
                .findFirst().orElseThrow();

        assertFalse(board.isValidPlacement(TileType.CITY, oceanTile, player));
    }

    @Test
    void ocean_validOnlyOnDesignatedOceanCoordinate() {
        Tile oceanTile = board.getTiles().stream()
                .filter(t -> board.isOceanCoordinate(t.getRow(), t.getCol()))
                .findFirst().orElseThrow();

        assertTrue(board.isValidPlacement(TileType.OCEAN, oceanTile, player));
        assertFalse(board.isValidPlacement(TileType.OCEAN, findFreeLandTile(), player));
    }
}