package hr.terraforming.mars.terraformingmars.manager;

import hr.terraforming.mars.terraformingmars.controller.game.ChooseCardsController;
import hr.terraforming.mars.terraformingmars.controller.game.GameScreenController;
import hr.terraforming.mars.terraformingmars.enums.ActionType;
import hr.terraforming.mars.terraformingmars.enums.PlayerType;
import hr.terraforming.mars.terraformingmars.model.*;
import hr.terraforming.mars.terraformingmars.util.ScreenUtils;
import javafx.application.Platform;
import javafx.stage.Window;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;

@Slf4j
public class ResearchPhaseManager {

    private final GameManager gameManager;
    private final Window ownerWindow;
    private final Runnable onResearchComplete;
    private final GameScreenController controller;
    private int researchPlayerIndex = 0;

    public ResearchPhaseManager(GameManager gameManager, Window ownerWindow, GameScreenController controller, Runnable onResearchComplete) {
        this.gameManager = gameManager;
        this.ownerWindow = ownerWindow;
        this.controller = controller;
        this.onResearchComplete = onResearchComplete;
    }

    public void start() {
        researchPlayerIndex = 0;
        Platform.runLater(this::showScreenForNextPlayer);
    }

    private void showScreenForNextPlayer() {
        if (researchPlayerIndex >= gameManager.getPlayers().size()) {
            log.debug("All players finished research! Calling onResearchComplete | currentPhase={}",
                    gameManager.getCurrentPhase());
            onResearchComplete.run();
            return;
        }

        Player currentPlayer = gameManager.getPlayers().get(researchPlayerIndex);
        String myPlayerName = ApplicationConfiguration.getInstance().getMyPlayerName();
        PlayerType playerType = ApplicationConfiguration.getInstance().getPlayerType();

        if (playerType != PlayerType.LOCAL && !currentPlayer.getName().equals(myPlayerName)) {
            return;
        }

        List<Card> offer = gameManager.drawCards(4);

        if (offer.isEmpty()) {
            finishForCurrentPlayer(Collections.emptyList());
            return;
        }

        ScreenUtils.showAsModal(
                ownerWindow,
                "ChooseCards.fxml",
                "Research Phase - " + currentPlayer.getName(),
                (ChooseCardsController c) -> c.setup(currentPlayer, offer, this::finishForCurrentPlayer, gameManager, true)
        );
    }

    private void finishForCurrentPlayer(List<Card> boughtCards) {
        if (researchPlayerIndex >= gameManager.getPlayers().size()) {
            log.warn("finishForCurrentPlayer called but research has already been complete!");
            return;
        }

        Player currentPlayer = gameManager.getPlayers().get(researchPlayerIndex);

        if (!boughtCards.isEmpty()) {
            String cardNames = boughtCards.stream().map(Card::getName).reduce((a, b) -> a + ", " + b).orElse("");

            int count = boughtCards.size();
            String card = (count == 1) ? "card" : "cards";

            String message = "bought " + count + " " + card;

            GameMove modalMove = new GameMove(
                    currentPlayer.getName(),
                    ActionType.OPEN_CHOOSE_CARDS_MODAL,
                    cardNames,
                    message,
                    LocalDateTime.now(ZoneOffset.UTC)
            );
            controller.getActionManager().saveMove(modalMove);
        }

        int cost = boughtCards.size() * 3;
        if (currentPlayer.canSpendMC(cost)) {
            currentPlayer.getHand().addAll(boughtCards);
        }

        String myPlayerName = ApplicationConfiguration.getInstance().getMyPlayerName();
        if (currentPlayer.getName().equals(myPlayerName)) {
            gameManager.hasMoreDraftPlayers();
        }

        researchPlayerIndex++;

        Platform.runLater(this::showScreenForNextPlayer);
    }
}