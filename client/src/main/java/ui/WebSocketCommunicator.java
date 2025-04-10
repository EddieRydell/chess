package ui;

import com.google.gson.Gson;
import org.glassfish.tyrus.client.ClientManager;
import websocket.commands.UserGameCommand;
import websocket.messages.ServerMessage;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.Session;
import java.net.URI;
import java.util.concurrent.CountDownLatch;

import javax.websocket.*;

@ClientEndpoint
public class WebSocketCommunicator {
    private Session session;
    private static ServerMessageObserver observer;
    private final String serverUrl; // e.g., "http://localhost:4567"
    private final Gson gson = new Gson();
    private CountDownLatch latch = new CountDownLatch(1);

    public WebSocketCommunicator(String serverUrl, ServerMessageObserver observer) {
        this.serverUrl = serverUrl;
        WebSocketCommunicator.observer = observer;
    }

    public void connect() {
        try {
            ClientManager client = ClientManager.createClient();
            String wsUrl = serverUrl.replace("http://", "ws://") + "/ws";
            session = client.connectToServer(
                    WebSocketCommunicator.class,
                    URI.create(wsUrl)
            );
            latch.await(); // Wait until onOpen is called.
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        latch.countDown();
    }

    @OnMessage
    public void onMessage(String message) {
        // Deserialize JSON message to a ServerMessage object and pass it to the observer.
        ServerMessage serverMessage = gson.fromJson(message, ServerMessage.class);
        observer.onServerMessage(serverMessage);
    }

    @OnError
    public void onError(Session session, Throwable t) {
        System.err.println("WebSocket error: " + t.getMessage());
    }

    public void sendCommand(UserGameCommand command) {
        String json = gson.toJson(command);
        if (session != null && session.isOpen()) {
            session.getAsyncRemote().sendText(json);
        }
    }

    // Convenience methods to send specific commands:

    public void sendConnectCommand(String authToken, int gameID) {
        UserGameCommand command = new UserGameCommand(UserGameCommand.CommandType.CONNECT, authToken, gameID);
        sendCommand(command);
    }

    public void sendMakeMoveCommand(String authToken, int gameID, chess.ChessMove move) {
        MakeMoveCommand command = new MakeMoveCommand(authToken, gameID, move);
        sendCommand(command);
    }

    public void sendResignCommand(String authToken, int gameID) {
        UserGameCommand command = new UserGameCommand(UserGameCommand.CommandType.RESIGN, authToken, gameID);
        sendCommand(command);
    }

    public void sendLeaveCommand(String authToken, int gameID) {
        UserGameCommand command = new UserGameCommand(UserGameCommand.CommandType.LEAVE, authToken, gameID);
        sendCommand(command);
    }

    // An inner class to extend UserGameCommand with a ChessMove field.
    public static class MakeMoveCommand extends UserGameCommand {
        private final chess.ChessMove move;

        public MakeMoveCommand(String authToken, int gameID, chess.ChessMove move) {
            super(CommandType.MAKE_MOVE, authToken, gameID);
            this.move = move;
        }

        public chess.ChessMove getMove() {
            return move;
        }
    }
}
