package server;

import dataaccess.DBDataAccess;
import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import handlers.*;
import service.ClearService;
import service.GameService;
import service.UserService;
import spark.Spark;
import websocket.GameWebSocketHandler;

public class Server {

    public int run(int desiredPort) {
        Spark.port(desiredPort);

        Spark.webSocket("/ws", GameWebSocketHandler.class);

        Spark.staticFiles.location("web");

        DataAccess dao;
        try {
            dao = new DBDataAccess();
        } catch (DataAccessException e) {
            throw new RuntimeException(e);
        }

        UserService  userService  = new UserService(dao);
        GameService  gameService  = new GameService(dao);
        ClearService clearService = new ClearService(dao);

        Spark.post  ("/user",   new RegisterHandler(userService));
        Spark.post  ("/session", new LoginHandler(userService));
        Spark.delete("/session", new LogoutHandler(userService));

        Spark.post ("/game", new CreateGameHandler(gameService));
        Spark.put  ("/game", new JoinGameHandler(gameService));
        Spark.get  ("/game", new ListGamesHandler(gameService));
        Spark.get  ("/game/:id", new GetGameHandler(gameService));

        Spark.delete("/db", new ClearHandler(clearService));

        Spark.init();
        Spark.awaitInitialization();
        System.out.println("HTTP  server running  on http://localhost:" + Spark.port());
        System.out.println("WS    endpoint ready on  ws://localhost:" + Spark.port() + "/ws");
        return Spark.port();
    }

    public void stop() {
        Spark.stop();
        Spark.awaitStop();
    }
}
