package service;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import model.AuthData;
import model.UserData;
import service.requests.RegisterRequest;
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
}
