package hr.terraforming.mars.terraformingmars.model;

import hr.terraforming.mars.terraformingmars.enums.GamePhase;
import hr.terraforming.mars.terraformingmars.exception.GameStateException;
import hr.terraforming.mars.terraformingmars.manager.TurnManager;
import hr.terraforming.mars.terraformingmars.service.DeckService;
import hr.terraforming.mars.terraformingmars.service.ProductionService;
import hr.terraforming.mars.terraformingmars.service.ScoringService;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
public class GameManager implements Serializable {

    private final List<Player> players;
    private final TurnManager turnManager;
    private final DeckService deckService;
    private transient GameBoard board;

    private int cardDraftPlayerIndex = 0;

    @Getter
    private int generation = 0;

    @Setter
    @Getter
    private GamePhase currentPhase;

    public GameManager(List<Player> players, GameBoard gameBoard) {
        this.players = new ArrayList<>(players);
        this.turnManager = new TurnManager(players);
        this.deckService = new DeckService();
        relink(gameBoard);
    }

    public void startGame() {
        currentPhase = GamePhase.ACTIONS;
        generation = 1;
        turnManager.reset();
        log.info("Starting Generation {}. Phase: {}", generation, currentPhase);
    }

    public void startNewGeneration() {
        generation++;
        currentPhase = GamePhase.RESEARCH;
        turnManager.reset();
    }

    public void beginActionPhase() {
        currentPhase = GamePhase.ACTIONS;
        turnManager.beginActionPhase();
    }

    public void doProduction() {
        ProductionService.executeProduction(players);
    }

    public boolean passTurn() {
        return turnManager.passTurn();
    }

    public void incrementActionsTaken() {
        turnManager.incrementActionsTaken();
    }

    public int getActionsTakenThisTurn() {
        return turnManager.getActionsTakenThisTurn();
    }

    public void rotateFirstPlayer() {
        turnManager.rotateFirstPlayer();
    }

    public void resetForNewGame(GameBoard newBoard) {
        board = newBoard;

        for (Player player : players) {
            player.setBoard(newBoard);
        }

        generation = 1;
        currentPhase = GamePhase.ACTIONS;
        cardDraftPlayerIndex = 0;
        turnManager.reset();
        deckService.reset();
    }

    public void relink(GameBoard gameBoard) {
        this.board = gameBoard;
        for (Player player : players) {
            player.setBoard(gameBoard);
        }
    }

    public void shuffleCorporations() {
        deckService.shuffleCorporations();
    }

    public void shuffleCards() {
        deckService.shuffleCards();
    }

    public List<Corporation> drawCorporations(int count) {
        return deckService.drawCorporations(count);
    }

    public List<Corporation> getCorporationOffer() {
        return deckService.getInitialCorporations();
    }

    public List<Card> drawCards(int count) {
        return deckService.drawCards(count);
    }

    public void assignCorporationAndAdvance(Corporation chosenCorp) {
        getCurrentPlayer().setCorporation(chosenCorp);
        turnManager.advanceCorporationDraft();
    }

    public Player getCurrentDraftPlayer() {
        if (cardDraftPlayerIndex >= players.size()) {
            return null;
        }

        return players.get(cardDraftPlayerIndex);
    }

    public boolean hasMoreDraftPlayers() {
        cardDraftPlayerIndex++;
        return cardDraftPlayerIndex < players.size();
    }

    public void resetDraftPhase() {
        cardDraftPlayerIndex = 0;
    }

    public Player getCurrentPlayer() {
        return turnManager.getCurrentPlayer();
    }

    public Player getFirstPlayer() {
        return turnManager.getFirstPlayer();
    }

    public List<Player> getPlayers() {
        return Collections.unmodifiableList(players);
    }

    public Player getPlayerByName(String playerName) {
        return players.stream()
                .filter(p -> p.getName().equals(playerName))
                .findFirst()
                .orElseThrow(() -> new GameStateException(
                        "Player '" + playerName + "' not found"
                ));
    }

    public void setCurrentPlayerByName(String playerName) {
        turnManager.setCurrentPlayerByName(playerName);
    }

    public GameBoard getGameBoard() {
        return board;
    }

    public List<Player> calculateFinalScores() {
        return ScoringService.calculateFinalScores(players, getGameBoard());
    }
}