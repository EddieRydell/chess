package handlers;

import com.google.gson.Gson;
import dataaccess.DataAccessException;
import service.GameService;
import service.results.ListGamesResult;
import spark.Request;
import spark.Response;
import spark.Route;

public class ListGamesHandler implements Route {
    private final GameService gameService;
    private final Gson gson = new Gson();

    public ListGamesHandler(GameService gameService) {
        this.gameService = gameService;
    }

    @Override
    public Object handle(Request req, Response res) {
        try {
            String token = req.headers("authorization");

            ListGamesResult result = gameService.listGames(token);

            res.status(200);
            return gson.toJson(result);

        }
        catch (DataAccessException e) {
            String msg = e.getMessage().toLowerCase();

            if (msg.contains("unauthorized")) {
                res.status(401);
                return "{\"message\":\"Error: unauthorized\"}";
            }
            else {
                res.status(500);
                return "{\"message\":\"Error: " + e.getMessage() + "\"}";
            }

        }
        catch (Exception e) {
            res.status(500);
            return "{\"message\":\"Error: " + e.getMessage() + "\"}";
        }
    }
}
