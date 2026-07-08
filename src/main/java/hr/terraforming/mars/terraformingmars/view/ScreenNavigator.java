package hr.terraforming.mars.terraformingmars.view;

import hr.terraforming.mars.terraformingmars.controller.game.ChooseCardsController;
import hr.terraforming.mars.terraformingmars.controller.game.ChooseCorporationController;
import hr.terraforming.mars.terraformingmars.controller.game.GameOverController;
import hr.terraforming.mars.terraformingmars.controller.game.GameScreenController;
import hr.terraforming.mars.terraformingmars.controller.setup.*;
import hr.terraforming.mars.terraformingmars.model.*;
import hr.terraforming.mars.terraformingmars.util.GameMoveUtils;
import hr.terraforming.mars.terraformingmars.util.ScreenUtils;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.function.Consumer;

@Slf4j
public class ScreenNavigator {

    private static final int STARTING_CARDS = 6;
    private static final int CARD_COST = 3;
    @Getter
    @Setter
    private static Stage mainStage;

    private ScreenNavigator() {
        throw new IllegalStateException("Utility class");
    }

    public static void showStartMenu() {
        ScreenUtils.showAsScreen(
                mainStage,
                FxmlPaths.START_MENU,
                "Terraforming Mars - Main Menu",
                (StartMenuController _) -> {
                }
        );

        log.info("Main Menu displayed.");
    }

    public static void showChooseModeScreen() {
        ScreenUtils.showAsScreen(
                mainStage,
                FxmlPaths.CHOOSE_MODE,
                "Choose Mode",
                (ChooseModeController _) -> {
                }
        );
    }

    public static void showChooseOnlineModeScreen() {
        ScreenUtils.showAsScreen(
                mainStage,
                FxmlPaths.CHOOSE_ONLINE_MODE,
                "Choose Online Mode",
                (ChooseOnlineModeController _) -> {
                }
        );
    }

    public static void showJoinGameScreen() {
        ScreenUtils.showAsScreen(
                mainStage,
                FxmlPaths.JOIN_GAME,
                "Join Game",
                (JoinGameController _) -> {
                }
        );
    }

    public static void showChooseNameScreen(GameManager gameManager, GameBoard gameBoard) {
        ScreenUtils.showAsScreen(
                mainStage,
                FxmlPaths.CHOOSE_NAME,
                "Choose a name",
                (ChooseNameController controller) -> controller.setup(gameManager, gameBoard)
        );
    }

    public static void showWaitingScreen(GameManager gameManager, int expectedPlayerCount) {
        ScreenUtils.showAsScreen(
                mainStage,
                FxmlPaths.WAITING,
                "Waiting for Players",
                (WaitingController controller) -> controller.setup(gameManager, expectedPlayerCount)
        );
    }

    public static void showChoosePlayersScreen() {
        ScreenUtils.showAsScreen(mainStage, FxmlPaths.CHOOSE_PLAYERS, "Choose the number of players", (ChoosePlayersController _) -> {
        });
    }

    public static void showChooseCorporationScreen(GameManager gameManager) {
        Player currentPlayer = gameManager.getCurrentPlayer();
        List<Corporation> offer = gameManager.getCorporationOffer();
        showChooseCorporationScreen(currentPlayer, offer, gameManager);
    }

    public static void showChooseCorporationScreen(Player player, List<Corporation> offer, GameManager gameManager) {
        ScreenUtils.showAsScreen(
                mainStage,
                FxmlPaths.CHOOSE_CORPORATION,
                "Choose for " + player.getName(),
                (ChooseCorporationController c) -> c.setCorporationOptions(player, offer, gameManager)
        );
    }

    public static void showInitialCardDraftScreen(GameManager gameManager) {
        Player currentPlayer = gameManager.getCurrentDraftPlayer();
        List<Card> offer = gameManager.drawCards(STARTING_CARDS);
        showInitialCardDraftScreen(currentPlayer, offer, gameManager);
    }

    public static void showInitialCardDraftScreen(Player player, List<Card> offer, GameManager gameManager) {
        Consumer<List<Card>> onConfirmAction =
                chosenCards -> handleCardDraftConfirmation(chosenCards, player, gameManager);

        ScreenUtils.showAsScreen(
                mainStage,
                FxmlPaths.CHOOSE_CARDS,
                "Choose Initial Cards - " + player.getName(),
                (ChooseCardsController c) -> c.setup(player, offer, onConfirmAction, gameManager, false)
        );
    }

    private static void handleCardDraftConfirmation(
            List<Card> chosenCards,
            Player currentPlayer,
            GameManager gameManager) {

        int cost = chosenCards.size() * CARD_COST;
        currentPlayer.canSpendMC(cost);
        currentPlayer.getHand().addAll(chosenCards);

        if (gameManager.hasMoreDraftPlayers()) {
            showInitialCardDraftScreen(gameManager);
        } else {
            GameMoveUtils.saveInitialSetupMove(gameManager);
            GameState gameState = new GameState(gameManager, gameManager.getGameBoard());
            startGameWithChosenCards(gameState);
        }
    }

    public static void startGameWithChosenCards(GameState gameState) {
        var result = ScreenUtils.loadFxml(FxmlPaths.GAME_SCREEN);
        GameScreenController gameScreenController = (GameScreenController) result.controller();
        Scene mainGameScene = ScreenUtils.createScene(result.root());
        ApplicationConfiguration.getInstance().setActiveGameController(gameScreenController);

        gameScreenController.setupGame(gameState);

        mainStage.setScene(mainGameScene);
        mainStage.setTitle("Terraforming Mars");
    }

    public static void showGameOverScreen(List<Player> rankedPlayers) {
        ScreenUtils.showAsScreen(
                mainStage,
                FxmlPaths.GAME_OVER,
                "Game Over - Final Score",
                (GameOverController c) -> c.setFinalScores(rankedPlayers));
    }

    public static void showGameScreen(GameScreenController controller) {
        if (controller == null || controller.getGameBoardPane() == null) {
            log.error("Cannot show GameScreen - controller or scene is null!");
            return;
        }

        Scene gameScene = controller.getGameBoardPane().getScene();

        if (gameScene == null) {
            log.error("Cannot show GameScreen - scene is null!");
            return;
        }

        mainStage.setScene(gameScene);
        mainStage.setTitle("Terraforming Mars - Replay");

        log.info("GameScreen displayed for replay");
    }
}