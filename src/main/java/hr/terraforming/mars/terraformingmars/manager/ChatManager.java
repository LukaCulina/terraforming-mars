package hr.terraforming.mars.terraformingmars.manager;

import hr.terraforming.mars.terraformingmars.chat.ChatService;
import hr.terraforming.mars.terraformingmars.chat.ChatServiceImpl;
import hr.terraforming.mars.terraformingmars.config.ConfigurationKey;
import hr.terraforming.mars.terraformingmars.config.ConfigurationReader;
import hr.terraforming.mars.terraformingmars.enums.PlayerType;
import hr.terraforming.mars.terraformingmars.model.ApplicationConfiguration;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import lombok.extern.slf4j.Slf4j;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

@Slf4j
public class ChatManager {
    private final ListView<String> chatListView;
    private final TextField chatInput;
    private final VBox chatBoxContainer;
    private ChatService chatService;

    public ChatManager(ListView<String> chatListView, TextField chatInput, VBox chatBoxContainer) {
        this.chatListView = chatListView;
        this.chatInput = chatInput;
        this.chatBoxContainer = chatBoxContainer;

        setupAutoScroll();
    }

    private void setupAutoScroll() {
        if (chatListView != null) {
            chatListView.getItems().addListener((ListChangeListener<String>) _ -> Platform.runLater(() -> {
                int size = chatListView.getItems().size();
                if (size > 0) {
                    chatListView.scrollTo(size - 1);
                }
            }));
        }
    }

    public void startLocalRmiRegistry() {
        try {
            int rmiPort = ConfigurationReader.getIntegerValue(ConfigurationKey.RMI_PORT);

            Registry registry = getOrCreateRegistry(rmiPort);

            ChatService chatRemoteService = new ChatServiceImpl();
            ChatService skeleton = (ChatService) UnicastRemoteObject.exportObject(chatRemoteService, 0);
            registry.rebind(ChatService.REMOTE_OBJECT_NAME, skeleton);

            log.info("Local RMI Registry for Chat started on port {}", rmiPort);
        } catch (RemoteException e) {
            log.error("Failed to start local RMI registry", e);
        }
    }

    private Registry getOrCreateRegistry(int port) throws RemoteException {
        try {
            return LocateRegistry.createRegistry(port);
        } catch (RemoteException _) {
            return LocateRegistry.getRegistry(port);
        }
    }

    public void setupChatSystem(PlayerType playerType) {
        boolean isOnline = (playerType != PlayerType.LOCAL);

        if (chatBoxContainer != null) {
            chatBoxContainer.setVisible(isOnline);
            chatBoxContainer.setManaged(isOnline);
        }

        if (playerType == PlayerType.HOST) {
            startLocalRmiRegistry();
        }

        if (isOnline) {
            connectToChatService();
        }
    }

    private void connectToChatService() {
        try {
            String hostname = ConfigurationReader.getStringValue(ConfigurationKey.HOSTNAME);
            int rmiPort = ConfigurationReader.getIntegerValue(ConfigurationKey.RMI_PORT);

            Registry registry = LocateRegistry.getRegistry(hostname, rmiPort);
            chatService = (ChatService) registry.lookup(ChatService.REMOTE_OBJECT_NAME);

            log.info("Connected to chat service");
            startChatPolling();

        } catch (Exception e) {
            log.warn("Chat service unavailable (ignoring): {}", e.getMessage());
            chatService = null;
        }
    }

    private void startChatPolling() {
        Timeline chatPoll = new Timeline(new KeyFrame(Duration.seconds(1), _ -> {
            try {
                if (chatService != null) {
                    List<String> messages = chatService.returnChatHistory();
                    Platform.runLater(() -> {
                        if (chatListView != null) {
                            chatListView.getItems().clear();
                            chatListView.getItems().addAll(messages);
                        }
                    });
                }
            } catch (RemoteException e) {
                log.error("Chat polling error", e);
            }
        }));
        chatPoll.setCycleCount(Animation.INDEFINITE);
        chatPoll.play();
    }

    public void sendMessage() {
        if (chatService == null || chatInput == null) return;

        try {
            String message = chatInput.getText();
            if (!message.isEmpty()) {
                String myName = ApplicationConfiguration.getInstance().getMyPlayerName();

                if (myName == null) {
                    myName = "Unknown";
                }

                chatService.sendChatMessage(myName + ": " + message);
                chatInput.clear();
            }
        } catch (RemoteException e) {
            log.error("Failed to send chat message", e);
        }
    }

    public void clearHistory() {
        if (chatService != null) {
            try {
                chatService.clearChatHistory();
                Platform.runLater(() -> {
                    if (chatListView != null) {
                        chatListView.getItems().clear();
                    }
                });
            } catch (RemoteException e) {
                log.error("Failed to clear chat history", e);
            }
        }
    }
}
