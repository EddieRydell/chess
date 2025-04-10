package websocket;

import chess.ChessGame;
import chess.ChessMove;
import com.google.gson.Gson;
import dataaccess.DataAccess;
import dataaccess.DBDataAccess;
import dataaccess.DataAccessException;
import model.AuthData;
import model.GameData;
import service.GameService;
import service.UserService;
import websocket.commands.UserGameCommand;
import websocket.messages.ServerMessage;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;

@WebSocket
public class GameWebSocketHandler {

    private static final Gson GSON = new Gson();
    private static final DataAccess DAO;
    private static final GameService GAME_SERVICE;
    private static final UserService USER_SERVICE;

    static {
        try {
            DAO = new DBDataAccess();
        } catch (DataAccessException e) { throw new RuntimeException(e); }
        GAME_SERVICE = new GameService(DAO);
        USER_SERVICE = new UserService(DAO);
    }

    /* ---------------------------------------------------- */

    @OnWebSocketConnect
    public void onConnect(Session session) { /* nothing yet */ }

    @OnWebSocketClose
    public void onClose(Session session, int status, String reason) {
        Lobby.remove(session);   // no notification for close itself
    }

    @OnWebSocketError
    public void onError(Session s, Throwable err) { err.printStackTrace(); }

    /* ------------- main dispatcher ---------------------- */

    @OnWebSocketMessage
    public void onMessage(Session session, String json) {
        try {
            UserGameCommand cmd = GSON.fromJson(json, UserGameCommand.class);
            switch (cmd.getCommandType()) {
                case CONNECT   -> handleConnect(session, cmd);
                case MAKE_MOVE -> handleMove(session, cmd);
                case LEAVE     -> handleLeave(session, cmd);
                case RESIGN    -> handleResign(session, cmd);
            }
        } catch (Exception ex) {     // any unexpected exception
            send(session, ServerMessage.error(ex.getMessage()));
        }
    }

    /* ------------- command handlers --------------------- */

    private void handleConnect(Session s, UserGameCommand cmd) throws DataAccessException {
        AuthData auth = DAO.getAuth(cmd.getAuthToken());
        GameData game = DAO.getGame(cmd.getGameID());

        if (auth == null || game == null) {
            send(s, ServerMessage.error("invalid auth or game id"));
            return;
        }

        // determine color / observer
        ChessGame.TeamColor color = null;
        if (auth.username().equals(game.whiteUsername())) color = ChessGame.TeamColor.WHITE;
        else if (auth.username().equals(game.blackUsername())) color = ChessGame.TeamColor.BLACK;

        Lobby.Client me = new Lobby.Client(s, auth.username(), game.gameID(), color);
        Lobby.add(me);

        // 1) send LOAD_GAME to root client
        send(s, ServerMessage.loadGame(game.game()));

        // 2) send NOTIFICATION to all *other* clients in game
        String note = (color == null)
                ? auth.username() + " connected as an observer"
                : auth.username() + " connected as " + color;
        broadcastExcept(game.gameID(), s, ServerMessage.notification(note));
    }

    private void handleMove(Session s, UserGameCommand cmd) throws DataAccessException {
        AuthData auth = DAO.getAuth(cmd.getAuthToken());
        GameData gameData = DAO.getGame(cmd.getGameID());
        if (auth == null || gameData == null) {
            send(s, ServerMessage.error("invalid auth or game id"));
            return;
        }

        ChessMove move = cmd.getMove();          // you added getMove() in subclass
        try {
            gameData.game().makeMove(move);      // throws if illegal / wrong turn
            DAO.updateGame(gameData);            // persist

            // broadcast LOAD_GAME to everyone
            ServerMessage load = ServerMessage.loadGame(gameData.game());
            broadcast(gameData.gameID(), load);

            // notify others (not root)
            String desc = auth.username() + " moved " +
                    move.getStartPosition() + " -> " + move.getEndPosition();
            broadcastExcept(gameData.gameID(), s, ServerMessage.notification(desc));

            // check / mate notifications
            if (gameData.game().isInCheckmate(gameData.game().getTeamTurn())) {
                broadcast(gameData.gameID(),
                        ServerMessage.notification(auth.username() + " is in checkmate"));
            } else if (gameData.game().isInCheck(gameData.game().getTeamTurn())) {
                broadcast(gameData.gameID(),
                        ServerMessage.notification(auth.username() + " is in check"));
            }

        } catch (Exception ex) {
            send(s, ServerMessage.error("illegal move"));
        }
    }

    private void handleLeave(Session s, UserGameCommand cmd) {
        Lobby.remove(s);
        broadcastExcept(cmd.getGameID(), s,
                ServerMessage.notification("A player left the game"));
    }

    private void handleResign(Session s, UserGameCommand cmd) throws DataAccessException {
        AuthData auth = DAO.getAuth(cmd.getAuthToken());
        GameData game = DAO.getGame(cmd.getGameID());
        if (auth == null || game == null) {
            send(s, ServerMessage.error("invalid auth or game id"));
            return;
        }
        game.game().setGameOver(true);
        DAO.updateGame(game);
        broadcast(game.gameID(),
                ServerMessage.notification(auth.username() + " resigned"));
    }

    private void send(Session s, ServerMessage m) {
        if (s.isOpen()) s.getRemote().sendStringByFuture(GSON.toJson(m));
    }

    private void broadcast(int gameId, ServerMessage m) {
        Lobby.clientsIn(gameId).forEach(c -> send(c.session(), m));
    }

    private void broadcastExcept(int gameId, Session except, ServerMessage m) {
        Lobby.clientsIn(gameId).stream()
                .filter(c -> !c.session().equals(except))
                .forEach(c -> send(c.session(), m));
    }

}
