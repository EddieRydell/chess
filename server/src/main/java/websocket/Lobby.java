package websocket;

import org.eclipse.jetty.websocket.api.Session;
import chess.ChessGame;

import java.util.*;

public class Lobby {
    public record Client(Session session,
                         String username,
                         int gameId,
                         ChessGame.TeamColor color) { }

    private static final Map<Integer, Set<Client>> rooms = new HashMap<>();

    public static void add(Client c) {
        rooms.computeIfAbsent(c.gameId(), g -> new HashSet<>()).add(c);
    }
    public static void remove(Session s) {
        rooms.values().forEach(set -> set.removeIf(c -> c.session().equals(s)));
    }
    public static Set<Client> clientsIn(int gameId) {
        return rooms.getOrDefault(gameId, Set.of());
    }
}
