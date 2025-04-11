package ui;

import com.google.gson.Gson;
import model.AuthData;
import model.GameData;
import websocket.messages.ServerMessage;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ServerFacade implements ServerMessageObserver {
    private final String baseUrl;
    private static final Gson GSON = new Gson();

    private ChessClient client;
    public void setClient(ChessClient client) {
        this.client = client;
    }

    // New field for WebSocket communication.
    private WebSocketCommunicator wsComm;

    public ServerFacade(int port) {
        this("http://localhost:" + port);
    }

    public ServerFacade(String url) {
        this.baseUrl = url;
    }

    // --- HTTP methods (unchanged) ---
    public AuthData login(String username, String password) {
        record LoginRequest(String username, String password) {}
        String path = "/session";
        LoginRequest requestBody = new LoginRequest(username, password);
        return makeRequest("POST", path, requestBody, AuthData.class, null);
    }

    public AuthData register(String username, String password, String email) {
        record RegisterRequest(String username, String password, String email) {}
        String path = "/user";
        RegisterRequest requestBody = new RegisterRequest(username, password, email);
        return makeRequest("POST", path, requestBody, AuthData.class, null);
    }

    public void logout(String authToken) {
        String path = "/session";
        makeRequest("DELETE", path, null, null, authToken);
    }

    public void createGame(String authToken, String gameName) {
        record CreateGameRequest(String authToken, String gameName) {}
        String path = "/game";
        CreateGameRequest body = new CreateGameRequest(authToken, gameName);
        makeRequest("POST", path, body, null, authToken);
    }

    public GameData getGame(String authToken, int gameId) {
        String path = "/game/" + gameId;
        return makeRequest("GET", path, null, GameData.class, authToken);
    }

    public List<GameData> listGames(String authToken) {
        record GameListResponse(GameData[] games) {}
        GameListResponse response = makeRequest("GET", "/game", null, GameListResponse.class, authToken);
        if (response == null || response.games() == null) {
            return List.of();
        }
        return Arrays.asList(response.games());
    }

    public void joinGame(String authToken, int gameId, String color) {
        record JoinGameRequest(String playerColor, int gameID) {}
        String path = "/game";
        JoinGameRequest body = new JoinGameRequest(color, gameId);
        makeRequest("PUT", path, body, null, authToken);
    }

    public void connectToGame(String authToken, int gameId) {
        wsComm = new WebSocketCommunicator(baseUrl, this);
        wsComm.connect();
        wsComm.sendConnectCommand(authToken, gameId);
    }

    public void sendMakeMove(String authToken, int gameId, chess.ChessMove move) {
        if (wsComm != null) {
            wsComm.sendMakeMoveCommand(authToken, gameId, move);
        }
    }

    public void sendResign(String authToken, int gameId) {
        if (wsComm != null) {
            wsComm.sendResignCommand(authToken, gameId);
        }
    }

    public void sendLeave(String authToken, int gameId) {
        if (wsComm != null) {
            wsComm.sendLeaveCommand(authToken, gameId);
        }
    }

    private <T> T makeRequest(String method,
                              String path,
                              Object requestBody,
                              Class<T> responseClass,
                              String authToken) {

        HttpURLConnection http = null;
        try {
            URL url = new URI(baseUrl + path).toURL();
            http = (HttpURLConnection) url.openConnection();
            http.setRequestMethod(method);

            if (authToken != null && !authToken.isBlank()) {
                http.setRequestProperty("Authorization", authToken);
            }

            if (!method.equals("GET")) {
                http.setDoOutput(true);
            }

            if (requestBody != null) {
                http.setRequestProperty("Content-Type", "application/json");
                String reqJson = GSON.toJson(requestBody);
                try (OutputStream os = http.getOutputStream()) {
                    os.write(reqJson.getBytes());
                }
            }

            http.connect();
            throwIfNotSuccessful(http);
            return readJsonBody(http, responseClass);

        } catch (Exception ex) {
            throw new RuntimeException("Error making HTTP request: " + ex.getMessage(), ex);
        } finally {
            if (http != null) {
                http.disconnect();
            }
        }
    }

    private static void throwIfNotSuccessful(HttpURLConnection http) throws IOException {
        int status = http.getResponseCode();
        if (status >= 200 && status < 300) {
            return;
        }
        String errorMessage = "";
        InputStream errorStream = http.getErrorStream();
        if (errorStream != null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream))) {
                errorMessage = reader.lines().collect(Collectors.joining("\n"));
            }
        }
        throw new RuntimeException("HTTP Error " + status + ": " + errorMessage);
    }

    private static <T> T readJsonBody(HttpURLConnection http, Class<T> responseClass) throws IOException {
        if (responseClass == null) {
            return null;
        }
        try (InputStream in = http.getInputStream();
             InputStreamReader reader = new InputStreamReader(in)) {
            return GSON.fromJson(reader, responseClass);
        }
    }

    @Override
    public void onServerMessage(ServerMessage message) {
        if (client != null) {
            client.handleServerMessage(message);
        } else {
            // Fallback if no client is set.
            System.out.println("Received message: " + message);
        }
    }
}
