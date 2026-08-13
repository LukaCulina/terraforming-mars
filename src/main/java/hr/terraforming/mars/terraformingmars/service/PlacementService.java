package hr.terraforming.mars.terraformingmars.service;

import hr.terraforming.mars.terraformingmars.enums.PlacementMode;
import hr.terraforming.mars.terraformingmars.enums.ResourceType;
import hr.terraforming.mars.terraformingmars.enums.StandardProject;
import hr.terraforming.mars.terraformingmars.enums.TileType;
import hr.terraforming.mars.terraformingmars.model.*;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public record PlacementService(GameBoard gameBoard) {

    public boolean isValidPlacement(TileType placementType, Tile tile, Player player) {
        if (tile.getOwner() != null || tile.getType() != TileType.LAND) {
            return false;
        }

        switch (placementType) {
            case CITY:
                if (gameBoard.isOceanCoordinate(tile.getRow(), tile.getCol())) {
                    return false;
                }
                return gameBoard.getAdjacentTiles(tile).stream().noneMatch(t -> t.getType() == TileType.CITY);

            case GREENERY:
                if (gameBoard.isOceanCoordinate(tile.getRow(), tile.getCol())) {
                    return false;
                }

                List<Tile> ownedTiles = gameBoard.getTiles().stream()
                        .filter(t -> player.equals(t.getOwner()))
                        .toList();

                if (ownedTiles.isEmpty()) {
                    return true;
                }

                boolean hasAnyValidAdjacentSpot = ownedTiles.stream()
                        .flatMap(ownedTile -> gameBoard.getAdjacentTiles(ownedTile).stream())
                        .anyMatch(adjacentTile ->
                                adjacentTile.getOwner() == null &&
                                        adjacentTile.getType() == TileType.LAND &&
                                        !gameBoard.isOceanCoordinate(adjacentTile.getRow(), adjacentTile.getCol())
                        );

                if (hasAnyValidAdjacentSpot) {
                    return gameBoard.getAdjacentTiles(tile).stream()
                            .anyMatch(ownedTiles::contains);
                } else {
                    return true;
                }

            case OCEAN:
                return gameBoard.isOceanCoordinate(tile.getRow(), tile.getCol());

            default:
                return false;
        }
    }

    public void placeOceanTile(Tile onTile, Player forPlayer) {
        if (gameBoard.getOceansPlaced() < GameBoard.MAX_OCEANS && gameBoard.isOceanCoordinate(onTile.getRow(), onTile.getCol())) {
            onTile.setType(TileType.OCEAN);
            gameBoard.incrementOceanCount();
            forPlayer.increaseTR(1);
            gameBoard.notifyUI();

            log.info("Ocean placed by {}. Total oceans: {}", forPlayer.getName(), gameBoard.getOceansPlaced());
        }
    }

    public void placeGreeneryTile(Tile onTile, Player forPlayer) {
        onTile.setType(TileType.GREENERY);
        onTile.setOwner(forPlayer);

        if (gameBoard.canIncreaseOxygen()) {
            forPlayer.increaseTR(1);
        }

        gameBoard.notifyUI();

        log.info("Greenery placed by {} on tile ({}, {}).", forPlayer.getName(), onTile.getRow(), onTile.getCol());
    }

    public void placeCityTile(Tile onTile, Player forPlayer) {
        onTile.setType(TileType.CITY);
        onTile.setOwner(forPlayer);
        forPlayer.increaseProduction(ResourceType.MEGA_CREDITS, 1);
        gameBoard.notifyUI();

        log.info("City placed by {} on tile ({}, {}).", forPlayer.getName(), onTile.getRow(), onTile.getCol());
    }

    public void placeByMode(PlacementContext context) {
        switch (context.mode()) {
            case FINAL_GREENERY -> placeFinalGreeneryTile(context.tile(), context.owner());
            case PLANT_CONVERSION -> placeGreeneryFromPlants(context.tile(), context.owner());
            case STANDARD_PROJECT, CARD -> placeWithPayment(context);
            default -> log.error("Unknown placement mode: {}", context.mode());
        }
    }

    private void placeFinalGreeneryTile(Tile tile, Player owner) {
        int cost = owner.getGreeneryCost();

        if (owner.resourceProperty(ResourceType.PLANTS).get() >= cost) {
            owner.spendPlantsForGreenery();
            placeGreeneryTile(tile, owner);
        } else {
            log.warn("{} does not have enough plants (has {}, needs {}).",
                    owner.getName(), owner.resourceProperty(ResourceType.PLANTS).get(), cost);
        }
    }

    private void placeGreeneryFromPlants(Tile tile, Player owner) {
        placeGreeneryTile(tile, owner);
        owner.spendPlantsForGreenery();
    }

    private void placeWithPayment(PlacementContext context) {
        switch (context.tileType()) {
            case OCEAN -> placeOceanTile(context.tile(), context.owner());
            case GREENERY -> placeGreeneryTile(context.tile(), context.owner());
            case CITY -> placeCityTile(context.tile(), context.owner());
            default -> {
                log.error("Unexpected tile type: {}", context.tileType());
                return;
            }
        }

        if (context.card() != null) {
            context.owner().playCard(context.card(), context.gameManager());
        } else if (context.project() != null) {
            int finalCost = CostService.getFinalProjectCost(context.project(), context.owner());
            context.owner().canSpendMC(finalCost);
        }
    }

    public record PlacementContext(
            PlacementMode mode,
            Tile tile,
            Player owner,
            GameManager gameManager,
            TileType tileType,
            Card card,
            StandardProject project
    ) {
    }
}
