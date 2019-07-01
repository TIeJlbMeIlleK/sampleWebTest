package ru.iitdgroup.tests.dbdriver;

import java.sql.SQLException;

public class With extends Context {
    protected With(Database database) {
        super(database);
    }

    public With with(String fieldName, String operator, String value) {
        database.whereConditions.add(String.join(" ", fieldName, operator, value));
        return this;
    }

    public With setFormula(String formula){
        database.setFormula( formula);
        return this;
    }

    public String[][] get() throws SQLException {
        return database.getData();
    }

    public With sort(String fieldName, boolean asc) {
        Sorts sorts = new Sorts(database);
        sorts.sort(fieldName, asc);
        return this;
    }

    public With limit(int limit) {
        Limit r = new Limit(database);
        r.limit(limit);
        return this;
    }

    public With limit(int offset, int limit) {
        Limit r = new Limit(database);
        r.limit(offset, limit);
        r.limit(limit);
        return this;
    }
}
