package ru.iitgroup.tests.dbdriver;

import ru.iitgroup.tests.properties.TestProperties;

import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by VGoudkov on 14.06.2017.
 */
public class SQLUtil {

    public static NClob copyNClob(Connection insertConn, NClob nclob) throws SQLException, IOException {
        NClob iNClob = insertConn.createNClob();
        copyLarge(nclob.getCharacterStream(), iNClob.setCharacterStream(0));
        return iNClob;
    }

    public static Blob copyBlob(Connection insertConn, Blob blob) throws SQLException, IOException {
        Blob iBlob = insertConn.createBlob();
        copyLarge(blob.getBinaryStream(), iBlob.setBinaryStream(0));
        return iBlob;
    }

    public static Clob copyClob(Connection insertConn, Clob clob) throws SQLException, IOException {
        Clob iClob = insertConn.createClob();
        copyLarge(clob.getCharacterStream(), iClob.setCharacterStream(0));
        return iClob;
    }

    public static SQLXML copySQLXML(Connection insertConn, SQLXML sqlxml) throws SQLException, IOException {
        SQLXML iSQLXML = insertConn.createSQLXML();
        copyLarge(sqlxml.getBinaryStream(), iSQLXML.setBinaryStream());
        return iSQLXML;
    }

    public static String getDBData(Connection conn, String tableName, String fieldName, String where) throws SQLException {
        String sql = "SELECT " + fieldName + " FROM " + tableName + " WHERE " + where;

        try (Statement statement = conn.createStatement()) {
            ResultSet rs = statement.executeQuery(sql);
            if (rs.next()) {
                return rs.getString(1);
            } else {
                throw new IllegalStateException("SQL " + sql + " returned no rows");
            }
        }
    }

    public static String[][] getSQLData(Connection conn, String sql) throws SQLException {
        List<String[]> rows = new ArrayList<>();
        try (Statement statement = conn.createStatement()) {
            ResultSet rs = statement.executeQuery(sql);
            int colCount = rs.getMetaData().getColumnCount();
            while (rs.next()) {
                String[] row = new String[colCount];
                for (int i = 0; i < colCount; i++) {
                    row[i] = rs.getString(i + 1);
                }
                rows.add(row);
            }
        }
        String[][] ret = new String[rows.size()][];
        for (int i = 0; i < rows.size(); i++) {
            ret[i] = rows.get(i);
        }
        return ret;
    }

    public static String explainSQLException(SQLException ex, PreparedStatement insertStatement) {
        StringBuilder sb = new StringBuilder();
        sb.append(ex.getMessage()).append(", SQL error code: ").append(ex.getErrorCode()).append(", SQL state: ").append(ex.getSQLState());
        if (insertStatement != null) {
            sb.append(", ").append(insertStatement.toString());
        }
        return sb.toString();
    }

    public static String explainSQLException(SQLException ex) {
        return explainSQLException(ex, null);
    }

    public static Connection connect(TestProperties props) throws ClassNotFoundException, SQLException {
        Connection conn;
        Class.forName(props.getDbDriver());
        Locale.setDefault(Locale.US);
        DriverManager.setLoginTimeout(2);
        conn = DriverManager.getConnection(props.getDbUrl(), props.getDbUser(), props.getDbPassword());
        conn.setAutoCommit(false);
        return conn;
    }


    @SuppressWarnings("Duplicates")
    public static long copyLarge(InputStream input, OutputStream output) throws IOException {
        /*
        from Apache IOUtils
         */
        //FIXME: find best buffer size
        byte[] buffer = new byte[1024 * 10];
        long count = 0;
        int n = 0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        output.close();
        return count;
    }

    @SuppressWarnings("Duplicates")
    public static long copyLarge(Reader input, Writer output) throws IOException {
        /*
        from Apache IOUtils
         */
        //FIXME: find best buffer size
        char[] buffer = new char[1024 * 10];
        long count = 0;
        int n = 0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        output.close();
        return count;
    }


    public static int executeSQL(Connection conn, String sql) throws SQLException {
        int ret = -1;
        try (Statement statement = conn.createStatement()) {
            ret = statement.executeUpdate(sql);
        }
        return ret;
    }

}
