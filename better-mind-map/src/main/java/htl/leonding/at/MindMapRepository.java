package htl.leonding.at;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MindMapRepository {

    public void save(MindMap map) {
        String sql = "INSERT OR REPLACE INTO mind_maps (id, name) VALUES (?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, map.getId());
            stmt.setString(2, map.getName());
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
                     "(id, mind_map_id, text, parent_id, x_coordinate, y_coordinate) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, node.getId());
            stmt.setString(2, mapId);
            stmt.setString(3, node.getText());
            stmt.setString(4, node.getParentId());
            stmt.setDouble(5, node.getXCoordinate());
            stmt.setDouble(6, node.getYCoordinate());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save node", e);
        }
    }

    public void updateNode(Node node) {
        String sql = "UPDATE nodes SET text = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, node.getText());
            stmt.setString(2, node.getId());
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

    public List<MindMap> loadAll() {
        List<MindMap> maps = new ArrayList<>();
        String mapSql = "SELECT id, name FROM mind_maps";
        String nodeSql = "SELECT id, text, parent_id, x_coordinate, y_coordinate " +
                         "FROM nodes WHERE mind_map_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(mapSql)) {

            while (rs.next()) {
                MindMap map = new MindMap(rs.getString("id"), rs.getString("name"));

                try (PreparedStatement nodeStmt = conn.prepareStatement(nodeSql)) {
                    nodeStmt.setString(1, map.getId());
                    try (ResultSet nodeRs = nodeStmt.executeQuery()) {
                        while (nodeRs.next()) {
                            map.addNode(new Node(
                                    nodeRs.getString("id"),
                                    nodeRs.getString("text"),
                                    nodeRs.getString("parent_id"),
                                    nodeRs.getDouble("x_coordinate"),
                                    nodeRs.getDouble("y_coordinate")
                            ));
                        }
                    }
                }
                maps.add(map);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load mind maps", e);
        }
        return maps;
    }
}
