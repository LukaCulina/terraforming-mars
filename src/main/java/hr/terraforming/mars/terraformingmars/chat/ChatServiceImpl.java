package hr.terraforming.mars.terraformingmars.chat;

import java.rmi.RemoteException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatServiceImpl implements ChatService {

    private final List<String> chatHistory = new CopyOnWriteArrayList<>();

    @Override
    public void sendChatMessage(String chatMessage) throws RemoteException {
        chatHistory.add(chatMessage);
    }

    @Override
    public List<String> returnChatHistory() throws RemoteException {
        return chatHistory;
    }

    @Override
    public void clearChatHistory() throws RemoteException {
        chatHistory.clear();
    }
}
