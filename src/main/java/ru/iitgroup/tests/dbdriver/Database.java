package ru.iitgroup.tests.dbdriver;

import ru.iitgroup.tests.properties.TestProperties;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Database implements AutoCloseable {

    final TestProperties props;
    Connection conn;
    List<String> selectFields = new ArrayList<>();
    String from;
    List<String> whereConditions = new ArrayList<>();
    String formula;

    public Database(TestProperties props) {
        this.props = props;
        try {
            conn = SQLUtil.connect(props);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Fields select() {
        selectFields.clear();
        from = null;
        whereConditions.clear();
        return new Fields(this);
    }

    @Override
    public void close() throws Exception {
        if (conn != null && !conn.isClosed()) conn.close();
    }

    protected void checkFormula(String formula) {
        String[] tokens = formula.split("\\s+");
        for (String token : tokens) {
            if (token.matches("\\d+")) {
                if (whereConditions.size() < Integer.parseInt(token))
                    throw new IllegalStateException("Неправильная формула - для неё не хватает условий");
            }
        }

    }

    public Connection getConn() {
        return conn;
    }

    public String[][] getData() throws SQLException {

        String fatFormula = formula;
        for (int i = 0; i < whereConditions.size(); i++) {
            String whereCondition = whereConditions.get(i);
            fatFormula = fatFormula.replaceAll(String.valueOf(i + 1), " (" + whereCondition + ") ");
        }


        StringBuilder sb = new StringBuilder();
        sb
                .append("SELECT").append(" ")
                .append(String.join(", ", selectFields)).append(" ")
                .append(" FROM ").append(from)
                .append(" WHERE ").append(fatFormula);

        return SQLUtil.getSQLData(conn, sb.toString());
    }

    public void setFormula(String formula) {
        checkFormula(formula);
        this.formula = formula;
    }
}
