package ru.iitgroup.tests.dbdriver;

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
        return  this;
    }

    public String[][] get() throws SQLException {
        return database.getData();
    }

}
