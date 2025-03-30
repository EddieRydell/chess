package ui;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import chess.ChessBoard;
import chess.ChessPiece;
import chess.ChessPosition;
import model.AuthData;
import model.GameData;
import server.ServerFacade;

import static chess.ChessGame.TeamColor.BLACK;
import static chess.ChessGame.TeamColor.WHITE;

public class ChessClient {

    private enum State {
        LOGGEDOUT,
        LOGGEDIN
    }

    private static final List<String> VALID_COMMANDS = List.of(
            "login", "register", "logout", "create", "list",
            "join", "observe", "help", "quit"
    );

    private State state = State.LOGGEDOUT;
    private AuthData currentUser = null;

    private final ServerFacade server;

    public ChessClient(String serverUrl) {
        this.server = new ServerFacade(serverUrl);
    }

    private String friendlyErrorMessage(String rawMessage) {
        String lower = rawMessage.toLowerCase();

        if (lower.contains("401") || lower.contains("unauthorized")) {
            return "Unauthorized. Please check your credentials or log in first.";
        }
        else if (lower.contains("403") || lower.contains("already taken")) {
            return "That action is not allowed (game already taken or no permission).";
        }
        else if (lower.contains("out of range")) {
            return "That choice is out of range.";
        }
        else {
            return "An unexpected error occurred. Try again";
        }
    }

    public String eval(String input) {
        try {
            var tokens = input.trim().split("\\s+");
            if (tokens.length == 0) {
                return help();
            }
            var cmdPartial = tokens[0].toLowerCase();
            String matchedCmd = matchCommand(cmdPartial);

            if (matchedCmd == null) {
                return "Unknown or ambiguous command. Type 'help' for options.";
            }

            var params = Arrays.copyOfRange(tokens, 1, tokens.length);

            return switch (matchedCmd) {
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
        }
        catch (RuntimeException ex) {
            String rawMessage = ex.getMessage();

            String friendlyMessage = friendlyErrorMessage(rawMessage);

            return "Error: " + friendlyMessage;
        }
    }

    private String matchCommand(String partial) {
        var matches = VALID_COMMANDS.stream()
                .filter(cmd -> cmd.startsWith(partial))
                .toList();

        if (matches.size() == 1) {
            return matches.get(0);
        }
        return null;
    }

    private String doLogin(String[] params) {
        if (params.length < 2) {
            throw new RuntimeException("Usage: login <username> <password>");
        }
        var username = params[0];
        var password = params[1];

        currentUser = server.login(username, password);
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

    private String getPieceEmoji(ChessPiece piece) {
        return switch (piece.getTeamColor()) {
            case WHITE -> switch (piece.getPieceType()) {
                case KING -> EscapeSequences.WHITE_KING;
                case QUEEN -> EscapeSequences.WHITE_QUEEN;
                case BISHOP -> EscapeSequences.WHITE_BISHOP;
                case KNIGHT -> EscapeSequences.WHITE_KNIGHT;
                case ROOK -> EscapeSequences.WHITE_ROOK;
                case PAWN -> EscapeSequences.WHITE_PAWN;
            };
            case BLACK -> switch (piece.getPieceType()) {
                case KING -> EscapeSequences.BLACK_KING;
                case QUEEN -> EscapeSequences.BLACK_QUEEN;
                case BISHOP -> EscapeSequences.BLACK_BISHOP;
                case KNIGHT -> EscapeSequences.BLACK_KNIGHT;
                case ROOK -> EscapeSequences.BLACK_ROOK;
                case PAWN -> EscapeSequences.BLACK_PAWN;
            };
        };
    }

    private String drawBoard(ChessBoard board, String perspective) {
        StringBuilder sb = new StringBuilder();
        boolean isWhite = perspective.equalsIgnoreCase("WHITE");

        // Column labels (files)
        String[] files = {"a", "b", "c", "d", "e", "f", "g", "h"};
        if (!isWhite) {
            Collections.reverse(Arrays.asList(files));
        }

        // Print top column labels
        sb.append("  ");
        for (String file : files) {
            sb.append(" ").append(file).append(" ");
        }
        sb.append("\n");

        for (int r = 0; r < 8; r++) {
            int actualRow = isWhite ? 8 - r : r + 1;
            sb.append(actualRow).append(" ");

            for (int c = 0; c < 8; c++) {
                int actualCol = isWhite ? c + 1 : 8 - c;

                ChessPiece piece = board.getPiece(new ChessPosition(actualRow, actualCol));

                boolean isDarkSquare = (actualRow + actualCol) % 2 == 0;
                String bgColor = isDarkSquare
                        ? EscapeSequences.SET_BG_COLOR_DARK_GREY
                        : EscapeSequences.SET_BG_COLOR_LIGHT_GREY;

                String pieceStr = EscapeSequences.EMPTY;
                if (piece != null) {
                    pieceStr = getPieceEmoji(piece);
                }

                sb.append(bgColor).append(pieceStr).append(EscapeSequences.RESET_BG_COLOR);
            }
            sb.append(" ").append(actualRow).append("\n");
        }

        sb.append("  ");
        for (String file : files) {
            sb.append(" ").append(file).append(" ");
        }
        sb.append("\n");

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

        var games = server.listGames(currentUser.authToken());
        if (gameNumber < 0 || gameNumber >= games.size()) {
            throw new RuntimeException("Game number out of range.");
        }
        var chosenGame = games.get(gameNumber);

        server.joinGame(currentUser.authToken(), chosenGame.gameID(), color);

        GameData updatedGame = server.getGame(currentUser.authToken(), chosenGame.gameID());

        String boardString = drawBoard(updatedGame.game().getBoard(), color);

        return "Joined game '" + chosenGame.gameName() + "' as " + color + "\n\n" + boardString;
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

        GameData data = server.getGame(currentUser.authToken(), chosenGame.gameID());

        String boardString = drawBoard(data.game().getBoard(), "white");

        return String.format("Now observing game '%s' (ID %d)\n%s",
                data.gameName(),
                data.gameID(),
                boardString
        );
    }


    private void assertLoggedIn() {
        if (state == State.LOGGEDOUT) {
            throw new RuntimeException("You must be logged in for that command.");
        }
    }

    public String help() {
        if (state == State.LOGGEDOUT) {
            return """
                Commands (LOGGED OUT):
                  login <username> <password>
                  register <username> <password> <email>
                  quit
                  help
                """;
        } else {
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
