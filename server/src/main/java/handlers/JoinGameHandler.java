package handlers;

import com.google.gson.Gson;
import dataaccess.DataAccessException;
import service.GameService;
import service.requests.JoinGameRequest;
import spark.Request;
import spark.Response;
import spark.Route;

public class JoinGameHandler implements Route {

    private final GameService gameService;
    private final Gson gson = new Gson();

    public JoinGameHandler(GameService gameService) {
        this.gameService = gameService;
    }

    @Override
    public Object handle(Request req, Response res) {
        try {
            String token = req.headers("authorization");
            var requestBody = req.body();
            var joinRequest = gson.fromJson(requestBody, JoinGameRequest.class);

            gameService.joinGame(joinRequest, token);

            res.status(200);
            return "{}";

        }
        catch (DataAccessException e) {
            String msg = e.getMessage().toLowerCase();
            if (msg.contains("unauthorized")) {
                res.status(401);
                return "{\"message\":\"Error: unauthorized\"}";
            }
            else if (msg.contains("bad request")) {
                res.status(400);
                return "{\"message\":\"Error: bad request\"}";
            }
            else if (msg.contains("already taken")) {
                res.status(403);
                return "{\"message\":\"Error: already taken\"}";
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
