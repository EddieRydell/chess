package dataaccess;

import chess.ChessGame;
import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import dataaccess.MemoryDataAccess;
import model.AuthData;
import model.GameData;
import model.UserData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import service.ClearService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DataAccessTests {
    @Test
    public void testCreateUserPositive() throws DataAccessException {
        DataAccess dao = new DBDataAccess();
        dao.clear();

        UserData user = new UserData("testUser", "password", "test@mail.com");
        dao.createUser(user);

        UserData retrieved = dao.getUser("testUser");
        assertNotNull(retrieved);
        assertEquals("testUser", retrieved.username());
        assertEquals("test@mail.com", retrieved.email());
    }

    @Test
    public void testCreateUserNegativeDuplicate() throws DataAccessException {
        DataAccess dao = new DBDataAccess();
        dao.clear();

        UserData user = new UserData("testUser", "password", "test@mail.com");
        dao.createUser(user);

        // Expect an exception when trying to create the same user again.
        assertThrows(DataAccessException.class, () -> dao.createUser(user));
    }

    @Test
    public void testGetUserPositive() throws DataAccessException {
        DataAccess dao = new DBDataAccess();
        dao.clear();

        UserData user = new UserData("testUser", "password", "test@mail.com");
        dao.createUser(user);

        UserData retrieved = dao.getUser("testUser");
        assertNotNull(retrieved);
    }

    @Test
    public void testGetUserNegativeNotFound() throws DataAccessException {
        DataAccess dao = new DBDataAccess();
        dao.clear();

        UserData retrieved = dao.getUser("nonExistentUser");
        assertNull(retrieved);
    }

    @Test
    public void testStoreAndVerifyPasswordPositive() throws DataAccessException {
        DataAccess dao = new DBDataAccess();
        dao.clear();

        UserData user = new UserData("testUser", "initialPassword", "test@mail.com");
        dao.createUser(user);

        // Update the password
        dao.storeUserPassword("testUser", "newPassword");

        // Correct password should return true
        assertTrue(dao.verifyUser("testUser", "newPassword"));
    }

    @Test
    public void testVerifyPasswordNegative() throws DataAccessException {
        DataAccess dao = new DBDataAccess();
        dao.clear();

        UserData user = new UserData("testUser", "initialPassword", "test@mail.com");
        dao.createUser(user);
        dao.storeUserPassword("testUser", "newPassword");

        // Incorrect password should return false
        assertFalse(dao.verifyUser("testUser", "wrongPassword"));
    }

    @Test
    public void testCreateGamePositive() throws DataAccessException {
        DataAccess dao = new DBDataAccess();
        dao.clear();

        GameData game = new GameData(1, null, null, "Test Game", new ChessGame());
        dao.createGame(game);

        GameData retrieved = dao.getGame(1);
        assertNotNull(retrieved);
        assertEquals("Test Game", retrieved.gameName());
    }

    @Test
    public void testCreateGameNegativeDuplicateID() throws DataAccessException {
        DataAccess dao = new DBDataAccess();
        dao.clear();

        GameData game = new GameData(1, null, null, "Test Game", new ChessGame());
        dao.createGame(game);

        GameData duplicate = new GameData(1, null, null, "Another Game", new ChessGame());
        assertThrows(DataAccessException.class, () -> dao.createGame(duplicate));
    }

    @Test
    public void testGetGamePositive() throws DataAccessException {
        DataAccess dao = new DBDataAccess();
        dao.clear();

        GameData game = new GameData(1, null, null, "Test Game", new ChessGame());
        dao.createGame(game);

        GameData retrieved = dao.getGame(1);
        assertNotNull(retrieved);
        assertEquals("Test Game", retrieved.gameName());
    }

    @Test
    public void testGetGameNegativeNotFound() throws DataAccessException {
        DataAccess dao = new DBDataAccess();
        dao.clear();

        GameData retrieved = dao.getGame(999);
        assertNull(retrieved);
    }

    @Test
    public void testListGamesPositive() throws DataAccessException {
        DataAccess dao = new DBDataAccess();
        dao.clear();

        GameData game1 = new GameData(1, null, null, "Game 1", new ChessGame());
        GameData game2 = new GameData(2, "user1", null, "Game 2", new ChessGame());
        dao.createGame(game1);
        dao.createGame(game2);

        List<GameData> games = dao.listGames();
        assertEquals(2, games.size());
    }

    @Test
    public void testListGamesNegativeEmpty() throws DataAccessException {
        DataAccess dao = new DBDataAccess();
        dao.clear();

        List<GameData> games = dao.listGames();
        assertTrue(games.isEmpty());
    }

    @Test
    public void testUpdateGamePositive() throws DataAccessException {
        DataAccess dao = new DBDataAccess();
        dao.clear();

        GameData game = new GameData(1, null, null, "Test Game", new ChessGame());
        dao.createGame(game);

        // Simulate a join: update whiteUsername
        GameData updatedGame = new GameData(1, "testUser", null, "Test Game", new ChessGame());
        dao.updateGame(updatedGame);

        GameData retrieved = dao.getGame(1);
        assertEquals("testUser", retrieved.whiteUsername());
    }

    @Test
    public void testUpdateGameNegativeNonexistent() throws DataAccessException {
        DataAccess dao = new DBDataAccess();
        dao.clear();

        GameData nonExistent = new GameData(999, "user", null, "No Game", new ChessGame());
        assertThrows(DataAccessException.class, () -> dao.updateGame(nonExistent));
    }

    @Test
    public void testCreateAuthPositive() throws DataAccessException {
        DataAccess dao = new DBDataAccess();
        dao.clear();

        AuthData auth = new AuthData("token123", "testUser");
        dao.createAuth(auth);

        AuthData retrieved = dao.getAuth("token123");
        assertNotNull(retrieved);
        assertEquals("testUser", retrieved.username());
    }

    @Test
    public void testCreateAuthNegativeDuplicate() throws DataAccessException {
        DataAccess dao = new DBDataAccess();
        dao.clear();

        AuthData auth = new AuthData("token123", "testUser");
        dao.createAuth(auth);

        assertThrows(DataAccessException.class, () -> dao.createAuth(auth));
    }

    @Test
    public void testGetAuthPositive() throws DataAccessException {
        DataAccess dao = new DBDataAccess();
        dao.clear();

        AuthData auth = new AuthData("token123", "testUser");
        dao.createAuth(auth);

        AuthData retrieved = dao.getAuth("token123");
        assertNotNull(retrieved);
    }

    @Test
    public void testGetAuthNegativeNotFound() throws DataAccessException {
        DataAccess dao = new DBDataAccess();
        dao.clear();

        AuthData retrieved = dao.getAuth("nonexistent");
        assertNull(retrieved);
    }

    @Test
    public void testDeleteAuthPositive() throws DataAccessException {
        DataAccess dao = new DBDataAccess();
        dao.clear();

        AuthData auth = new AuthData("token123", "testUser");
        dao.createAuth(auth);

        dao.deleteAuth("token123");
        AuthData retrieved = dao.getAuth("token123");
        assertNull(retrieved);
    }

    @Test
    public void testClearPositive() throws DataAccessException {
        DataAccess dao = new DBDataAccess();

        dao.createUser(new UserData("user1", "pass", "user1@mail.com"));
        dao.createGame(new GameData(1, null, null, "Game 1", new ChessGame()));
        dao.createAuth(new AuthData("token1", "user1"));

        dao.clear();

        assertNull(dao.getUser("user1"));
        assertNull(dao.getAuth("token1"));
        assertTrue(dao.listGames().isEmpty());
    }
}
