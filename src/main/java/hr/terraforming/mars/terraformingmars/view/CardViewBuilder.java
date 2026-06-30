package hr.terraforming.mars.terraformingmars.view;

import hr.terraforming.mars.terraformingmars.enums.TagType;
import hr.terraforming.mars.terraformingmars.model.Card;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;

public final class CardViewBuilder {

    private static final double BASE_CARD_WIDTH = 180.0;
    private static final double BASE_CARD_HEIGHT = 250.0;
    private static final double MAX_SCALE = 1.0;

    private CardViewBuilder() {
        throw new IllegalStateException("Utility class");
    }

    public static void setupCardTilePane(TilePane tilePane, int minColumns, int maxColumns) {
        tilePane.setHgap(15);
        tilePane.setVgap(15);

        DoubleBinding scaleBinding = Bindings.createDoubleBinding(
                () -> calculateOptimalScale(tilePane.getWidth()),
                tilePane.widthProperty()
        );

        tilePane.prefTileWidthProperty().bind(scaleBinding.multiply(BASE_CARD_WIDTH));
        tilePane.prefTileHeightProperty().bind(scaleBinding.multiply(BASE_CARD_HEIGHT));

        tilePane.prefColumnsProperty().bind(
                Bindings.createIntegerBinding(() -> {
                    double width = tilePane.getWidth();
                    if (width < 100) return minColumns;

                    double scale = calculateOptimalScale(width);
                    double cardWidth = BASE_CARD_WIDTH * scale;
                    int cols = (int) ((width + 5) / (cardWidth + 5));
                    return Math.clamp(cols, minColumns, maxColumns);
                }, tilePane.widthProperty())
        );
    }

    public static VBox createCardNode(Card card, Pane parentContainer) {
        VBox cardBox = createBaseCard(card);

        if (parentContainer instanceof TilePane tilePane) {
            DoubleBinding scaleBinding = Bindings.createDoubleBinding(
                    () -> calculateOptimalScale(tilePane.getWidth()),
                    tilePane.widthProperty()
            );

            cardBox.scaleXProperty().bind(scaleBinding);
            cardBox.scaleYProperty().bind(scaleBinding);

        } else if (parentContainer != null) {
            DoubleBinding scaleBinding = Bindings.createDoubleBinding(() ->
                            calculateOptimalScale(parentContainer.getWidth()),
                    parentContainer.widthProperty()
            );

            cardBox.scaleXProperty().bind(scaleBinding);
            cardBox.scaleYProperty().bind(scaleBinding);
        }

        return cardBox;
    }

    private static VBox createBaseCard(Card card) {
        VBox cardBox = new VBox(5);
        cardBox.setAlignment(Pos.TOP_CENTER);
        cardBox.getStyleClass().add("card-view");

        cardBox.setPrefSize(BASE_CARD_WIDTH, BASE_CARD_HEIGHT);
        cardBox.setMinSize(BASE_CARD_WIDTH, BASE_CARD_HEIGHT);
        cardBox.setMaxSize(BASE_CARD_WIDTH, BASE_CARD_HEIGHT);

        Label costLabel = new Label(card.getCost() + " MC");
        costLabel.getStyleClass().add("card-cost-label");

        Label nameLabel = new Label(card.getName());
        nameLabel.setWrapText(true);
        nameLabel.getStyleClass().add("card-name-label");

        HBox tagBox = new HBox(5);
        tagBox.setAlignment(Pos.CENTER);
        for (TagType tag : card.getTags()) {
            Region tagIcon = createTagIcon(tag);
            tagBox.getChildren().add(tagIcon);
        }

        Label descLabel = new Label(card.getDescription());
        descLabel.setWrapText(true);
        descLabel.getStyleClass().add("card-description-label");

        VBox descriptionWrapper = new VBox(descLabel);
        descriptionWrapper.setAlignment(Pos.CENTER);
        VBox.setVgrow(descriptionWrapper, Priority.ALWAYS);

        cardBox.getChildren().addAll(costLabel, nameLabel, tagBox, descriptionWrapper);

        Label vpLabel = new Label(String.valueOf(card.getVictoryPoints()));
        vpLabel.getStyleClass().add("vp-label");
        StackPane vpContainer = new StackPane(vpLabel);
        vpContainer.setAlignment(Pos.BOTTOM_RIGHT);
        vpContainer.setPadding(new Insets(0, 3, 3, 0));
        vpContainer.setVisible(card.getVictoryPoints() > 0);
        vpContainer.setManaged(card.getVictoryPoints() > 0);

        cardBox.getChildren().add(vpContainer);

        String tooltipText = card.getName() + "\n\n" + card.getDescription();

        Tooltip tooltip = new Tooltip(tooltipText);

        tooltip.setPrefWidth(250);
        tooltip.setWrapText(true);

        tooltip.setShowDelay(javafx.util.Duration.millis(100));
        tooltip.setShowDuration(javafx.util.Duration.INDEFINITE);

        Tooltip.install(cardBox, tooltip);

        return cardBox;
    }

    private static double calculateOptimalScale(double containerWidth) {
        if (containerWidth <= 0) return MAX_SCALE;

        if (containerWidth < 600) return 0.75;
        if (containerWidth < 800) return 0.85;
        if (containerWidth < 1000) return 0.95;

        return MAX_SCALE;
    }

    public static Region createTagIcon(TagType tag) {
        Region tagIcon = new Region();
        tagIcon.getStyleClass().add("tag-icon");
        tagIcon.getStyleClass().add("tag-" + tag.name().toLowerCase());
        return tagIcon;
    }
}