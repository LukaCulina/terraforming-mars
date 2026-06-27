package hr.terraforming.mars.terraformingmars.network;

import hr.terraforming.mars.terraformingmars.exception.NetworkException;
import hr.terraforming.mars.terraformingmars.model.Card;
import hr.terraforming.mars.terraformingmars.model.GameMove;
import hr.terraforming.mars.terraformingmars.model.GameState;
import hr.terraforming.mars.terraformingmars.network.message.CardChoiceMessage;
import hr.terraforming.mars.terraformingmars.network.message.CorporationChoiceMessage;
import hr.terraforming.mars.terraformingmars.network.message.PlayerNameMessage;
import lombok.extern.slf4j.Slf4j;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class GameClientThread implements Runnable {
    private final String hostname;
    private final int port;
    private final List<GameStateListener> listeners = new ArrayList<>();
    private final ClientMessageDispatcher messageDispatcher;
    private Socket clientSocket;
    private ObjectOutputStream serverOutput;
    private volatile boolean running = true;
    private GameState lastGameState;

    public GameClientThread(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
        this.messageDispatcher = new ClientMessageDispatcher(this, listeners);
    }

    @Override
    public void run() {
        try (Socket socket = new Socket(hostname, port);
             ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream())) {

            this.clientSocket = socket;
            this.serverOutput = outputStream;

            log.info("Connected to server at {}:{}", hostname, port);

            while (running) {
                Object receivedMessage = inputStream.readObject();
                processMessage(receivedMessage);
            }

        } catch (EOFException _) {
            log.info("Server closed connection");
        } catch (IOException | ClassNotFoundException e) {
            handleConnectionError(e);
        }
    }

    private void processMessage(Object message) {
        if (message instanceof GameState state) {
            lastGameState = state;
        }

        messageDispatcher.dispatch(message, lastGameState);
    }

    private void handleConnectionError(Exception e) {
        if (running) {
            throw new NetworkException("Failed to connect to server at " + hostname + ":" + port, e);
        } else {
            log.info("Client disconnected");
        }
    }

    public void addGameStateListener(GameStateListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    public void sendCardChoice(List<Card> selectedCards) {
        sendMessage(
                new CardChoiceMessage(selectedCards.stream().map(Card::getName).toList()),
                () -> log.debug("Sent card choice with {} cards", selectedCards.size())
        );
    }

    public void sendPlayerName(String playerName) {
        sendMessage(
                new PlayerNameMessage(playerName),
                () -> log.debug("Sent player name to server: {}", playerName)
        );
    }

    public void sendCorporationChoice(String corporationName) {
        sendMessage(
                new CorporationChoiceMessage(corporationName),
                () -> log.debug("Sent corporation choice to server: {}", corporationName)
        );
    }

    public void sendMove(GameMove move) {
        sendMessage(move, null);
    }

    private synchronized void sendMessage(Object message, Runnable onSuccess) {
        try {
            if (serverOutput != null) {
                serverOutput.writeObject(message);
                serverOutput.flush();
                if (onSuccess != null) {
                    onSuccess.run();
                }
            } else {
                throw new NetworkException("Cannot send message: server output stream is null");
            }
        } catch (IOException e) {
            throw new NetworkException("Failed to send " + message.getClass().getSimpleName() + " to server", e);
        }
    }

    public void shutdown() {
        log.info("GameClientThread shutting down");
        running = false;

        closeSocket();
        clearState();
    }

    private void closeSocket() {
        try {
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
        } catch (IOException e) {
            throw new NetworkException("Error closing client socket", e);
        }
    }

    private void clearState() {
        synchronized (listeners) {
            listeners.clear();
        }

        lastGameState = null;
        serverOutput = null;
    }
}
