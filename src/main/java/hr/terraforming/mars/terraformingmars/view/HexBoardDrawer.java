package hr.terraforming.mars.terraformingmars.view;

import hr.terraforming.mars.terraformingmars.manager.PlacementManager;
import hr.terraforming.mars.terraformingmars.model.GameBoard;
import hr.terraforming.mars.terraformingmars.model.Player;
import hr.terraforming.mars.terraformingmars.model.Tile;
import hr.terraforming.mars.terraformingmars.enums.TileType;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HexBoardDrawer {

    private final AnchorPane hexBoardPane;
    @Setter
    private GameBoard gameBoard;
    private final PlacementManager placementManager;
    private static final double HEX_SPACING = 0.85;
    private double hexRadius = 0;

    public HexBoardDrawer(AnchorPane hexBoardPane, GameBoard gameBoard, PlacementManager placementManager) {
        this.hexBoardPane = hexBoardPane;
        this.gameBoard = gameBoard;
        this.placementManager = placementManager;
    }

    public void drawBoard() {
        double paneWidth = hexBoardPane.getWidth();
        double paneHeight = hexBoardPane.getHeight();
        if (paneWidth <= 0 || paneHeight <= 0) return;

        hexBoardPane.getChildren().clear();

        calculateHexRadius(paneWidth, paneHeight);
        double hexWidth = 2 * hexRadius;
        double hexHeight = Math.sqrt(3) * hexRadius;

        int numRows = gameBoard.getHexesInRow().length;
        double startY = (paneHeight - (numRows * hexHeight * HEX_SPACING)) / 2.0 + hexHeight / 2;

        for (Tile tile : gameBoard.getTiles()) {
            double[] coords = calculateHexPosition(tile, hexWidth, hexHeight, paneWidth, startY);
            double x = coords[0];
            double y = coords[1];

            StackPane tileNode = createTileNode(tile);

            tileNode.setLayoutX(x - hexRadius);
            tileNode.setLayoutY(y - (hexHeight / 2));

            hexBoardPane.getChildren().add(tileNode);
        }

        if (placementManager.isPlacementMode()) {
            highlightValidTiles();
        }
    }

    private StackPane createTileNode(Tile tile) {
        StackPane stackPane = new StackPane();
        stackPane.setPrefSize(2 * hexRadius, Math.sqrt(3) * hexRadius);
        stackPane.setUserData(tile);

        Polygon hex = createHexPolygon();
        hex.setFill(getTileColor(tile));
        hex.setPickOnBounds(false);
        hex.setMouseTransparent(true);

        stackPane.getChildren().add(hex);

        if (tile.getOwner() != null) {
            Player owner = tile.getOwner();

            Circle markerBackground = new Circle(hexRadius * 0.4);
            markerBackground.setFill(owner.getPlayerColor());
            markerBackground.setStroke(Color.WHITE);
            markerBackground.setStrokeWidth(1.5);
            markerBackground.setMouseTransparent(true);

            Text playerNumberText = new Text(String.valueOf(owner.getPlayerNumber()));
            playerNumberText.setFill(owner.getPlayerColor().getBrightness() < 0.5 ? Color.WHITE : Color.BLACK);
            playerNumberText.setMouseTransparent(true);

            double fontSize = hexRadius * 0.5;
            playerNumberText.setFont(Font.font("System", FontWeight.BOLD, fontSize));

            stackPane.getChildren().addAll(markerBackground, playerNumberText);
        }

        stackPane.setOnMouseClicked(this::handleHexClick);

        return stackPane;
    }

    private void calculateHexRadius(double paneWidth, double paneHeight) {
        int numRows = gameBoard.getHexesInRow().length;
        int maxHexesInRow = 9;

        double radiusByWidth = (paneWidth / (maxHexesInRow * 1.5 + 0.5)) / (2 * HEX_SPACING);
        double radiusByHeight = (paneHeight / numRows) / (2 * HEX_SPACING);
        hexRadius = Math.min(radiusByWidth, radiusByHeight) * 1.3;
    }

    private double[] calculateHexPosition(Tile tile, double hexWidth, double hexHeight, double paneWidth, double startY) {
        int[] hexesInRow = gameBoard.getHexesInRow();
        double rowWidth = hexesInRow[tile.getRow()] * hexWidth * HEX_SPACING;
        double startX = (paneWidth - rowWidth) / 2.0;

        double x = startX + tile.getCol() * hexWidth * HEX_SPACING;
        double y = startY + tile.getRow() * hexHeight * HEX_SPACING;
        return new double[]{x, y};
    }

    private Polygon createHexPolygon() {
        Polygon hex = new Polygon();
        for (int a = 0; a < 6; a++) {
            double angle = Math.toRadians(60 * a - 30.0);
            double px = hexRadius * Math.cos(angle);
            double py = hexRadius * Math.sin(angle);
            hex.getPoints().addAll(px, py);
        }
        hex.setStroke(Color.BLACK);
        hex.setStrokeWidth(1.5);
        return hex;
    }

    private Color getTileColor(Tile tile) {
        if (gameBoard.isOceanCoordinate(tile.getRow(), tile.getCol()) && tile.getType() == TileType.LAND) {
            return Color.rgb(187, 225, 250);
        }
        return switch (tile.getType()) {
            case OCEAN -> Color.DARKBLUE;
            case CITY -> Color.SLATEGRAY;
            case GREENERY -> Color.FORESTGREEN;
            case LAND -> Color.WHITE;
        };
    }

    private void highlightValidTiles() {
        TileType typeToPlace = placementManager.getTileTypeToPlace();
        Player placementOwner = placementManager.getPlacementOwner();
        if (typeToPlace == null || placementOwner == null) return;

        for (Node node : hexBoardPane.getChildren()) {
            if (node instanceof StackPane pane) {
                Tile tile = (Tile) pane.getUserData();

                if (tile != null && gameBoard.isValidPlacement(typeToPlace, tile, placementOwner)) {
                    pane.getStyleClass().add("hand-cursor");

                    if (!pane.getChildren().isEmpty() && pane.getChildren().getFirst() instanceof Polygon hex) {
                        hex.getStyleClass().add("valid-placement-hex");
                    }
                }
            }
        }
    }

    private void handleHexClick(MouseEvent event) {
        StackPane clickedPane = (StackPane) event.getSource();
        Tile clickedTile = (Tile) clickedPane.getUserData();
        if (clickedTile == null) {
            log.warn("Hex click detected on a node with no Tile data.");
            return;
        }

        if (placementManager.isPlacementMode()) {
            if (!clickedPane.getChildren().isEmpty() && clickedPane.getChildren().getFirst() instanceof Polygon hex && hex.getStyleClass().contains("valid-placement-hex")) {
                    placementManager.executePlacement(clickedTile);
                }

        } else {
            String ownerInfo = clickedTile.getOwner() != null ? ", Owner: " + clickedTile.getOwner().getName() : "";

            log.info("Tile info requested for ({}, {}). Type: {}{}.",
                    clickedTile.getRow(),
                    clickedTile.getCol(),
                    clickedTile.getType(),
                    ownerInfo
            );
        }
    }
}