package service;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import model.AuthData;
import model.GameData;
import service.requests.CreateGameRequest;
import service.requests.JoinGameRequest;
import service.results.CreateGameResult;
import service.results.JoinGameResult;
import service.results.ListGamesResult;

import chess.ChessGame;

import java.util.List;

public class GameService {
    private final DataAccess dao;
    private int nextGameID = 1;

    public GameService(DataAccess dao) {
        this.dao = dao;
    }

    public GameData getGame(int gameID, String token) throws DataAccessException {
        AuthData auth = requireValidAuth(token);
        GameData game = dao.getGame(gameID);
        if (game == null) {
            throw new DataAccessException("Bad request: game not found");
        }
        return game;
    }

    public CreateGameResult createGame(CreateGameRequest request, String authToken)
            throws DataAccessException {
        requireValidAuth(authToken);

        if (request.gameName() == null || request.gameName().isBlank()) {
            throw new DataAccessException("Bad request: gameName is missing/empty");
        }

        int newID = nextGameID;
        nextGameID++;

        GameData newGame = new GameData(
                newID,
                null,
                null,
                request.gameName(),
                new ChessGame()
        );

        dao.createGame(newGame);

        return new CreateGameResult(newID);
    }

    public JoinGameResult joinGame(JoinGameRequest request, String authToken)
            throws DataAccessException {
        AuthData authData = requireValidAuth(authToken);
        String username = authData.username();

        if (request.playerColor() == null) {
            throw new DataAccessException("Bad request: No color specified");
        }
        String color = request.playerColor().toUpperCase();
        if (!color.equals("WHITE") && !color.equals("BLACK")) {
            throw new DataAccessException("Bad request: invalid player color '" + request.playerColor() + "'");
        }

        if (request.gameID() <= 0) {
            throw new DataAccessException("Bad request: invalid gameID");
        }

        GameData game = dao.getGame(request.gameID());
        if (game == null) {
            throw new DataAccessException("Bad request: game does not exist");
        }

        if (color.equals("WHITE")) {
            if (game.whiteUsername() != null) {
                throw new DataAccessException("already taken");
            }
            game = new GameData(
                    game.gameID(),
                    username,
                    game.blackUsername(),
                    game.gameName(),
                    game.game()
            );
        }
        else {
            if (game.blackUsername() != null) {
                throw new DataAccessException("already taken");
            }
            game = new GameData(
                    game.gameID(),
                    game.whiteUsername(),
                    username,
                    game.gameName(),
                    game.game()
            );
        }

        dao.updateGame(game);

        return new JoinGameResult();
    }

    public ListGamesResult listGames(String authToken) throws DataAccessException {
        requireValidAuth(authToken);

        List<GameData> allGames = dao.listGames();

        return new ListGamesResult(allGames);
    }

    private AuthData requireValidAuth(String token) throws DataAccessException {
        if (token == null || token.isBlank()) {
            throw new DataAccessException("Unauthorized: no authToken provided");
        }
        AuthData auth = dao.getAuth(token);
        if (auth == null) {
            throw new DataAccessException("Unauthorized: invalid token");
        }
        return auth;
    }
}
