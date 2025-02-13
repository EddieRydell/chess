package handlers;

import com.google.gson.Gson;
import dataaccess.DataAccessException;
import service.UserService;
import service.requests.RegisterRequest;
import service.results.RegisterResult;
import spark.Request;
import spark.Response;
import spark.Route;

public class RegisterHandler implements Route {
    private final UserService userService;
    private final Gson gson = new Gson();

    public RegisterHandler(UserService userService) {
        this.userService = userService;
    }

    @Override
    public Object handle(Request req, Response res) {
        try {
            String requestBody = req.body();
            RegisterRequest registerRequest = gson.fromJson(requestBody, RegisterRequest.class);

            RegisterResult result = userService.register(registerRequest);

            res.status(200);
            return gson.toJson(result);

        }
        catch (IllegalArgumentException e) {
            res.status(400);
            return "{\"message\":\"Error: bad request\"}";

        }
        catch (DataAccessException e) {
            if (e.getMessage().contains("User already exists")
                    || e.getMessage().contains("already taken")) {
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
