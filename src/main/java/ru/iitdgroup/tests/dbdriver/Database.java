package ru.iitdgroup.tests.dbdriver;

import ru.iitdgroup.tests.properties.TestProperties;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Database implements AutoCloseable {

    private final TestProperties props;
    Connection conn;
    List<String> selectFields = new ArrayList<>();
    String from;
    List<String> whereConditions = new ArrayList<>();
    String formula;
    List<String> joins = new ArrayList<>();

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

        String fatFormula = whereConditions
                .stream()
                .map(c -> "(" + c + ")")
                .collect(Collectors.joining(" " + formula + " "));

        StringBuilder sb = new StringBuilder();
        sb
                .append("SELECT").append(" ")
                .append(String.join(", ", selectFields)).append(" ")
                .append(" FROM ").append(from).append(" ")
                .append(String.join("\n", joins))
                .append(" WHERE ").append(fatFormula);

        return SQLUtil.getSQLData(conn, sb.toString());
    }

    public void setFormula(String formula) {
        checkFormula(formula);
        this.formula = formula;
    }

    public List<String> getJoins() {
        return joins;
    }

    public void setJoins(List<String> joins) {
        this.joins = joins;
    }

    public String[][] getSQLData(String sql){
        try {
            return SQLUtil.getSQLData( conn, sql);
        } catch (SQLException e) {
            throw new RuntimeException( SQLUtil.explainSQLException(e),e);
        }
    }
}
