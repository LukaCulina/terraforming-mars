package hr.terraforming.mars.terraformingmars.chat;

import hr.terraforming.mars.terraformingmars.jndi.ConfigurationKey;
import hr.terraforming.mars.terraformingmars.jndi.ConfigurationReader;
import lombok.extern.slf4j.Slf4j;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

@Slf4j
public class ChatServer {

    private static final int RANDOM_PORT_HINT = 0;

    static void main() {
        try {
            Registry registry = LocateRegistry.createRegistry(ConfigurationReader.getIntegerValue(
                    ConfigurationKey.RMI_PORT));
            ChatService chatRemoteService = new ChatServiceImpl();
            ChatService skeleton = (ChatService) UnicastRemoteObject.exportObject(chatRemoteService,
                    RANDOM_PORT_HINT);
            registry.rebind(ChatService.REMOTE_OBJECT_NAME, skeleton);
            log.info("Service method called using @Slf4j");
        } catch (RemoteException e) {
            log.error("RemoteException occurred while starting RMI registry and binding ChatService", e);
        }
    }
}
