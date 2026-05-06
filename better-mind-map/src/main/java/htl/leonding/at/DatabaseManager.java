package htl.leonding.at;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private static String dbUrl = "jdbc:sqlite:data/mindmaps.db";

    public static void setDbUrl(String url) {
        dbUrl = url;
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl);
    }

    public static void initialize() {
        new File("data").mkdirs();

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS mind_maps (" +
                    "id TEXT PRIMARY KEY, name TEXT NOT NULL)"
            );

            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS nodes (" +
                    "id TEXT PRIMARY KEY, mind_map_id TEXT NOT NULL, " +
                    "text TEXT NOT NULL, parent_id TEXT, " +
                    "x_coordinate REAL NOT NULL, y_coordinate REAL NOT NULL, " +
                    "FOREIGN KEY (mind_map_id) REFERENCES mind_maps(id))"
            );

        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }
}
