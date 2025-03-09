package java.dataaccess;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import dataaccess.MemoryDataAccess;
import model.UserData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import service.UserService;
import service.requests.LoginRequest;
import service.requests.LogoutRequest;
import service.requests.RegisterRequest;
import service.results.LoginResult;
import service.results.LogoutResult;
import service.results.RegisterResult;

import static org.junit.jupiter.api.Assertions.*;

class UserServiceTests {

    private DataAccess dao;
    private UserService userService;


    @BeforeEach
    void setup() {
        dao = new MemoryDataAccess();
        userService = new UserService(dao);
    }

    @Test
    void testRegisterSuccess() throws DataAccessException {
        RegisterRequest request = new RegisterRequest("alice", "password", "alice@byu.edu");
        RegisterResult result = userService.register(request);

        assertNotNull(result);
        assertEquals("alice", result.username());
        assertNotNull(result.authToken());
        assertFalse(result.authToken().isEmpty());

        UserData userInDao = dao.getUser("alice");
        assertNotNull(userInDao);
        assertEquals("alice", userInDao.username());
    }

    @Test
    void testRegisterDuplicateUser() throws DataAccessException {
        dao.createUser(new UserData("bob", "secret", "bob@byu.edu")); // Pre-load user

        RegisterRequest request = new RegisterRequest("bob", "secret", "bob@byu.edu");
        assertThrows(DataAccessException.class, () -> userService.register(request),
                "Expected an exception when registering a duplicate user");
    }

    @Test
    void testLoginSuccess() throws DataAccessException {
        userService.register(new RegisterRequest("charlie", "password", "c@byu.edu"));

        LoginRequest request = new LoginRequest("charlie", "password");
        LoginResult result = userService.login(request);

        assertNotNull(result);
        assertEquals("charlie", result.username());
        assertNotNull(result.authToken());
        assertFalse(result.authToken().isEmpty());
    }

    @Test
    void testLoginWrongPassword() throws DataAccessException {
        userService.register(new RegisterRequest("alice", "abcd", "a@byu.edu"));

        LoginRequest badPassword = new LoginRequest("alice", "abce");
        assertThrows(DataAccessException.class, () -> userService.login(badPassword),
                "Expected an exception for invalid password");
    }

    @Test
    void testLogoutSuccess() throws DataAccessException {
        userService.register(new RegisterRequest("aaa", "123", "aaa@byu.edu"));
        LoginResult loginResult = userService.login(new LoginRequest("aaa", "123"));

        LogoutResult logoutResult = userService.logout(new LogoutRequest(loginResult.authToken()));
        assertNotNull(logoutResult);

        assertThrows(DataAccessException.class,
                () -> userService.logout(new LogoutRequest(loginResult.authToken())),
                "Expected logout to fail");
    }

    @Test
    void testLogoutInvalidToken() {
        LogoutRequest badReq = new LogoutRequest("bad-token");
        assertThrows(DataAccessException.class, () -> userService.logout(badReq),
                "Expected invalid token");
    }
}
