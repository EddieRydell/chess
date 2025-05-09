package dataaccess;

import com.google.gson.Gson;
import model.AuthData;
import model.GameData;
import model.UserData;
import chess.ChessGame;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DBDataAccess implements DataAccess {

    private final Gson gson = new Gson();

    public DBDataAccess() throws DataAccessException {
        configureDatabase();
    }

    private final String[] createStatements = {
            """
            CREATE TABLE IF NOT EXISTS userData (
                username      VARCHAR(50) NOT NULL,
                passwordHash  VARCHAR(200) NOT NULL,
                email         VARCHAR(100),
                PRIMARY KEY (username)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
            """,
            """
            CREATE TABLE IF NOT EXISTS authData (
                authToken VARCHAR(100) PRIMARY KEY,
                username  VARCHAR(50) NOT NULL,
                FOREIGN KEY (username) REFERENCES userData(username)
                ON DELETE CASCADE
                ON UPDATE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
            """,
            """
            CREATE TABLE IF NOT EXISTS gameData (
                gameID INT PRIMARY KEY,
                gameName VARCHAR(100),
                whiteUsername VARCHAR(50),
                blackUsername VARCHAR(50),
                gameJSON TEXT NOT NULL
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
            """
    };

    public int getMaxGameID() throws DataAccessException {
        final String sql = "SELECT COALESCE(MAX(gameID), 0) AS max_id FROM gameData";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt("max_id");
            }
            else {
                return 0;
            }

        }
        catch (SQLException e) {
            throw new DataAccessException("Error retrieving max gameID: " + e.getMessage());
        }
    }

    private void configureDatabase() throws DataAccessException {
        DatabaseManager.createDatabase();
        try (var conn = DatabaseManager.getConnection()) {
            for (var statement : createStatements) {
                try (var preparedStatement = conn.prepareStatement(statement)) {
                    preparedStatement.executeUpdate();
                }
            }
        }
        catch (SQLException e) {
            throw new DataAccessException(String.format("Unable to configure database: %s", e.getMessage()));
        }
    }

    public void storeUserPassword(String username, String clearTextPassword) throws DataAccessException {
        String hashedPassword = BCrypt.hashpw(clearTextPassword, BCrypt.gensalt());

        final String sql = "UPDATE userData SET passwordHash = ? WHERE username = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, hashedPassword);
            stmt.setString(2, username);
            stmt.executeUpdate();

        }
        catch (SQLException e) {
            throw new DataAccessException("Error writing hashed password to database: " + e.getMessage());
        }
    }

    public boolean verifyUser(String username, String providedClearTextPassword) throws DataAccessException {
        UserData user = getUser(username);
        if (user == null) {
            return false;
        }

        String hashedPassword = user.password();
        return BCrypt.checkpw(providedClearTextPassword, hashedPassword);
    }

    @Override
    public void createUser(UserData user) throws DataAccessException {
        if (getUser(user.username()) != null) {
            throw new DataAccessException("User already exists");
        }

        String hashedPass = BCrypt.hashpw(user.password(), BCrypt.gensalt());

        final String sql = """
            INSERT INTO userData (username, passwordHash, email)
            VALUES (?, ?, ?)
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user.username());
            stmt.setString(2, hashedPass);
            stmt.setString(3, user.email());

            stmt.executeUpdate();

        } catch (SQLException e) {
            if (e instanceof SQLIntegrityConstraintViolationException) {
                throw new DataAccessException("User already exists: " + e.getMessage());
            }
            throw new DataAccessException("Error inserting new user row: " + e.getMessage());
        }
    }

    @Override
    public UserData getUser(String username) throws DataAccessException {
        final String sql = """
            SELECT username, passwordHash, email
            FROM userData
            WHERE username = ?
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new UserData(
                            rs.getString("username"),
                            rs.getString("passwordHash"),
                            rs.getString("email")
                    );
                }
            }

        }
        catch (SQLException e) {
            throw new DataAccessException("Error retrieving user by username: " + e.getMessage());
        }
        return null;
    }

    @Override
    public void createGame(GameData game) throws DataAccessException {
        final String sql = """
        INSERT INTO gameData (gameID, gameName, whiteUsername, blackUsername, gameJSON)
        VALUES (?, ?, ?, ?, ?)
    """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, game.gameID());
            stmt.setString(2, game.gameName());
            stmt.setString(3, game.whiteUsername());
            stmt.setString(4, game.blackUsername());
            String gameJson = gson.toJson(game.game());
            stmt.setString(5, gameJson);

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("Error inserting new game: " + e.getMessage());
        }
    }

    @Override
    public GameData getGame(int gameID) throws DataAccessException {
        final String sql = """
            SELECT gameID, gameName, whiteUsername, blackUsername, gameJSON
            FROM gameData
            WHERE gameID = ?
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, gameID);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String json = rs.getString("gameJSON");
                    ChessGame chessGame = gson.fromJson(json, ChessGame.class);

                    return new GameData(
                            rs.getInt("gameID"),
                            rs.getString("whiteUsername"),
                            rs.getString("blackUsername"),
                            rs.getString("gameName"),
                            chessGame
                    );
                }
            }

        }
        catch (SQLException e) {
            throw new DataAccessException("Error retrieving game by ID: " + e.getMessage());
        }
        return null;
    }

    @Override
    public List<GameData> listGames() throws DataAccessException {
        final String sql = """
            SELECT gameID, gameName, whiteUsername, blackUsername, gameJSON
            FROM gameData
        """;
        List<GameData> games = new ArrayList<>();

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String json = rs.getString("gameJSON");
                ChessGame chessGame = gson.fromJson(json, ChessGame.class);

                GameData gd = new GameData(
                        rs.getInt("gameID"),
                        rs.getString("whiteUsername"),
                        rs.getString("blackUsername"),
                        rs.getString("gameName"),
                        chessGame
                );
                games.add(gd);
            }

        }
        catch (SQLException e) {
            throw new DataAccessException("Error listing games: " + e.getMessage());
        }
        return games;
    }

    @Override
    public void updateGame(GameData game) throws DataAccessException {
        // Verify that the game exists.
        GameData currentGame = getGame(game.gameID());
        if (currentGame == null) {
            throw new DataAccessException("Game ID does not exist: " + game.gameID());
        }

        final String sql = """
    UPDATE gameData
    SET gameName = ?, whiteUsername = ?, blackUsername = ?, gameJSON = ?
    WHERE gameID = ?
    """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, game.gameName());
            // Use the values provided in game data—even if they're null.
            stmt.setString(2, game.whiteUsername());
            stmt.setString(3, game.blackUsername());
            String gameJson = gson.toJson(game.game());
            stmt.setString(4, gameJson);
            stmt.setInt(5, game.gameID());

            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated == 0) {
                throw new DataAccessException("Game ID does not exist: " + game.gameID());
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error updating game: " + e.getMessage());
        }
    }

    @Override
    public void createAuth(AuthData auth) throws DataAccessException {
        final String sql = """
        INSERT INTO authData (authToken, username)
        VALUES (?, ?)
    """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, auth.authToken());
            stmt.setString(2, auth.username());
            stmt.executeUpdate();

        }
        catch (SQLException e) {
            if (e instanceof SQLIntegrityConstraintViolationException) {
                throw new DataAccessException("Cannot create auth. " + e.getMessage());
            }
            throw new DataAccessException("Error inserting auth token: " + e.getMessage());
        }
    }

    @Override
    public AuthData getAuth(String authToken) throws DataAccessException {
        final String sql = """
            SELECT authToken, username
            FROM authData
            WHERE authToken = ?
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, authToken);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new AuthData(
                            rs.getString("authToken"),
                            rs.getString("username")
                    );
                }
            }

        }
        catch (SQLException e) {
            throw new DataAccessException("Error retrieving auth token: " + e.getMessage());
        }

        return null;
    }

    @Override
    public void deleteAuth(String authToken) throws DataAccessException {
        final String sql = "DELETE FROM authData WHERE authToken = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, authToken);
            stmt.executeUpdate();

        }
        catch (SQLException e) {
            throw new DataAccessException("Error deleting auth token: " + e.getMessage());
        }
    }

    @Override
    public void clear() throws DataAccessException {
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("DELETE FROM gameData");
            stmt.executeUpdate("DELETE FROM authData");
            stmt.executeUpdate("DELETE FROM userData");

        }
        catch (SQLException e) {
            throw new DataAccessException("Error clearing tables: " + e.getMessage());
        }
    }
}
