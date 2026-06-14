package htl.leonding.at;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MindMapRepository {

    public void save(MindMap map) {
        String sql = "INSERT OR REPLACE INTO mind_maps (id, name, user_id, sync_status, theme) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, map.getId());
            stmt.setString(2, map.getName());
            stmt.setString(3, map.getUserId());
            stmt.setString(4, map.getSyncStatus());
            stmt.setString(5, map.getTheme());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save mind map", e);
        }

        for (Node node : map.getNodes()) {
            saveNode(map.getId(), node);
        }
    }

    public void saveNode(String mapId, Node node) {
        String sql = "INSERT OR REPLACE INTO nodes " +
                     "(id, mind_map_id, text, parent_id, x_coordinate, y_coordinate, text_size, color) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, node.getId());
            stmt.setString(2, mapId);
            stmt.setString(3, node.getText());
            stmt.setString(4, node.getParentId());
            stmt.setDouble(5, node.getXCoordinate());
            stmt.setDouble(6, node.getYCoordinate());
            stmt.setDouble(7, node.getTextSize());
            stmt.setString(8, node.getColor());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save node", e);
        }
    }

    public void updateNode(Node node) {
        String sql = "UPDATE nodes SET text = ?, x_coordinate = ?, y_coordinate = ?, text_size = ?, color = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, node.getText());
            stmt.setDouble(2, node.getXCoordinate());
            stmt.setDouble(3, node.getYCoordinate());
            stmt.setDouble(4, node.getTextSize());
            stmt.setString(5, node.getColor());
            stmt.setString(6, node.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update node", e);
        }
    }

    public void deleteNode(String nodeId) {
        String sql = "DELETE FROM nodes WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nodeId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete node", e);
        }
    }

    public void updateTheme(String mapId, String theme) {
        String sql = "UPDATE mind_maps SET theme = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, theme);
            stmt.setString(2, mapId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update map theme", e);
        }
    }

    public void deleteMindMap(String mapId) {
        try (Connection conn = DatabaseManager.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM nodes WHERE mind_map_id = ?")) {
                stmt.setString(1, mapId);
                stmt.executeUpdate();
            }
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM mind_maps WHERE id = ?")) {
                stmt.setString(1, mapId);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete mind map", e);
        }
    }

    public List<MindMap> loadAll(String userId) {
        List<MindMap> maps = new ArrayList<>();
        String mapSql = "SELECT id, name, user_id, sync_status, theme FROM mind_maps WHERE user_id = ?";
        String nodeSql = "SELECT id, text, parent_id, x_coordinate, y_coordinate, text_size, color " +
                         "FROM nodes WHERE mind_map_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(mapSql)) {
            stmt.setString(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    MindMap map = new MindMap(rs.getString("id"), rs.getString("name"));
                    map.setUserId(rs.getString("user_id"));
                    map.setSyncStatus(rs.getString("sync_status"));
                    map.setTheme(rs.getString("theme"));

                    try (PreparedStatement nodeStmt = conn.prepareStatement(nodeSql)) {
                        nodeStmt.setString(1, map.getId());
                        try (ResultSet nodeRs = nodeStmt.executeQuery()) {
                            while (nodeRs.next()) {
                                map.addNode(new Node(
                                        nodeRs.getString("id"),
                                        nodeRs.getString("text"),
                                        nodeRs.getString("parent_id"),
                                        nodeRs.getDouble("x_coordinate"),
                                        nodeRs.getDouble("y_coordinate"),
                                        nodeRs.getDouble("text_size"),
                                        nodeRs.getString("color")
                                ));
                            }
                        }
                    }
                    maps.add(map);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load mind maps", e);
        }
        return maps;
    }
}
