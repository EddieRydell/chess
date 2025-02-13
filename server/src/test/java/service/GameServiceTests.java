package service;

import chess.ChessGame;
import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import dataaccess.MemoryDataAccess;
import model.GameData;
import model.UserData;
import org.junit.jupiter.api.*;
import service.requests.CreateGameRequest;
import service.requests.JoinGameRequest;
import service.results.CreateGameResult;
import service.results.JoinGameResult;
import service.results.ListGamesResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameServiceTests {

    private DataAccess dao;
    private GameService gameService;

    @BeforeEach
    void setup() throws DataAccessException {
        dao = new MemoryDataAccess();
        gameService = new GameService(dao);

        dao.createUser(new UserData("bob", "pass", "bob@byu.edu"));
        dao.createAuth(new model.AuthData("bob-token", "bob"));
    }

    @Test
    void testCreateGameSuccess() throws DataAccessException {
        CreateGameRequest request = new CreateGameRequest("test");
        CreateGameResult result = gameService.createGame(request, "bob-token");

        assertNotNull(result);
        assertTrue(result.gameID() > 0);

        GameData createdGame = dao.getGame(result.gameID());
        assertNotNull(createdGame);
        assertEquals("test", createdGame.gameName());
        assertNull(createdGame.whiteUsername());
        assertNull(createdGame.blackUsername());
        assertNotNull(createdGame.game());
    }

    @Test
    void testCreateGameUnauthorized() {
        CreateGameRequest request = new CreateGameRequest("No Auth Game");
        assertThrows(DataAccessException.class,
                () -> gameService.createGame(request, "bad-token"),
                "Expected exception for invalid/unauthorized token");
    }

    @Test
    void testJoinGameSuccess() throws DataAccessException {
        CreateGameRequest req = new CreateGameRequest("Joinable");
        CreateGameResult cResult = gameService.createGame(req, "bob-token");

        JoinGameRequest joinReq = new JoinGameRequest("WHITE", cResult.gameID());
        JoinGameResult jResult = gameService.joinGame(joinReq, "bob-token");

        assertNotNull(jResult);

        GameData updated = dao.getGame(cResult.gameID());
        assertEquals("bob", updated.whiteUsername());
        assertNull(updated.blackUsername());
    }

    @Test
    void testJoinGameAlreadyTaken() throws DataAccessException {
        CreateGameRequest req = new CreateGameRequest("Occupied");
        CreateGameResult cResult = gameService.createGame(req, "bob-token");

        JoinGameRequest joinReq1 = new JoinGameRequest("BLACK", cResult.gameID());
        gameService.joinGame(joinReq1, "bob-token");

        dao.createUser(new UserData("alice", "xyz", "alice@byu.edu"));
        dao.createAuth(new model.AuthData("alice-token", "alice"));

        JoinGameRequest joinReq2 = new JoinGameRequest("BLACK", cResult.gameID());
        assertThrows(DataAccessException.class,
                () -> gameService.joinGame(joinReq2, "alice-token"),
                "Expected 'already taken' exception");
    }

    @Test
    void testListGamesSuccess() throws DataAccessException {
        dao.createGame(new GameData(1, null, null, "GameOne", new ChessGame()));
        dao.createGame(new GameData(2, "bob", null, "GameTwo", new ChessGame()));

        ListGamesResult result = gameService.listGames("bob-token");
        assertNotNull(result);
        List<GameData> games = result.games();
        assertEquals(2, games.size());
    }

    @Test
    void testListGamesUnauthorized() {
        assertThrows(DataAccessException.class,
                () -> gameService.listGames("bad-token"),
                "Expected unauthorized for invalid token");
    }
}
