package server;

import com.google.gson.Gson;
import spark.Session;
import websocket.commands.UserGameCommand;
import websocket.messages.ServerMessage;
import websocket.messages.ServerMessage.ServerMessageType;

import javax.websocket.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@ServerEndpoint(value = "/ws")
public class GameWebSocketHandler {
    private static final Logger logger = Logger.getLogger(GameWebSocketHandler.class.getName());
    private final Gson gson = new Gson();
    private Session session;

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        logger.info("WebSocket opened: " + session.getId());
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        logger.info("Received message: " + message);
        try {
            // Deserialize incoming JSON into a UserGameCommand.
            UserGameCommand command = gson.fromJson(message, UserGameCommand.class);
            ServerMessage response = null;

            switch(command.getCommandType()){
                case CONNECT:
                    // Validate and send a LOAD_GAME message back to the client.
                    response = new ServerMessage(ServerMessageType.LOAD_GAME);
                    // Optionally set the game data via response.setGame(...)
                    break;
                case MAKE_MOVE:
                    // In a complete implementation cast to your MakeMoveCommand to extract the move,
                    // update game state, then create a LOAD_GAME response.
                    response = new ServerMessage(ServerMessageType.LOAD_GAME);
                    break;
                case RESIGN:
                    response = new ServerMessage(ServerMessageType.NOTIFICATION);
                    // Mark game resigned, broadcast notifications to connected clients.
                    break;
                case LEAVE:
                    response = new ServerMessage(ServerMessageType.NOTIFICATION);
                    break;
            }
            if(response != null) {
                sendMessage(session, gson.toJson(response));
                // In a full implementation, broadcast to other sessions in the same game.
            }
        } catch(Exception ex) {
            logger.log(Level.SEVERE, "Error processing message", ex);
            ServerMessage error = new ServerMessage(ServerMessageType.ERROR);
            sendMessage(session, gson.toJson(error));
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        logger.info("Session closed: " + session.getId() + " Reason: " + closeReason.getReasonPhrase());
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        logger.log(Level.SEVERE, "Error in WebSocket session " + session.getId(), throwable);
    }

    private void sendMessage(Session session, String message) {
        try {
            session.getBasicRemote().sendText(message);
        } catch(Exception ex) {
            logger.log(Level.SEVERE, "Failed to send message", ex);
        }
    }
}
