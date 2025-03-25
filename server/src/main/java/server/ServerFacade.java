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

    // -----------------------------------------------
    // 1) LOGIN
    // -----------------------------------------------
    public AuthData login(String username, String password) {
        record LoginRequest(String username, String password) {}
        String path = "/session";
        LoginRequest requestBody = new LoginRequest(username, password);

        return makeRequest("POST", path, requestBody, AuthData.class, null);
    }

    // -----------------------------------------------
    // 2) REGISTER
    // -----------------------------------------------
    public AuthData register(String username, String password, String email) {
        record RegisterRequest(String username, String password, String email) {}
        String path = "/user";
        RegisterRequest requestBody = new RegisterRequest(username, password, email);

        return makeRequest("POST", path, requestBody, AuthData.class, null);
    }

    // -----------------------------------------------
    // 3) LOGOUT
    // -----------------------------------------------
    public void logout(String authToken) {
        String path = "/session";
        makeRequest("DELETE", path, null, null, authToken);
    }

    // -----------------------------------------------
    // 4) CREATE GAME
    // -----------------------------------------------
    public void createGame(String authToken, String gameName) {
        record CreateGameRequest(String authToken, String gameName) {}
        String path = "/game";
        CreateGameRequest body = new CreateGameRequest(authToken, gameName);

        makeRequest("POST", path, body, null, null);
    }

    // -----------------------------------------------
    // 5) LIST GAMES
    // -----------------------------------------------
    public List<GameData> listGames(String authToken) {
        GameData[] gamesArray = makeRequest("GET", "/game", null, GameData[].class, authToken);
        if (gamesArray == null) {
            return List.of();
        }
        return Arrays.asList(gamesArray);
    }

    // -----------------------------------------------
    // 6) JOIN GAME
    // -----------------------------------------------
    public void joinGame(String authToken, String gameId, String color) {
        record JoinGameRequest(String authToken, String gameId, String color) {}
        String path = "/game";
        JoinGameRequest body = new JoinGameRequest(authToken, gameId, color);

        makeRequest("PUT", path, body, null, null);
    }

    // -----------------------------------------------
    // 7) OBSERVE GAME
    // -----------------------------------------------
    public void observeGame(String authToken, String gameId) {
        record ObserveGameRequest(String authToken, String gameId) {}
        String path = "/game/observe"; // Make sure this endpoint exists on the server
        ObserveGameRequest body = new ObserveGameRequest(authToken, gameId);

        makeRequest("PUT", path, body, null, null);
    }

    // =====================================================
    //      LOW-LEVEL HELPER that does the HTTP calls
    // =====================================================
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

            // If we have an auth token, put it in the Authorization header
            if (authToken != null && !authToken.isBlank()) {
                http.setRequestProperty("Authorization", authToken);
            }

            // For POST/PUT/DELETE, we usually want to send a body
            if (!method.equals("GET")) {
                http.setDoOutput(true);
            }

            // Write body as JSON if present
            if (requestBody != null) {
                http.setRequestProperty("Content-Type", "application/json");
                String reqJson = gson.toJson(requestBody);
                try (OutputStream os = http.getOutputStream()) {
                    os.write(reqJson.getBytes());
                }
            }

            // Fire the request
            http.connect();

            // Throw if not 2xx
            throwIfNotSuccessful(http);

            // Parse JSON body (if responseClass != null)
            return readJsonBody(http, responseClass);

        } catch (Exception ex) {
            // Wrap any exception in a RuntimeException
            throw new RuntimeException("Error making HTTP request: " + ex.getMessage(), ex);
        } finally {
            if (http != null) {
                http.disconnect();
            }
        }
    }

    /**
     * Checks the HTTP status code and throws a RuntimeException if
     * it is not a 2xx successful response.
     */
    private static void throwIfNotSuccessful(HttpURLConnection http) throws IOException {
        int status = http.getResponseCode();
        if (status < 200 || status >= 300) {
            // Attempt to read the error stream (if any) for diagnostic info
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

    /**
     * Reads the response body as JSON into the specified type.
     * If responseClass is null, we skip parsing and return null.
     */
    private static <T> T readJsonBody(HttpURLConnection http, Class<T> responseClass) throws IOException {
        if (responseClass == null) {
            // If no response is expected, just return null
            return null;
        }
        // Otherwise parse the JSON
        try (InputStream in = http.getInputStream();
             InputStreamReader reader = new InputStreamReader(in)) {
            return gson.fromJson(reader, responseClass);
        }
    }
}
