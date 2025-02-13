package service;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;

public class ClearService {
    private final DataAccess dao;

    public ClearService(DataAccess dao) {
        this.dao = dao;
    }

    public void clear() throws DataAccessException {
        if (dao == null) {
            throw new DataAccessException("DAO does not exist");
        }
        dao.clear();
    }
}

