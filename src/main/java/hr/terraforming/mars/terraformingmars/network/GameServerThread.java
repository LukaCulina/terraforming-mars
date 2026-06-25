package hr.terraforming.mars.terraformingmars.network;

import hr.terraforming.mars.terraformingmars.exception.ConfigurationException;
import hr.terraforming.mars.terraformingmars.exception.NetworkException;
import hr.terraforming.mars.terraformingmars.jndi.ConfigurationKey;
import hr.terraforming.mars.terraformingmars.jndi.ConfigurationReader;
import hr.terraforming.mars.terraformingmars.manager.ActionManager;
import hr.terraforming.mars.terraformingmars.model.GameBoard;
import hr.terraforming.mars.terraformingmars.model.GameManager;
import hr.terraforming.mars.terraformingmars.model.GameState;
import javafx.application.Platform;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

@Slf4j
public class GameServerThread implements Runnable {

    private final GameManager gameManager;
    private final GameBoard gameBoard;
    private final int maxPlayers;
    private final List<ClientHandler> connectedClients = new CopyOnWriteArrayList<>();
    private final List<GameStateListener> localListeners = new CopyOnWriteArrayList<>();
    private ActionManager actionManager;
    @Setter
    private Consumer<Integer> onPlayerCountChanged;
    private ServerSocket serverSocket;
    private CardDistributor cardDistributor;
    private volatile boolean running = true;

    public GameServerThread(GameManager gameManager, GameBoard gameBoard, ActionManager actionManager, int maxPlayers) {
        this.gameManager = gameManager;
        this.gameBoard = gameBoard;
        this.actionManager = actionManager;
        this.maxPlayers = maxPlayers;
    }

    @Override
    public void run() {
        int port;

        try {
            port = ConfigurationReader.getIntegerValue(ConfigurationKey.SERVER_PORT);
        } catch (Exception e) {
            throw new ConfigurationException("Failed to read server port from configuration", e);
        }

        try {
            serverSocket = new ServerSocket(port);
            log.info("Server started on port {}, waiting for {} players", port, maxPlayers - 1);

            while (running && connectedClients.size() < maxPlayers - 1) {
                Socket clientSocket = serverSocket.accept();
                log.info("Client connected: {}", clientSocket.getInetAddress());

                ClientHandler handler = new ClientHandler(clientSocket, gameManager, actionManager);
                connectedClients.add(handler);
                new Thread(handler).start();

                if (onPlayerCountChanged != null) {
                    Platform.runLater(() -> onPlayerCountChanged.accept(connectedClients.size()));
                }
                broadcastGameState(new GameState(gameManager, gameBoard));
            }
            log.info("All players connected, game can start!");

        } catch (SocketException e) {
            if (!running) {
                log.info("Server successfully shut down (Socket closed deliberately).");
            } else {
                log.error("Socket error unexpectedly: ", e);
            }
        } catch (IOException e) {
            if (running) {
                throw new NetworkException("Failed to start server on port " + port, e);
            } else {
                log.info("Server stopped");
            }
        }
    }

    public void sendToPlayer(String playerName, Object message) {
        for (ClientHandler client : connectedClients) {
            if (playerName.equals(client.getPlayerName())) {
                client.sendObject(message);
                return;
            }
        }
        log.warn("Cannot send message to player {}, client not found.", playerName);
    }

    public void distributeInitialCorporations() {
        ensureDistributor();
        cardDistributor.distributeInitialCorporations();
    }

    public void distributeInitialCards() {
        ensureDistributor();
        cardDistributor.distributeInitialCards();
    }

    public void distributeResearchCards() {
        ensureDistributor();
        cardDistributor.distributeResearchCards();
    }

    private synchronized void ensureDistributor() {
        if (cardDistributor == null) {
            this.cardDistributor = new CardDistributor(gameManager, this, actionManager);
        }
    }

    public void setActionManager(ActionManager actionManager) {
        this.actionManager = actionManager;
        log.debug("ActionManager injected into GameServerThread");
        for (ClientHandler client : connectedClients) {
            client.setActionManager(actionManager);
        }
        this.cardDistributor = new CardDistributor(gameManager, this, actionManager);
    }

    public void addLocalListener(GameStateListener listener) {
        if (listener != null) {
            this.localListeners.add(listener);
        }
    }

    public void broadcastGameState(GameState state) {
        log.debug("Broadcasting to {} clients.", connectedClients.size());
        for (ClientHandler client : connectedClients) {
            client.sendGameState(state);
        }
        for (GameStateListener listener : localListeners) {
            Platform.runLater(() -> listener.onGameStateReceived(state));
        }
    }

    public void broadcastToAll(Object message) {
        log.debug("Broadcasting {} to all {} clients",
                message.getClass().getSimpleName(), connectedClients.size());
        for (ClientHandler client : connectedClients) {
            client.sendObject(message);
        }
    }

    public void shutdown() {
        log.info("GameServerThread shutting down");
        running = false;

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }

            for (ClientHandler client : connectedClients) {
                client.close();
            }

            connectedClients.clear();
            localListeners.clear();
            cardDistributor = null;
        } catch (IOException e) {
            throw new NetworkException("Failed to shutdown server cleanly", e);
        }
    }
}
