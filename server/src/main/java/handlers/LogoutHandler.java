package handlers;

import com.google.gson.Gson;
import dataaccess.DataAccessException;
import service.UserService;
import service.requests.LogoutRequest;
import service.results.LogoutResult;
import spark.Request;
import spark.Response;
import spark.Route;

public class LogoutHandler implements Route {
    private final UserService userService;
    private final Gson gson = new Gson();

    public LogoutHandler(UserService userService) {
        this.userService = userService;
    }

    @Override
    public Object handle(Request req, Response res) {
        try {
            String authToken = req.headers("authorization");

            LogoutRequest logoutRequest = new LogoutRequest(authToken);

            LogoutResult result = userService.logout(logoutRequest);

            res.status(200);
            return gson.toJson(result);

        }
        catch (DataAccessException e) {
            if (e.getMessage().contains("token")) {
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
