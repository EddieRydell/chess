package dataaccess;

import chess.ChessGame;
import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import dataaccess.DBDataAccess;
import model.AuthData;
import model.GameData;
import model.UserData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DataAccessTests {

    private static DataAccess dao;

    @BeforeAll
    static void init() throws DataAccessException {
        dao = new DBDataAccess();
    }

    @BeforeEach
    void setUp() throws DataAccessException {
        dao.clear();
        dao.createUser(new UserData("testUser", "password", "test@mail.com"));
        dao.createUser(new UserData("user1", "pass", "user1@mail.com"));
        dao.createUser(new UserData("a", "pass", "a@mail.com"));
        dao.createUser(new UserData("b", "pass", "b@mail.com"));
        dao.createUser(new UserData("c", "pass", "c@mail.com"));
    }

    @AfterEach
    void tearDown() throws DataAccessException {
        dao.clear();
    }

    @Test
    public void testCreateUserPositive() throws DataAccessException {
        UserData user = new UserData("newUser", "password", "newUser@mail.com");
        dao.createUser(user);

        UserData retrieved = dao.getUser("newUser");
        assertNotNull(retrieved);
        assertEquals("newUser", retrieved.username());
        assertEquals("newUser@mail.com", retrieved.email());
    }

    @Test
    public void testCreateUserNegativeDuplicate() throws DataAccessException {
        UserData duplicate = new UserData("testUser", "password", "test@mail.com");
        assertThrows(DataAccessException.class, () -> dao.createUser(duplicate));
    }

    @Test
    public void testGetUserPositive() throws DataAccessException {
        UserData retrieved = dao.getUser("testUser");
        assertNotNull(retrieved);
    }

    @Test
    public void testGetUserNegativeNotFound() throws DataAccessException {
        UserData retrieved = dao.getUser("nonExistentUser");
        assertNull(retrieved);
    }

    @Test
    public void testStoreAndVerifyPasswordPositive() throws DataAccessException {
        dao.storeUserPassword("testUser", "newPassword");
        assertTrue(dao.verifyUser("testUser", "newPassword"));
    }

    @Test
    public void testVerifyPasswordNegative() throws DataAccessException {
        dao.storeUserPassword("testUser", "newPassword");
        assertFalse(dao.verifyUser("testUser", "wrongPassword"));
    }

    @Test
    public void testCreateGamePositive() throws DataAccessException {
        GameData game = new GameData(1, null, null, "Test Game", new ChessGame());
        dao.createGame(game);

        GameData retrieved = dao.getGame(1);
        assertNotNull(retrieved);
        assertEquals("Test Game", retrieved.gameName());
    }

    @Test
    public void testCreateGameNegativeDuplicateID() throws DataAccessException {
        GameData game = new GameData(1, null, null, "Test Game", new ChessGame());
        dao.createGame(game);

        GameData duplicate = new GameData(1, null, null, "Another Game", new ChessGame());
        assertThrows(DataAccessException.class, () -> dao.createGame(duplicate));
    }

    @Test
    public void testGetGamePositive() throws DataAccessException {
        GameData game = new GameData(1, null, null, "Test Game", new ChessGame());
        dao.createGame(game);

        GameData retrieved = dao.getGame(1);
        assertNotNull(retrieved);
        assertEquals("Test Game", retrieved.gameName());
    }

    @Test
    public void testGetGameNegativeNotFound() throws DataAccessException {
        GameData retrieved = dao.getGame(999);
        assertNull(retrieved);
    }

    @Test
    public void testListGamesPositive() throws DataAccessException {
        GameData game1 = new GameData(1, null, null, "Game 1", new ChessGame());
        GameData game2 = new GameData(2, "user1", null, "Game 2", new ChessGame());
        dao.createGame(game1);
        dao.createGame(game2);

        List<GameData> games = dao.listGames();
        assertEquals(2, games.size());
    }

    @Test
    public void testListGamesNegativeEmpty() throws DataAccessException {
        List<GameData> games = dao.listGames();
        assertTrue(games.isEmpty());
    }

    @Test
    public void testUpdateGamePositive() throws DataAccessException {
        GameData game = new GameData(1, null, null, "Test Game", new ChessGame());
        dao.createGame(game);

        GameData updatedGame = new GameData(1, "testUser", null, "Test Game", new ChessGame());
        dao.updateGame(updatedGame);

        GameData retrieved = dao.getGame(1);
        assertEquals("testUser", retrieved.whiteUsername());
    }

    @Test
    public void testUpdateGameNegativeNonexistent() throws DataAccessException {
        GameData nonExistent = new GameData(999, "user", null, "No Game", new ChessGame());
        assertThrows(DataAccessException.class, () -> dao.updateGame(nonExistent));
    }

    @Test
    public void testCreateAuthPositive() throws DataAccessException {
        AuthData auth = new AuthData("token123", "testUser");
        dao.createAuth(auth);

        AuthData retrieved = dao.getAuth("token123");
        assertNotNull(retrieved);
        assertEquals("testUser", retrieved.username());
    }

    @Test
    public void testCreateAuthNegativeDuplicate() throws DataAccessException {
        AuthData auth = new AuthData("token123", "testUser");
        dao.createAuth(auth);
        assertThrows(DataAccessException.class, () -> dao.createAuth(auth));
    }

    @Test
    public void testGetAuthPositive() throws DataAccessException {
        AuthData auth = new AuthData("token123", "testUser");
        dao.createAuth(auth);

        AuthData retrieved = dao.getAuth("token123");
        assertNotNull(retrieved);
    }

    @Test
    public void testGetAuthNegativeNotFound() throws DataAccessException {
        AuthData retrieved = dao.getAuth("nonexistent");
        assertNull(retrieved);
    }

    @Test
    public void testDeleteAuthPositive() throws DataAccessException {
        AuthData auth = new AuthData("token123", "testUser");
        dao.createAuth(auth);
        dao.deleteAuth("token123");
        AuthData retrieved = dao.getAuth("token123");
        assertNull(retrieved);
    }

    @Test
    public void testClearPositive() throws DataAccessException {
        dao.clear();

        assertNull(dao.getUser("user1"));
        assertNull(dao.getAuth("token1"));
        assertTrue(dao.listGames().isEmpty());
    }
}
