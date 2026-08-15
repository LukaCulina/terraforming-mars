package hr.terraforming.mars.terraformingmars.controller.setup;

import hr.terraforming.mars.terraformingmars.config.ConfigurationKey;
import hr.terraforming.mars.terraformingmars.config.ConfigurationReader;
import hr.terraforming.mars.terraformingmars.model.ApplicationConfiguration;
import hr.terraforming.mars.terraformingmars.network.ClientGameCoordinator;
import hr.terraforming.mars.terraformingmars.network.GameClientThread;
import hr.terraforming.mars.terraformingmars.view.ScreenNavigator;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JoinGameController {

    @FXML
    private TextField playerNameInput;

    @FXML
    private TextField serverIpInput;

    @FXML
    private TextField portInput;

    @FXML
    private Label statusLabel;

    @FXML
    private void initialize() {
        try {
            String defaultHost = ConfigurationReader.getStringValue(ConfigurationKey.HOSTNAME);
            int defaultPort = ConfigurationReader.getIntegerValue(ConfigurationKey.SERVER_PORT);

            serverIpInput.setText(defaultHost);
            portInput.setText(String.valueOf(defaultPort));
        } catch (Exception e) {
            log.warn("Failed to load default configuration", e);
        }
    }

    @FXML
    private void handleConnect() {
        String playerName = playerNameInput.getText().trim();
        String serverIp = serverIpInput.getText().trim();
        String portText = portInput.getText().trim();

        if (playerName.isEmpty()) {
            showStatus("Please enter your name!", true);
            return;
        }

        if (serverIp.isEmpty()) {
            showStatus("Please enter server IP address!", true);
            return;
        }

        int port;

        try {
            port = Integer.parseInt(portText);
        } catch (NumberFormatException _) {
            showStatus("Invalid port number!", true);
            return;
        }

        ApplicationConfiguration config = ApplicationConfiguration.getInstance();
        config.setMyPlayerName(playerName);

        log.info("Attempting to connect to server {}:{} as '{}'", serverIp, port, playerName);
        showStatus("Connecting to server...", false);

        connectToServer(serverIp, port);
    }

    private void connectToServer(String serverIp, int port) {
        new Thread(() -> {
            try {
                GameClientThread client = new GameClientThread(serverIp, port);
                ApplicationConfiguration.getInstance().setGameClient(client);

                ClientGameCoordinator coordinator = new ClientGameCoordinator(client);
                client.addGameStateListener(coordinator);

                new Thread(client).start();

                Platform.runLater(() -> {
                    showStatus("Connected! Waiting for game to start...", false);
                    disableInputs();
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    log.error("Failed to connect to server", e);
                    showStatus("Connection failed: " + e.getMessage(), true);
                });
            }
        }).start();
    }

    private void disableInputs() {
        playerNameInput.setDisable(true);
        serverIpInput.setDisable(true);
        portInput.setDisable(true);
    }

    @FXML
    private void handleBack() {
        ScreenNavigator.showChooseOnlineModeScreen();
    }

    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.getStyleClass().removeAll("status-label-error", "status-label-ok");
        statusLabel.getStyleClass().add(isError ? "status-label-error" : "status-label-ok");
    }
}