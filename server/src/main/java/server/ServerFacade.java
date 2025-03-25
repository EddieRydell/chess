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

    public AuthData login(String username, String password) throws ResponseException {
        record LoginRequest(String username, String password) {}
        String path = "/session";
        LoginRequest requestBody = new LoginRequest(username, password);

        return makeRequest("POST", path, requestBody, AuthData.class, null);
    }

    public AuthData register(String username, String password, String email) throws ResponseException {
        record RegisterRequest(String username, String password, String email) {}
        String path = "/user";
        RegisterRequest requestBody = new RegisterRequest(username, password, email);

        return makeRequest("POST", path, requestBody, AuthData.class, null);
    }

    public void logout(String authToken) throws ResponseException {
        String path = "/session";
        makeRequest("DELETE", path, null, null, authToken);
    }

    public void createGame(String authToken, String gameName) throws ResponseException {
        record CreateGameRequest(String authToken, String gameName) {}
        String path = "/game";
        var body = new CreateGameRequest(authToken, gameName);

        makeRequest("POST", path, body, null, null);
    }

    public List<GameData> listGames(String authToken) throws ResponseException {
        // We'll parse it as an array, then convert to a List.
        GameData[] gamesArray = makeRequest("GET", "/game", null, GameData[].class, authToken);
        if (gamesArray == null) {
            return List.of(); // empty list
        }
        return Arrays.asList(gamesArray);
    }

    public void joinGame(String authToken, String gameId, String color) throws ResponseException {
        record JoinGameRequest(String authToken, String gameId, String color) {}
        String path = "/game";
        var body = new JoinGameRequest(authToken, gameId, color);

        makeRequest("PUT", path, body, null, null);
    }

    public void observeGame(String authToken, String gameId) throws ResponseException {
        record ObserveGameRequest(String authToken, String gameId) {}
        String path = "/game/observe"; // You must implement this on the server
        var body = new ObserveGameRequest(authToken, gameId);

        makeRequest("PUT", path, body, null, null);
    }

    private <T> T makeRequest(String method,
                              String path,
                              Object requestBody,
                              Class<T> responseClass,
                              String authToken) throws ResponseException {

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

            // If we have a request body, write it out as JSON
            if (requestBody != null) {
                http.setRequestProperty("Content-Type", "application/json");
                String reqJson = gson.toJson(requestBody);
                try (OutputStream os = http.getOutputStream()) {
                    os.write(reqJson.getBytes());
                }
            }

            // Connect & check status
            http.connect();
            throwIfNotSuccessful(http);

            // For endpoints that return JSON, parse it if responseClass != null
            return readJsonBody(http, responseClass);

        } catch (ResponseException rex) {
            throw rex;
        } catch (Exception ex) {
            // Wrap other exceptions in a 500-level ResponseException
            throw new ResponseException(500, ex.getMessage());
        } finally {
            if (http != null) {
                http.disconnect();
            }
        }
    }

    /**
     * Throws a ResponseException if HTTP status != 2xx
     */
    private static void throwIfNotSuccessful(HttpURLConnection http)
            throws IOException, ResponseException {
        int status = http.getResponseCode();
        if (status < 200 || status >= 300) {
            // Attempt to parse error message from body:
            try (InputStream errorStream = http.getErrorStream()) {
                if (errorStream != null) {
                    throw ResponseException.fromJson(errorStream);
                }
            }
            // Fallback if no error body
            throw new ResponseException(status, "HTTP Error " + status);
        }
    }

    /**
     * If responseClass is non-null, parse the JSON body into that class.
     * Otherwise returns null.
     */
    private static <T> T readJsonBody(HttpURLConnection http,
                                      Class<T> responseClass) throws IOException {
        if (responseClass == null) {
            // We don't want a response object
            return null;
        }
        try (InputStream in = http.getInputStream()) {
            InputStreamReader reader = new InputStreamReader(in);
            return gson.fromJson(reader, responseClass);
        }
    }
}
