package websocket;

import org.eclipse.jetty.websocket.api.Session;
import chess.ChessGame;

import java.util.*;

public class Lobby {
    public record Client(Session session,
                         String username,
                         int gameId,
                         ChessGame.TeamColor color) { }

    private static final Map<Integer, Set<Client>> ROOMS = new HashMap<>();

    public static void add(Client c) {
        ROOMS.computeIfAbsent(c.gameId(), g -> new HashSet<>()).add(c);
    }
    public static void remove(Session s) {
        ROOMS.values().forEach(set -> set.removeIf(c -> c.session().equals(s)));
    }
    public static Set<Client> clientsIn(int gameId) {
        return ROOMS.getOrDefault(gameId, Set.of());
    }
}
