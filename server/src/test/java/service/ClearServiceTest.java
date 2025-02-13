package service;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import dataaccess.MemoryDataAccess;
import model.UserData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClearServiceTest {
    private DataAccess dao;
    private ClearService clearService;

    @BeforeEach
    void setup() throws DataAccessException {

    }

    @Test
    void testClearSuccess() throws DataAccessException {
        dao = new MemoryDataAccess();

        clearService = new ClearService(dao);

        dao.createUser(new UserData("fred", "pwd", "f@byu.edu"));

        assertNotNull(dao.getUser("fred"));

        clearService.clear();

        assertNull(dao.getUser("fred"), "User data should be cleared");
    }

    @Test
    void testClearFailure() throws DataAccessException {
        // Only possible failure for clearing would be if there is no DAO
        dao = null;
        clearService = new ClearService(dao);
        assertThrows(DataAccessException.class,
                () -> clearService.clear(),
                "Expected exception for when clearing null DAO");
    }
}
