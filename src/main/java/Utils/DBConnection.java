package Utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DBConnection {
    private static final String URL = "jdbc:sqlite:db_swd.db";

    static {
        try { Class.forName("org.sqlite.JDBC"); }
        catch (ClassNotFoundException e) { throw new RuntimeException("SQLite JDBC driver not found", e); }
    }

    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(URL);
        conn.createStatement().execute("PRAGMA foreign_keys = ON");
        return conn;
    }

    public static void close(AutoCloseable... resources) {
        for (AutoCloseable r : resources)
            if (r != null) try { r.close(); } catch (Exception ignored) {}
    }
}
