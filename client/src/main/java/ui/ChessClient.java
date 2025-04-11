package ui;

import java.util.*;

import chess.ChessBoard;
import chess.ChessGame;
import chess.ChessPiece;
import chess.ChessPosition;
import model.AuthData;
import model.GameData;
import websocket.messages.ServerMessage;

import static ui.EscapeSequences.RESET_TEXT_COLOR;
import static ui.EscapeSequences.SET_TEXT_COLOR_BLUE;

public class ChessClient {

    private String playerColor = "white";

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
    private Integer currentGameID = null;

    private final ServerFacade server;

    public ChessClient(String serverUrl) {
        this.server = new ServerFacade(serverUrl);
        server.setClient(this);
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
            String trimmedInput = input.trim();
            if (trimmedInput.isEmpty()) {
                return help();
            }

            String[] tokens = trimmedInput.split("\\s+");
            String command = tokens[0].toLowerCase();
            String[] params = Arrays.copyOfRange(tokens, 1, tokens.length);

            return switch (command) {
                case "move" -> handleMoveCommand(params);
                case "resign" -> handleResignCommand();
                case "leave" -> handleLeaveCommand();
                case "redraw" -> doRedraw();
                case "highlight" -> doHighlightCommand(params); // <-- NEW highlight command
                case "login" -> doLogin(params);
                case "register" -> doRegister(params);
                case "logout" -> doLogout();
                case "create" -> doCreateGame(params);
                case "list" -> doListGames();
                case "join" -> doJoinGame(params);
                case "observe" -> doObserveGame(params);
                case "help" -> help();
                case "quit" -> "quit";
                default -> "Unknown or ambiguous command. Type 'help' for options.";
            };
        } catch (RuntimeException ex) {
            String rawMessage = ex.getMessage();
            String friendlyMessage = friendlyErrorMessage(rawMessage);
            return "Error: " + friendlyMessage /* + " Raw Message: " + rawMessage*/ ;
        }
    }

    private String handleMoveCommand(String[] params) {
        chess.ChessMove move;
        // Algebraic notation: move e2 e4
        if (params.length == 2) {
            chess.ChessPosition start = parseAlgebraic(params[0]);
            chess.ChessPosition end = parseAlgebraic(params[1]);
            if (start == null || end == null) {
                return "Usage: move <startSquare> <endSquare> (e.g., move e2 e4)";
            }
            move = new chess.ChessMove(start, end, null);
        }
        // Numeric coordinates: move 2 5 3 5
        else if (params.length == 4) {
            try {
                int startRow = Integer.parseInt(params[0]);
                int startCol = Integer.parseInt(params[1]);
                int endRow = Integer.parseInt(params[2]);
                int endCol = Integer.parseInt(params[3]);
                move = new chess.ChessMove(
                        new chess.ChessPosition(startRow, startCol),
                        new chess.ChessPosition(endRow, endCol),
                        null);
            }
            catch (NumberFormatException ex) {
                return "Usage: move <startRow> <startCol> <endRow> <endCol>";
            }
        }
        else {
            return "Usage: move <startRow> <startCol> <endRow> <endCol>";
        }

        if (currentGameID == null) {
            return "You are not in a game.";
        }
        server.sendMakeMove(currentUser.authToken(), currentGameID, move);
        return "Move command sent.";
    }

    private String handleResignCommand() {
        if (currentGameID == null) {
            return "You are not in a game.";
        }
        server.sendResign(currentUser.authToken(), currentGameID);
        return "Resign command sent.";
    }

    private String handleLeaveCommand() {
        if (currentGameID == null) {
            return "You are not in a game.";
        }
        server.sendLeave(currentUser.authToken(), currentGameID);
        currentGameID = null;
        return "Leave command sent.";
    }

    /**
     * Helper function to parse algebraic notation, e.g., "e2"
     */
    private ChessPosition parseAlgebraic(String square) {
        if (square == null || square.length() != 2) {
            return null;
        }
        char fileChar = Character.toLowerCase(square.charAt(0));
        char rankChar = square.charAt(1);
        if (fileChar < 'a' || fileChar > 'h') {
            return null;
        }
        if (rankChar < '1' || rankChar > '8') {
            return null;
        }
        int col = fileChar - 'a' + 1;
        int row = rankChar - '0';
        return new chess.ChessPosition(row, col);
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

    private String doHighlightCommand(String[] params) {
        if (params.length != 1) {
            return "Usage: highlight <square> (e.g., highlight e2)";
        }
        ChessPosition selectedPos = parseAlgebraic(params[0]);
        if (selectedPos == null) {
            return "Invalid square. Please use algebraic notation (e.g., e2).";
        }
        if (currentGameID == null) {
            return "You are not in a game.";
        }

        GameData data = server.getGame(currentUser.authToken(), currentGameID);
        if (data == null) {
            return "Unable to retrieve game data.";
        }
        ChessBoard board = data.game().getBoard();
        ChessGame game = data.game();

        var legalMoves = game.validMoves(selectedPos);
        if (legalMoves == null) {
            return "No piece at the selected square.";
        }
        if (legalMoves.isEmpty()) {
            return "The piece at the selected square has no legal moves.";
        }

        Set<ChessPosition> highlightSquares = new HashSet<>();
        highlightSquares.add(selectedPos);
        for (var move : legalMoves) {
            highlightSquares.add(move.getEndPosition());
        }

        return drawBoardWithHighlights(board, playerColor, selectedPos, highlightSquares);
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

    private String drawBoardWithHighlights(ChessBoard board, String perspective, ChessPosition selected, Set<ChessPosition> highlights) {
        StringBuilder sb = new StringBuilder();
        boolean isWhite = perspective.equalsIgnoreCase("WHITE");

        String[] files = {"a", "b", "c", "d", "e", "f", "g", "h"};
        if (!isWhite) {
            Collections.reverse(Arrays.asList(files));
        }
        sb.append(SET_TEXT_COLOR_BLUE + "\n  ");
        for (String file : files) {
            sb.append(" ").append(file).append("  ");
        }
        sb.append("\n");

        String selectedColor = EscapeSequences.SET_BG_COLOR_YELLOW;
        String legalMoveColor = EscapeSequences.SET_BG_COLOR_GREEN;

        for (int r = 0; r < 8; r++) {
            int actualRow = isWhite ? 8 - r : r + 1;
            sb.append(actualRow).append(" ");

            for (int c = 0; c < 8; c++) {
                int actualCol = isWhite ? c + 1 : 8 - c;
                ChessPosition pos = new ChessPosition(actualRow, actualCol);
                ChessPiece piece = board.getPiece(pos);

                boolean isDarkSquare = (actualRow + actualCol) % 2 == 0;
                String normalBgColor = isDarkSquare
                        ? EscapeSequences.SET_BG_COLOR_DARK_GREY
                        : EscapeSequences.SET_BG_COLOR_LIGHT_GREY;

                String cellBg = normalBgColor;
                if (highlights != null && highlights.contains(pos)) {
                    if (pos.equals(selected)) {
                        cellBg = selectedColor;
                    } else {
                        cellBg = legalMoveColor;
                    }
                }

                String pieceStr = EscapeSequences.EMPTY;
                if (piece != null) {
                    pieceStr = getPieceEmoji(piece);
                }

                sb.append(cellBg).append(pieceStr).append(EscapeSequences.RESET_BG_COLOR);
            }
            sb.append(" ").append(actualRow).append("\n");
        }

        sb.append("  ");
        for (String file : files) {
            sb.append(" ").append(file).append("  ");
        }
        sb.append("\n" + RESET_TEXT_COLOR);
        return sb.toString();
    }

    private String drawBoard(ChessBoard board, String perspective) {
        String[] files = {"a", "b", "c", "d", "e", "f", "g", "h"};
        boolean isWhite = perspective.equalsIgnoreCase("WHITE");
        if (!isWhite) {
            Collections.reverse(Arrays.asList(files));
        }

        StringBuilder sb = new StringBuilder();
        sb.append(SET_TEXT_COLOR_BLUE + "\n  ");
        for (String file : files) {
            sb.append(" ").append(file).append("  ");
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
            sb.append(" ").append(file).append("  ");
        }
        sb.append("\n" + RESET_TEXT_COLOR);

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
        // Open WebSocket connection for gameplay:
        server.connectToGame(currentUser.authToken(), chosenGame.gameID());
        currentGameID = chosenGame.gameID();

        GameData updatedGame = server.getGame(currentUser.authToken(), chosenGame.gameID());
        String boardString = drawBoard(updatedGame.game().getBoard(), color);
        playerColor = color;
        return "Joined game '" + chosenGame.gameName() + "' as " + color + "\n\n" + boardString;
    }

    private String doObserveGame(String[] params) {
        playerColor = "white";
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

        server.connectToGame(currentUser.authToken(), chosenGame.gameID());
        currentGameID = chosenGame.gameID();

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

    private String doRedraw() {
        assertLoggedIn();
        if (currentGameID == null) {
            return "You are not in a game.";
        }
        GameData data = server.getGame(currentUser.authToken(), currentGameID);
        if (data == null) {
            return "Unable to retrieve game data.";
        }
        return drawBoard(data.game().getBoard(), playerColor);
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
                  redraw
                  highlight <square>
                  logout
                  quit
                  help
                  move <startRow> <startCol> <endRow> <endCol>
                  resign
                  leave
                """;
        }
    }

    public void handleServerMessage(websocket.messages.ServerMessage message) {
        switch (message.getServerMessageType()) {
            case LOAD_GAME:
                if (message.game != null) {
                    ChessGame updatedGame = message.game;
                    String boardStr = drawBoard(updatedGame.getBoard(), playerColor);
                    System.out.println(boardStr);
                } else {
                    System.out.println("Received LOAD_GAME update but game state is missing.");
                }
                break;
            case NOTIFICATION:
                System.out.println("NOTIFICATION: " + message.message);
                if (message.message.toLowerCase().contains("resigned")) {
                    System.out.println("Game over. No further moves allowed.");
                }
                break;
            case ERROR:
                System.err.println("ERROR: " + message.errorMessage);
                break;
        }
    }
}
