package ui;

import java.util.Arrays;

import model.AuthData;
import model.GameData;
import server.ServerFacade;

/**
 * ChessClient:
 *  - Interprets console-like commands (e.g. "login user pass")
 *  - Communicates with the server via ServerFacade
 *  - Maintains a simple logged-in / logged-out state
 */
public class ChessClient {

    private enum State {
        LOGGEDOUT,
        LOGGEDIN
    }

    private State state = State.LOGGEDOUT;
    private AuthData currentUser = null;

    private final ServerFacade server;
    private final String serverUrl;

    /**
     * Create a ChessClient with the given server URL.
     * Example: new ChessClient("http://localhost:8080");
     */
    public ChessClient(String serverUrl) {
        this.serverUrl = serverUrl;
        this.server = new ServerFacade(serverUrl);
    }

    /**
     * Evaluate a single line of user input, e.g.:
     *   "login alice mypassword"
     *   "list"
     *   "create BestGameEver"
     *
     * Returns a string response to display.
     * If something goes wrong, returns an error message.
     */
    public String eval(String input) {
        try {
            var tokens = input.trim().split("\\s+");
            if (tokens.length == 0) {
                return help();
            }
            var cmd = tokens[0].toLowerCase();
            var params = Arrays.copyOfRange(tokens, 1, tokens.length);

            return switch (cmd) {
                case "login"     -> doLogin(params);
                case "register"  -> doRegister(params);
                case "logout"    -> doLogout();
                case "create"    -> doCreateGame(params);
                case "list"      -> doListGames();
                case "join"      -> doJoinGame(params);
                case "observe"   -> doObserveGame(params);
                case "help"      -> help();
                case "quit"      -> "quit";
                default          -> "Unknown command. Type 'help' for options.";
            };
        } catch (RuntimeException ex) {
            // Catch all errors and show them as user-friendly messages:
            return "Error: " + ex.getMessage();
        }
    }

    // ----------------------------------------------------
    // Command Handlers
    // ----------------------------------------------------

    private String doLogin(String[] params) {
        if (params.length < 2) {
            throw new RuntimeException("Usage: login <username> <password>");
        }
        var username = params[0];
        var password = params[1];

        var authData = server.login(username, password); // throws RuntimeException if failed
        currentUser = authData;
        state = State.LOGGEDIN;
        return "Login successful for user '" + username + "'";
    }

    private String doRegister(String[] params) {
        if (params.length < 3) {
            throw new RuntimeException("Usage: register <username> <password> <email>");
        }
        var username = params[0];
        var password = params[1];
        var email    = params[2];

        currentUser = server.register(username, password, email);
        state = State.LOGGEDIN;
        return "Registration successful for user '" + username + "' (logged in now).";
    }

    private String doLogout() {
        assertLoggedIn();
        server.logout(currentUser.authToken());
        currentUser = null;
        state = State.LOGGEDOUT;
        return "Logged out successfully.";
    }

    private String doCreateGame(String[] params) {
        assertLoggedIn();
        if (params.length < 1) {
            throw new RuntimeException("Usage: create <gameName>");
        }
        var gameName = String.join(" ", params);
        server.createGame(currentUser.authToken(), gameName);
        return "Game created successfully: " + gameName;
    }

    private String doListGames() {
        assertLoggedIn();
        var games = server.listGames(currentUser.authToken());
        if (games.isEmpty()) {
            return "No games currently available.";
        }

        var sb = new StringBuilder("Available games:\n");
        for (int i = 0; i < games.size(); i++) {
            GameData game = games.get(i);
            sb.append(String.format(
                    "%d) %s - White: %s\tBlack: %s\n",
                    i + 1,
                    game.gameName(),
                    game.whiteUsername(),
                    game.blackUsername()
            ));
        }
        return sb.toString();
    }

    private String doJoinGame(String[] params) {
        assertLoggedIn();
        if (params.length < 2) {
            throw new RuntimeException("Usage: join <gameNumber> <white|black>");
        }
        int gameNumber;
        try {
            gameNumber = Integer.parseInt(params[0]) - 1;
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid game number.");
        }

        var color = params[1].toLowerCase();
        if (!color.equals("white") && !color.equals("black")) {
            throw new RuntimeException("Color must be 'white' or 'black'.");
        }

        // Retrieve fresh game list
        var games = server.listGames(currentUser.authToken());
        if (gameNumber < 0 || gameNumber >= games.size()) {
            throw new RuntimeException("Game number out of range.");
        }
        var chosenGame = games.get(gameNumber);

        server.joinGame(currentUser.authToken(), String.valueOf(chosenGame.gameID()), color);
        return "Joined game '" + chosenGame.gameName() + "' as " + color;
    }

    private String doObserveGame(String[] params) {
        assertLoggedIn();
        if (params.length < 1) {
            throw new RuntimeException("Usage: observe <gameNumber>");
        }
        int gameNumber;
        try {
            gameNumber = Integer.parseInt(params[0]) - 1;
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid game number.");
        }

        var games = server.listGames(currentUser.authToken());
        if (gameNumber < 0 || gameNumber >= games.size()) {
            throw new RuntimeException("Game number out of range.");
        }
        var chosenGame = games.get(gameNumber);

        server.observeGame(currentUser.authToken(), String.valueOf(chosenGame.gameID()));
        return "Now observing game '" + chosenGame.gameName() + "'";
    }

    private void assertLoggedIn() {
        if (state == State.LOGGEDOUT) {
            throw new RuntimeException("You must be logged in for that command.");
        }
    }

    private String help() {
        if (state == State.LOGGEDOUT) {
            return """
                Commands (LOGGED OUT):
                  login <username> <password>
                  register <username> <password> <email>
                  quit
                  help
                """;
        }
        else {
            return """
                Commands (LOGGED IN):
                  create <gameName>
                  list
                  join <gameNumber> <white|black>
                  observe <gameNumber>
                  logout
                  quit
                  help
                """;
        }
    }
}
