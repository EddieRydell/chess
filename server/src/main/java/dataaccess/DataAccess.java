package dataaccess;

import model.AuthData;
import model.GameData;
import model.UserData;

import java.util.List;

public interface DataAccess {
    void createUser(UserData user) throws DataAccessException;
    UserData getUser(String username) throws DataAccessException;

    void createGame(GameData game) throws DataAccessException;
    GameData getGame(int gameID) throws DataAccessException;
    List<GameData> listGames() throws DataAccessException;
    void updateGame(GameData game) throws DataAccessException;

    void createAuth(AuthData auth) throws DataAccessException;
    AuthData getAuth(String authToken) throws DataAccessException;
    void deleteAuth(String authToken) throws DataAccessException;

    void clear() throws DataAccessException;

    void storeUserPassword(String username, String password) throws DataAccessException;

    boolean verifyUser(String username, String password) throws DataAccessException;
}
