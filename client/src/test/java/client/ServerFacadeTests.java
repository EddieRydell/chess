package client;

import org.junit.jupiter.api.*;
import server.Server;
import server.ServerFacade;

import model.AuthData;
import model.GameData;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.MethodName.class)
public class ServerFacadeTests {

    private static Server server;
    private static ServerFacade facade;
    private static int port;

    /**
     * Start the server once before all tests, on a random port (by passing 0 to server.run).
     */
    @BeforeAll
    public static void init() {
        server = new Server();
        port = server.run(0);  // random port
        System.out.println("Started test HTTP server on port " + port);

        // Create a ServerFacade pointing at that port
        facade = new ServerFacade(port);
    }

    /**
     * Stop the server once after all tests.
     */
    @AfterAll
    public static void stopServer() {
        server.stop();
    }

    /**
     * Clear the database before EACH test by calling the "/db" endpoint with DELETE.
     * Adjust if your server uses a different route or approach to clearing.
     */
    @BeforeEach
    public void clearDatabase() throws IOException, URISyntaxException {
        // We'll do a simple HTTP DELETE to /db
        URL url = new URI("http://localhost:" + port + "/db").toURL();
        HttpURLConnection http = (HttpURLConnection) url.openConnection();
        http.setRequestMethod("DELETE");
        http.connect();

        int status = http.getResponseCode();
        if (status < 200 || status >= 300) {
            throw new RuntimeException("Failed to clear DB. HTTP status = " + status);
        }
        http.disconnect();
    }

    // -----------------------------------------------------------------
    // REGISTER
    // -----------------------------------------------------------------

    @Test
    public void testRegister_Success() {
        AuthData auth = facade.register("testuser", "testpass", "test@example.com");
        assertNotNull(auth);
        assertNotNull(auth.authToken());
        assertFalse(auth.authToken().isEmpty(), "Auth token should not be empty");
        // Possibly check username too
        assertEquals("testuser", auth.username());
    }

    @Test
    public void testRegister_Conflict() {
        // Register the same user twice -> second time should fail
        facade.register("sam", "abc", "sam@abc.com");
        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> facade.register("sam", "def", "sam2@abc.com")
        );
        String msg = ex.getMessage().toLowerCase();
        // Check that it indicates user already exists or 400/409 conflict
        assertTrue(msg.contains("already exists") || msg.contains("400") || msg.contains("409"));
    }

    // -----------------------------------------------------------------
    // LOGIN
    // -----------------------------------------------------------------

    @Test
    public void testLogin_Success() {
        facade.register("alice", "password", "alice@test.com");
        AuthData auth = facade.login("alice", "password");
        assertNotNull(auth);
        assertEquals("alice", auth.username());
        assertNotNull(auth.authToken());
    }

    @Test
    public void testLogin_WrongPassword() {
        facade.register("bob", "secret", "bob@test.com");

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> facade.login("bob", "wrongpass")
        );
        String msg = ex.getMessage().toLowerCase();
        // Expect 401 or similar unauthorized
        assertTrue(msg.contains("401") || msg.contains("unauthorized"));
    }

    @Test
    public void testLogin_NoSuchUser() {
        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> facade.login("nosuchuser", "pass")
        );
        String msg = ex.getMessage().toLowerCase();
        assertTrue(msg.contains("401") || msg.contains("unauthorized") || msg.contains("not found"));
    }

    // -----------------------------------------------------------------
    // LOGOUT
    // -----------------------------------------------------------------

    @Test
    public void testLogout_Success() {
        AuthData auth = facade.register("dave", "123", "dave@test.com");
        // No exception means success
        facade.logout(auth.authToken());

        // Attempt an action that should fail after logout
        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> facade.createGame(auth.authToken(), "someGame")
        );
        String msg = ex.getMessage().toLowerCase();
        assertTrue(msg.contains("401") || msg.contains("unauthorized"));
    }

    @Test
    public void testLogout_BadToken() {
        // Logging out with an invalid token should either do nothing or error
        // We'll see what your server does
        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> facade.logout("not-a-real-token")
        );
        // Possibly your server is lenient and returns 200, or it may 401
        String msg = ex.getMessage().toLowerCase();
        assertTrue(msg.contains("401") || msg.contains("unauthorized") || msg.contains("error"));
    }

    // -----------------------------------------------------------------
    // CREATE GAME
    // -----------------------------------------------------------------

    @Test
    public void testCreateGame_Success() {
        AuthData auth = facade.register("creator", "password", "creator@test.com");
        // If no exception is thrown, we consider it a success
        facade.createGame(auth.authToken(), "BestGameEver");
        // Optionally, you can check if the game shows up in listGames
        List<GameData> games = facade.listGames(auth.authToken());
        assertFalse(games.isEmpty());
        assertEquals("BestGameEver", games.get(0).gameName());
    }

    @Test
    public void testCreateGame_NoAuth() {
        // Attempt without logging in
        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> facade.createGame("", "NoAuthGame")
        );
        String msg = ex.getMessage().toLowerCase();
        assertTrue(msg.contains("401") || msg.contains("unauthorized"));
    }

    // -----------------------------------------------------------------
    // LIST GAMES
    // -----------------------------------------------------------------

    @Test
    public void testListGames_Empty() {
        AuthData auth = facade.register("lister", "pass", "lister@test.com");
        List<GameData> games = facade.listGames(auth.authToken());
        assertTrue(games.isEmpty(), "No games should be present initially");
    }

    @Test
    public void testListGames_SomeGames() {
        AuthData auth = facade.register("someone", "pass", "someone@test.com");
        // Create a couple of games
        facade.createGame(auth.authToken(), "g1");
        facade.createGame(auth.authToken(), "g2");
        // Now check if they show up
        List<GameData> games = facade.listGames(auth.authToken());
        assertEquals(2, games.size());
        assertEquals("g1", games.get(0).gameName());
        assertEquals("g2", games.get(1).gameName());
    }

    // -----------------------------------------------------------------
    // JOIN GAME
    // -----------------------------------------------------------------

    @Test
    public void testJoinGame_Success() {
        AuthData auth = facade.register("joiner", "pass", "joiner@test.com");
        // create a game
        facade.createGame(auth.authToken(), "JoinableGame");
        // retrieve gameID
        List<GameData> allGames = facade.listGames(auth.authToken());
        assertFalse(allGames.isEmpty());
        int newGameId = allGames.get(0).gameID();

        // join as white
        facade.joinGame(auth.authToken(), newGameId, "white");

        // get the game to verify
        GameData game = facade.getGame(auth.authToken(), newGameId);
        assertEquals("joiner", game.whiteUsername());
        assertNull(game.blackUsername()); // no black joined yet
    }

    @Test
    public void testJoinGame_InvalidToken() {
        // no login
        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> facade.joinGame("fakeToken", 999, "white")
        );
        String msg = ex.getMessage().toLowerCase();
        assertTrue(msg.contains("401") || msg.contains("unauthorized"));
    }

    @Test
    public void testJoinGame_BadColor() {
        AuthData auth = facade.register("joiner2", "pass", "joiner2@test.com");
        facade.createGame(auth.authToken(), "BadColorGame");
        var games = facade.listGames(auth.authToken());
        int gameId = games.get(0).gameID();

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> facade.joinGame(auth.authToken(), gameId, "green")
        );
        String msg = ex.getMessage().toLowerCase();
        assertTrue(msg.contains("400") || msg.contains("bad request") || msg.contains("invalid"));
    }

    // -----------------------------------------------------------------
    // OBSERVE GAME
    // -----------------------------------------------------------------

    @Test
    public void testObserveGame_Success() {
        AuthData auth = facade.register("obs", "pass", "obs@test.com");
        facade.createGame(auth.authToken(), "ObserveGame");
        List<GameData> games = facade.listGames(auth.authToken());
        int gameId = games.get(0).gameID();

        // "Observe" basically calls getGame
        GameData data = facade.observeGame(auth.authToken(), gameId);
        assertEquals("ObserveGame", data.gameName());
    }

    @Test
    public void testObserveGame_NotFound() {
        AuthData auth = facade.register("obs2", "pass", "obs2@test.com");

        // Observing game 999 (nonexistent) should fail
        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> facade.observeGame(auth.authToken(), 999)
        );
        String msg = ex.getMessage().toLowerCase();
        assertTrue(msg.contains("404") || msg.contains("not found") || msg.contains("bad request"));
    }

}
