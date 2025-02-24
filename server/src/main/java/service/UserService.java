package service;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import model.AuthData;
import model.UserData;
import org.mindrot.jbcrypt.BCrypt;
import service.requests.LoginRequest;
import service.requests.LogoutRequest;
import service.requests.RegisterRequest;
import service.results.LoginResult;
import service.results.LogoutResult;
import service.results.RegisterResult;

import java.util.UUID;

public class UserService {
    private final DataAccess dao;

    public UserService(DataAccess dao) {
        this.dao = dao;
    }

    public RegisterResult register(RegisterRequest request) throws DataAccessException {
        if (request.username() == null || request.password() == null || request.email() == null) {
            throw new IllegalArgumentException("Missing required fields");
        }

        var user = new UserData(request.username(), request.password(), request.email());
        dao.createUser(user);

        String token = UUID.randomUUID().toString();
        var authData = new AuthData(token, request.username());
        dao.createAuth(authData);

        return new RegisterResult(request.username(), token);
    }

    public LoginResult login(LoginRequest request) throws DataAccessException {
        if (request.username() == null || request.password() == null) {
            throw new IllegalArgumentException("Missing username or password");
        }

        UserData user = dao.getUser(request.username());
        if (user == null) {
            throw new DataAccessException("Invalid username or password");
        }

        boolean valid = BCrypt.checkpw(request.password(), user.password());
        if (!valid) {
            throw new DataAccessException("Invalid username or password");
        }

        String token = UUID.randomUUID().toString();
        AuthData authData = new AuthData(token, user.username());
        dao.createAuth(authData);

        return new LoginResult(user.username(), token);
    }

    public LogoutResult logout(LogoutRequest request) throws DataAccessException {
        var authData = dao.getAuth(request.authToken());
        if (authData == null) {
            throw new DataAccessException("Invalid or expired auth token");
        }

        dao.deleteAuth(request.authToken());

        return new LogoutResult();
    }
}
