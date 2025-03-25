package handlers;

import model.GameData;
import service.GameService;
import com.google.gson.Gson;
import dataaccess.DataAccessException;
import spark.Request;
import spark.Response;
import spark.Route;

public class GetGameHandler implements Route {
    private final GameService gameService;
    private final Gson gson = new Gson();

    public GetGameHandler(GameService gameService) {
        this.gameService = gameService;
    }

    @Override
    public Object handle(Request req, Response res) throws DataAccessException {
        try {
            String token = req.headers("Authorization");
            int gameId = Integer.parseInt(req.params("id"));

            GameData gameData = gameService.getGame(gameId, token);

            res.status(200);
            return gson.toJson(gameData);

        }
        catch (DataAccessException e) {
            if (e.getMessage().toLowerCase().contains("unauthorized")) {
                res.status(401);
                return "{\"message\":\"Error: unauthorized\"}";
            }
            res.status(500);
            return "{\"message\":\"Error: " + e.getMessage() + "\"}";
        }
        catch (Exception e) {
            res.status(500);
            return "{\"message\":\"Error: " + e.getMessage() + "\"}";
        }
    }
}
