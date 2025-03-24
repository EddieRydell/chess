package ui;

import server.Server;

import model.AuthData;
import model.GameData;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Main entry point for the Chess Client application (console-based).
 */
public class Main {

    // --------------------------------------------------
    // State variables & fields
    // --------------------------------------------------
    private static final Scanner sc = new Scanner(System.in);
    private static ServerFacade facade;

    private static boolean isLoggedIn = false;
    private static AuthData currentUser = null;
    private static List<GameData> lastRetrievedGames = new ArrayList<>();

    // --------------------------------------------------
    // main() method: Start the console UI loop
    // --------------------------------------------------
    public static void main(String[] args) {
        // Adjust the port or URL as needed. Possibly read from args, if desired.
        facade = new ServerFacade(8080);

        boolean running = true;
        while (running) {
            if (!isLoggedIn) {
                // === Prelogin UI ===
                System.out.println("\n=== Prelogin ===");
                System.out.print("> ");
                String command = sc.nextLine().trim().toLowerCase();

                switch (command) {
                    case "help":
                        printPreloginHelp();
                        break;
                    case "login":
                        handleLogin();
                        break;
                    case "register":
                        handleRegister();
                        break;
                    case "quit":
                        running = false;
                        break;
                    default:
                        System.out.println("Unrecognized command. Type 'help' for options.");
                }
            }
            else {
                // === Postlogin UI ===
                System.out.println("\n=== Postlogin ===");
                System.out.print("> ");
                String command = sc.nextLine().trim().toLowerCase();

                switch (command) {
                    case "help":
                        printPostloginHelp();
                        break;
                    case "logout":
                        handleLogout();
                        break;
                    case "create game":
                        handleCreateGame();
                        break;
                    case "list games":
                        handleListGames();
                        break;
                    case "play game":
                        handlePlayGame();
                        break;
                    case "observe game":
                        handleObserveGame();
                        break;
                    default:
                        System.out.println("Unrecognized command. Type 'help' for options.");
                }
            }
        }

        System.out.println("Exiting program.");
    }

    // --------------------------------------------------
    // Prelogin / Postlogin HELPERS
    // --------------------------------------------------

    private static void printPreloginHelp() {
        System.out.println("Available commands (Prelogin):");
        System.out.println("  help      - Display this help text");
        System.out.println("  login     - Log in with an existing account");
        System.out.println("  register  - Create a new account");
        System.out.println("  quit      - Exit the program");
    }

    private static void printPostloginHelp() {
        System.out.println("Available commands (Postlogin):");
        System.out.println("  help          - Display this help text");
        System.out.println("  logout        - Log out of the current account");
        System.out.println("  create game   - Create a new game on the server");
        System.out.println("  list games    - List all available games on the server");
        System.out.println("  play game     - Join a game as white or black");
        System.out.println("  observe game  - Observe a game (view only)");
    }

    // --------------------------------------------------
    // Command Handlers
    // --------------------------------------------------

    private static void handleLogin() {
        System.out.print("Enter username: ");
        String username = sc.nextLine();
        System.out.print("Enter password: ");
        String password = sc.nextLine();

        try {
            AuthData data = facade.login(username, password);
            currentUser = data;
            isLoggedIn = true;
            System.out.println("Login successful!");
        } catch (Exception e) {
            System.out.println("Login failed: " + e.getMessage());
        }
    }

    private static void handleRegister() {
        System.out.print("Enter username: ");
        String username = sc.nextLine();
        System.out.print("Enter password: ");
        String password = sc.nextLine();
        System.out.print("Enter email: ");
        String email = sc.nextLine();

        try {
            AuthData data = facade.register(username, password, email);
            currentUser = data;
            isLoggedIn = true;
            System.out.println("Registration and login successful!");
        } catch (Exception e) {
            System.out.println("Registration failed: " + e.getMessage());
        }
    }

    private static void handleLogout() {
        if (currentUser == null) {
            System.out.println("You are not logged in.");
            return;
        }
        try {
            facade.logout(currentUser.authToken());
            currentUser = null;
            isLoggedIn = false;
            lastRetrievedGames.clear();
            System.out.println("Logged out successfully.");
        } catch (Exception e) {
            System.out.println("Logout failed: " + e.getMessage());
        }
    }

    private static void handleCreateGame() {
        System.out.print("Enter a name for the new game: ");
        String gameName = sc.nextLine();
        try {
            facade.createGame(currentUser.authToken(), gameName);
            System.out.println("Game created successfully.");
        } catch (Exception e) {
            System.out.println("Failed to create game: " + e.getMessage());
        }
    }

    private static void handleListGames() {
        try {
            lastRetrievedGames = facade.listGames(currentUser.authToken());
            if (lastRetrievedGames.isEmpty()) {
                System.out.println("No games currently available.");
                return;
            }
            System.out.println("List of games:");
            for (int i = 0; i < lastRetrievedGames.size(); i++) {
                GameData game = lastRetrievedGames.get(i);
                // Print game info. Adjust if your GameData differs
                System.out.printf("%d) %s - Players: %s\n",
                        i + 1,
                        game.getName(),
                        game.getPlayerNamesString()
                );
            }
        } catch (Exception e) {
            System.out.println("Failed to list games: " + e.getMessage());
        }
    }

    private static void handlePlayGame() {
        if (lastRetrievedGames.isEmpty()) {
            System.out.println("No games are available. Try 'list games' first.");
            return;
        }
        System.out.print("Enter the game number you want to join: ");
        String line = sc.nextLine();
        int gameIndex;
        try {
            gameIndex = Integer.parseInt(line) - 1;
        } catch (NumberFormatException e) {
            System.out.println("Invalid number. Command aborted.");
            return;
        }
        if (gameIndex < 0 || gameIndex >= lastRetrievedGames.size()) {
            System.out.println("Game number out of range. Command aborted.");
            return;
        }

        GameData chosenGame = lastRetrievedGames.get(gameIndex);

        System.out.print("Which color do you want to play? (white or black): ");
        String color = sc.nextLine().trim().toLowerCase();
        if (!color.equals("white") && !color.equals("black")) {
            System.out.println("Invalid color choice. Command aborted.");
            return;
        }

        try {
            facade.joinGame(currentUser.authToken(), chosenGame.getId(), color);
            System.out.println("Joined game successfully as " + color + ".");
            // Draw initial board from the chosen perspective
            drawChessBoard(color);
        } catch (Exception e) {
            System.out.println("Failed to join game: " + e.getMessage());
        }
    }

    private static void handleObserveGame() {
        if (lastRetrievedGames.isEmpty()) {
            System.out.println("No games are available to observe. Try 'list games' first.");
            return;
        }
        System.out.print("Enter the game number you want to observe: ");
        String line = sc.nextLine();
        int gameIndex;
        try {
            gameIndex = Integer.parseInt(line) - 1;
        } catch (NumberFormatException e) {
            System.out.println("Invalid number. Command aborted.");
            return;
        }
        if (gameIndex < 0 || gameIndex >= lastRetrievedGames.size()) {
            System.out.println("Game number out of range. Command aborted.");
            return;
        }

        GameData chosenGame = lastRetrievedGames.get(gameIndex);

        try {
            facade.observeGame(currentUser.authToken(), chosenGame.getId());
            System.out.println("Observing game: " + chosenGame.getName());
            // By requirement, if you observe as an observer, draw from White perspective
            drawChessBoard("white");
        } catch (Exception e) {
            System.out.println("Failed to observe game: " + e.getMessage());
        }
    }

    // --------------------------------------------------
    // Chessboard Drawing
    // --------------------------------------------------

    private static void drawChessBoard(String perspective) {
        // Hardcode the initial arrangement for demonstration
        char[][] initialBoard = {
                {'r','n','b','q','k','b','n','r'},
                {'p','p','p','p','p','p','p','p'},
                {'.','.','.','.','.','.','.','.'},
                {'.','.','.','.','.','.','.','.'},
                {'.','.','.','.','.','.','.','.'},
                {'.','.','.','.','.','.','.','.'},
                {'P','P','P','P','P','P','P','P'},
                {'R','N','B','Q','K','B','N','R'}
        };

        // Choose row and column iteration order depending on perspective
        int[] rowOrder = perspective.equals("white")
                ? new int[]{7,6,5,4,3,2,1,0}
                : new int[]{0,1,2,3,4,5,6,7};

        int[] colOrder = perspective.equals("white")
                ? new int[]{0,1,2,3,4,5,6,7}
                : new int[]{7,6,5,4,3,2,1,0};

        // Print the board
        for (int rowIndex : rowOrder) {
            // Rank label
            int rankLabel = perspective.equals("white")
                    ? rowIndex + 1
                    : 8 - rowIndex;

            System.out.printf("%d ", rankLabel);

            for (int colIndex : colOrder) {
                char piece = initialBoard[rowIndex][colIndex];
                boolean isLightSquare = ((rowIndex + colIndex) % 2 == 0);
                // Example: [piece] for light squares, {piece} for dark squares
                String square = isLightSquare ? "[" + piece + "]" : "{" + piece + "}";
                System.out.print(square + " ");
            }
            System.out.println();
        }

        // File labels across the bottom
        System.out.print("  ");
        if (perspective.equals("white")) {
            for (char file = 'a'; file <= 'h'; file++) {
                System.out.print(" " + file + "  ");
            }
        } else {
            for (char file = 'h'; file >= 'a'; file--) {
                System.out.print(" " + file + "  ");
            }
        }
        System.out.println("\n");
    }
}
