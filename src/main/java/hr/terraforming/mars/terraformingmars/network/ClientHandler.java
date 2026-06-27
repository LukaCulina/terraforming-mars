package hr.terraforming.mars.terraformingmars.network;

import hr.terraforming.mars.terraformingmars.exception.NetworkException;
import hr.terraforming.mars.terraformingmars.manager.ActionManager;
import hr.terraforming.mars.terraformingmars.model.ApplicationConfiguration;
import hr.terraforming.mars.terraformingmars.model.GameManager;
import hr.terraforming.mars.terraformingmars.model.GameMove;
import hr.terraforming.mars.terraformingmars.model.GameState;
import hr.terraforming.mars.terraformingmars.network.message.CardChoiceMessage;
import hr.terraforming.mars.terraformingmars.network.message.CorporationChoiceMessage;
import hr.terraforming.mars.terraformingmars.network.message.PlayerNameMessage;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ClientHandler implements Runnable {
    private final Socket socket;
    private final GameManager gameManager;
    private final CountDownLatch readyLatch = new CountDownLatch(1);
    private ObjectOutputStream clientOutput;
    private ObjectInputStream clientInput;
    private volatile boolean isClientReady = false;
    private volatile boolean isServerRunning = true;
    @Getter
    private String playerName;
    private ServerMessageHandler messageHandler;

    public ClientHandler(Socket socket, GameManager gameManager, ActionManager actionManager) {
        this.socket = socket;
        this.gameManager = gameManager;
        setActionManager(actionManager);
    }

    public void setActionManager(ActionManager actionManager) {
        messageHandler = new ServerMessageHandler(
                gameManager,
                actionManager,
                this::broadcastIfAvailable
        );
    }

    @Override
    public void run() {
        try (ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream())) {

            clientOutput = outputStream;
            clientInput = inputStream;
            isClientReady = true;
            readyLatch.countDown();

            while (isServerRunning && !socket.isClosed()) {
                Object obj = inputStream.readObject();
                handleMessage(obj);
            }
        } catch (EOFException _) {
            log.info("Client {} disconnected gracefully.", playerName != null ? playerName : "Unknown");
        } catch (IOException | ClassNotFoundException e) {
            if (isServerRunning) {
                throw new NetworkException("Client handler connection error", e);
            } else {
                log.info("Client disconnected");
            }
        } finally {
            cleanup();
        }
    }

    private void handleMessage(Object obj) {
        switch (obj) {
            case PlayerNameMessage msg -> playerName = messageHandler.handlePlayerName(msg);
            case CorporationChoiceMessage msg -> messageHandler.handleCorporationChoice(playerName, msg);
            case CardChoiceMessage msg -> messageHandler.handleCardChoice(playerName, msg);
            case GameMove move -> messageHandler.handleGameMove(move);
            default -> log.warn("Unknown message type: {}", obj.getClass());
        }
    }

    private void broadcastIfAvailable() {
        NetworkBroadcaster broadcaster = ApplicationConfiguration.getInstance().getBroadcaster();

        if (broadcaster != null) {
            broadcaster.broadcast();
        }
    }

    public synchronized void sendGameState(GameState state) {
        log.debug("Sending GameState to {}", playerName);

        try {
            boolean ready = readyLatch.await(2, TimeUnit.SECONDS);
            if (!ready) {
                log.warn("ClientHandler not ready after 2 seconds, skipping send");
                return;
            }
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
            return;
        }

        try {
            clientOutput.writeObject(state);
            clientOutput.reset();
            clientOutput.flush();
        } catch (IOException e) {
            cleanup();
            throw new NetworkException("Failed to send GameState to " + playerName, e);
        }
    }

    public synchronized void sendObject(Object message) {
        if (!isClientReady) return;

        try {
            clientOutput.writeObject(message);
            clientOutput.reset();
            clientOutput.flush();
            log.debug("Sent object of type {} to {}", message.getClass().getSimpleName(), playerName);
        } catch (IOException e) {
            cleanup();
            throw new NetworkException("Failed to send " + message.getClass().getSimpleName() + " to " + playerName, e);
        }
    }

    public void close() {
        cleanup();
    }

    private void cleanup() {
        isServerRunning = false;
        isClientReady = false;
        try {
            if (clientInput != null) clientInput.close();
            if (clientOutput != null) clientOutput.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            throw new NetworkException("Error closing client resources for " + playerName, e);
        }
    }
}
