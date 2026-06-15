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
                    "CREATE TABLE IF NOT EXISTS users (" +
                    "id TEXT PRIMARY KEY, username TEXT UNIQUE NOT NULL, " +
                    "password_hash TEXT NOT NULL, salt TEXT NOT NULL)"
            );

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

            // Check which columns exist in mind_maps
            boolean hasUserId = false;
            boolean hasSyncStatus = false;
            boolean hasTheme = false;
            try (java.sql.ResultSet rs = stmt.executeQuery("PRAGMA table_info(mind_maps)")) {
                while (rs.next()) {
                    String columnName = rs.getString("name");
                    if ("user_id".equals(columnName)) hasUserId = true;
                    if ("sync_status".equals(columnName)) hasSyncStatus = true;
                    if ("theme".equals(columnName)) hasTheme = true;
                }
            }

            if (!hasUserId) {
                stmt.execute("ALTER TABLE mind_maps ADD COLUMN user_id TEXT");
            }
            if (!hasSyncStatus) {
                stmt.execute("ALTER TABLE mind_maps ADD COLUMN sync_status TEXT DEFAULT 'PENDING'");
            }
            if (!hasTheme) {
                stmt.execute("ALTER TABLE mind_maps ADD COLUMN theme TEXT DEFAULT 'LIGHT'");
            }

            // Check which columns exist in nodes
            boolean hasTextSize = false;
            boolean hasColor = false;
            boolean hasShape = false;
            try (java.sql.ResultSet rs = stmt.executeQuery("PRAGMA table_info(nodes)")) {
                while (rs.next()) {
                    String columnName = rs.getString("name");
                    if ("text_size".equals(columnName)) hasTextSize = true;
                    if ("color".equals(columnName)) hasColor = true;
                    if ("shape".equals(columnName)) hasShape = true;
                }
            }

            if (!hasTextSize) {
                stmt.execute("ALTER TABLE nodes ADD COLUMN text_size REAL DEFAULT 12.0");
            }
            if (!hasColor) {
                stmt.execute("ALTER TABLE nodes ADD COLUMN color TEXT DEFAULT '#ffffff'");
            }
            boolean hasDescription = false;
            try (java.sql.ResultSet rs2 = stmt.executeQuery("PRAGMA table_info(nodes)")) {
                while (rs2.next()) {
                    if ("description".equals(rs2.getString("name"))) hasDescription = true;
                }
            }

            if (!hasShape) {
                stmt.execute("ALTER TABLE nodes ADD COLUMN shape TEXT DEFAULT 'ROUNDED_RECT'");
            }
            if (!hasDescription) {
                stmt.execute("ALTER TABLE nodes ADD COLUMN description TEXT DEFAULT ''");
            }

            // icon + badge columns (added in ki-testing feature)
            boolean hasIcon = false, hasBadge = false;
            try (java.sql.ResultSet rs3 = stmt.executeQuery("PRAGMA table_info(nodes)")) {
                while (rs3.next()) {
                    String col = rs3.getString("name");
                    if ("icon".equals(col))  hasIcon  = true;
                    if ("badge".equals(col)) hasBadge = true;
                }
            }
            if (!hasIcon)  stmt.execute("ALTER TABLE nodes ADD COLUMN icon TEXT DEFAULT ''");
            if (!hasBadge) stmt.execute("ALTER TABLE nodes ADD COLUMN badge TEXT DEFAULT ''");

        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }
}
