package websocket.messages;

import chess.ChessGame;

import java.util.Objects;

/**
 * Represents a Message the server can send through a WebSocket
 * 
 * Note: You can add to this class, but you should not alter the existing
 * methods.
 */
public class ServerMessage {
    ServerMessageType serverMessageType;

    public enum ServerMessageType {
        LOAD_GAME,
        ERROR,
        NOTIFICATION
    }

    public ServerMessage(ServerMessageType type) {
        this.serverMessageType = type;
    }

    public ServerMessageType getServerMessageType() {
        return this.serverMessageType;
    }

    public ChessGame game;
    public String message;
    public String errorMessage;

    public static ServerMessage loadGame(ChessGame game) {
        ServerMessage m = new ServerMessage(ServerMessageType.LOAD_GAME);
        m.game = game;
        return m;
    }

    public static ServerMessage notification(String msg) {
        ServerMessage m = new ServerMessage(ServerMessageType.NOTIFICATION);
        m.message = msg;
        return m;
    }

    public static ServerMessage error(String msg) {
        ServerMessage m = new ServerMessage(ServerMessageType.ERROR);
        m.errorMessage = msg.contains("error") ? msg : "Error: " + msg;
        return m;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ServerMessage)) {
            return false;
        }
        ServerMessage that = (ServerMessage) o;
        return getServerMessageType() == that.getServerMessageType();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getServerMessageType());
    }
}
