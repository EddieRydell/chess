package ui;

import com.google.gson.Gson;
import websocket.commands.UserGameCommand;
import websocket.messages.ServerMessage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;

public class WebSocketCommunicator {
    private final ServerMessageObserver observer;
    private final String serverUrl;
    private WebSocket webSocket;
    private final Gson gson = new Gson();

    public WebSocketCommunicator(String serverUrl, ServerMessageObserver observer) {
        this.serverUrl = serverUrl;
        this.observer = observer;
    }

    public void connect() {
        CountDownLatch latch = new CountDownLatch(1);
        HttpClient client = HttpClient.newHttpClient();
        client.newWebSocketBuilder()
                .buildAsync(URI.create(serverUrl + "/ws"), new WebSocket.Listener() {
                    @Override
                    public void onOpen(WebSocket webSocket) {
                        WebSocketCommunicator.this.webSocket = webSocket;
                        webSocket.request(1);
                        latch.countDown();
                    }
                    @Override
                    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                        String json = data.toString();
                        ServerMessage message = gson.fromJson(json, ServerMessage.class);
                        observer.onServerMessage(message);
                        webSocket.request(1);
                        return null;
                    }
                });
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void sendCommand(UserGameCommand command) {
        String json = gson.toJson(command);
        if (webSocket != null) {
            webSocket.sendText(json, true);
        }
    }

    public void sendConnectCommand(String authToken, int gameID) {
        UserGameCommand command = new UserGameCommand(UserGameCommand.CommandType.CONNECT, authToken, gameID);
        sendCommand(command);
    }

    public void sendMakeMoveCommand(String authToken, int gameID, Object move) {
        MakeMoveCommand command = new MakeMoveCommand(authToken, gameID, move);
        String json = gson.toJson(command);
        if (webSocket != null) {
            webSocket.sendText(json, true);
        }
    }

    public void sendResignCommand(String authToken, int gameID) {
        UserGameCommand command = new UserGameCommand(UserGameCommand.CommandType.RESIGN, authToken, gameID);
        sendCommand(command);
    }

    public void sendLeaveCommand(String authToken, int gameID) {
        UserGameCommand command = new UserGameCommand(UserGameCommand.CommandType.LEAVE, authToken, gameID);
        sendCommand(command);
    }

    public static class MakeMoveCommand extends UserGameCommand {
        private Object move;

        public MakeMoveCommand(String authToken, int gameID, Object move) {
            super(CommandType.MAKE_MOVE, authToken, gameID);
            this.move = move;
        }

        public Object getMove() {
            return move;
        }
    }
}
