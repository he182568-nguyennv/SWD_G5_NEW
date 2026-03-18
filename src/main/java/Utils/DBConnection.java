package Utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    private static final String DB_PATH = System.getProperty(
            "vpt.db.path",
            System.getProperty("user.dir") + "/db_swd.sqlite"
    );
    private static final String URL = "jdbc:sqlite:" + DB_PATH;

    static {
        try {
            Class.forName("org.sqlite.JDBC");
            // Kiểm tra file tồn tại trước khi connect
            java.io.File dbFile = new java.io.File(DB_PATH);
            if (!dbFile.exists()) {
                throw new RuntimeException("[DB] File không tồn tại: " + DB_PATH);
            }
            System.out.println("[DB] Connecting to: " + DB_PATH);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("SQLite JDBC driver not found", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    public static void close(AutoCloseable... resources) {
        for (AutoCloseable r : resources)
            if (r != null) try { r.close(); } catch (Exception ignored) {}
    }
}