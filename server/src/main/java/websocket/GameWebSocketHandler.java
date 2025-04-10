package websocket;

import com.google.gson.Gson;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;

import websocket.commands.UserGameCommand;
import websocket.messages.ServerMessage;
import websocket.messages.ServerMessage.ServerMessageType;

import java.io.IOException;

@WebSocket
public class GameWebSocketHandler {

    static {
        org.eclipse.jetty.util.log.Log.setLog(new org.eclipse.jetty.util.log.StdErrLog());
        ((org.eclipse.jetty.util.log.StdErrLog)
                org.eclipse.jetty.util.log.Log.getRootLogger()).setLevel(org.eclipse.jetty.util.log.StdErrLog.LEVEL_DEBUG);
    }


    private static final Gson GSON = new Gson();

    @OnWebSocketConnect
    public void onConnect(Session session) {
        System.out.println("WS connect: " + session);
    }

    @OnWebSocketMessage
    public void onMessage(Session session, String msg) {
        try {
            UserGameCommand cmd = GSON.fromJson(msg, UserGameCommand.class);

            ServerMessage reply = switch (cmd.getCommandType()) {
                case CONNECT, MAKE_MOVE -> new ServerMessage(ServerMessageType.LOAD_GAME);
                case RESIGN, LEAVE      -> new ServerMessage(ServerMessageType.NOTIFICATION);
            };

            session.getRemote().sendString(GSON.toJson(reply));
        } catch (Exception ex) {
            ex.printStackTrace();
            try {
                ServerMessage err = new ServerMessage(ServerMessageType.ERROR);
                session.getRemote().sendString(GSON.toJson(err));
            } catch (IOException ignore) { /* nothing else we can do */ }
        }
    }


    @OnWebSocketClose
    public void onClose(Session session, int status, String reason) {
        System.out.println("WS close: " + session + " [" + status + "] " + reason);
    }

    @OnWebSocketError
    public void onError(Session session, Throwable error) {
        error.printStackTrace();
    }
}
