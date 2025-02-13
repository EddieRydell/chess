package handlers;

import com.google.gson.Gson;
import dataaccess.DataAccessException;
import service.UserService;
import service.requests.LoginRequest;
import service.results.LoginResult;
import spark.Request;
import spark.Response;
import spark.Route;

/**
 * Handles POST /session
 */
public class LoginHandler implements Route {

    private final UserService userService;
    private final Gson gson = new Gson();

    public LoginHandler(UserService userService) {
        this.userService = userService;
    }

    @Override
    public Object handle(Request req, Response res) {
        try {
            var requestBody = req.body();
            LoginRequest loginRequest = gson.fromJson(requestBody, LoginRequest.class);

            LoginResult loginResult = userService.login(loginRequest);

            res.status(200);
            return gson.toJson(loginResult);

        }
        catch (IllegalArgumentException e) {
            res.status(400);
            return "{\"message\":\"Error: bad request\"}";

        }
        catch (DataAccessException e) {
            if (e.getMessage().contains("Invalid username or password")) {
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
