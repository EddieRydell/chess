package server;

import dataaccess.DBDataAccess;
import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import handlers.*;
import service.ClearService;
import service.GameService;
import service.UserService;
import spark.*;

public class Server {

    public int run(int desiredPort) {
        Spark.port(desiredPort);

        Spark.staticFiles.location("web");

        // Register your endpoints and handle exceptions here.
        DataAccess dao;
        try {
            dao = new DBDataAccess();
        }
        catch (DataAccessException e) {
            throw new RuntimeException(e);
        }
        UserService userService = new UserService(dao);
        ClearService clearService = new ClearService(dao);
        GameService gameService = new GameService(dao);

        Spark.post("/user", new RegisterHandler(userService));
        Spark.post("/session", new LoginHandler(userService));
        Spark.delete("/session", new LogoutHandler(userService));
        Spark.post("/game", new CreateGameHandler(gameService));
        Spark.put("/game", new JoinGameHandler(gameService));
        Spark.get("/game", new ListGamesHandler(gameService));
        Spark.delete("/db", new ClearHandler(clearService));

        //This line initializes the server and can be removed once you have a functioning endpoint
        Spark.init();

        Spark.awaitInitialization();
        return Spark.port();
    }

    public void stop() {
        Spark.stop();
        Spark.awaitStop();
    }
}
