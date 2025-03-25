package server;

import com.google.gson.Gson;
import model.AuthData;
import model.GameData;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

public class ServerFacade {
    private final String baseUrl;
    private static final Gson gson = new Gson();

    public ServerFacade(int port) {
        this("http://localhost:" + port);
    }

    public ServerFacade(String url) {
        this.baseUrl = url;
    }

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
        record JoinGameRequest(String color, int gameID) {}
        String path = "/game";
        JoinGameRequest body = new JoinGameRequest(color, gameId);

        makeRequest("PUT", path, body, null, authToken);
    }

    public GameData observeGame(String authToken, int gameId) {
        return getGame(authToken, gameId);
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
                String reqJson = gson.toJson(requestBody);
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
        if (status < 200 || status >= 300) {
            StringBuilder sb = new StringBuilder();
            try (InputStream errorStream = http.getErrorStream()) {
                if (errorStream != null) {
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(errorStream))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            sb.append(line).append("\n");
                        }
                    }
                }
            }
            throw new RuntimeException("HTTP Error " + status + ": " + sb);
        }
    }

    private static <T> T readJsonBody(HttpURLConnection http, Class<T> responseClass) throws IOException {
        if (responseClass == null) {
            return null;
        }
        try (InputStream in = http.getInputStream();
             InputStreamReader reader = new InputStreamReader(in)) {
            return gson.fromJson(reader, responseClass);
        }
    }
}
