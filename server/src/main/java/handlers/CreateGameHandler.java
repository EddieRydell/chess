package handlers;

import com.google.gson.Gson;
import dataaccess.DataAccessException;
import service.GameService;
import service.requests.CreateGameRequest;
import spark.Request;
import spark.Response;
import spark.Route;

public class CreateGameHandler implements Route {
    private final GameService gameService;
    private final Gson gson = new Gson();

    public CreateGameHandler(GameService gameService) {
        this.gameService = gameService;
    }

    @Override
    public Object handle(Request req, Response res) {
        try {
            String token = req.headers("authorization");

            var requestBody = req.body();
            var createRequest = gson.fromJson(requestBody, CreateGameRequest.class);

            var result = gameService.createGame(createRequest, token);

            res.status(200);
            return gson.toJson(result);

        }
        catch (DataAccessException e) {
            if (e.getMessage().toLowerCase().contains("unauthorized")) {
                res.status(401);
                return "{\"message\":\"Error: unauthorized\"}";
            }
            else if (e.getMessage().toLowerCase().contains("bad request")) {
                res.status(400);
                return "{\"message\":\"Error: bad request\"}";
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
