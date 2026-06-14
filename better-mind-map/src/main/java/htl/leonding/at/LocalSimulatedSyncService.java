package htl.leonding.at;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class LocalSimulatedSyncService implements SyncService {

    @Override
    public void syncMap(String mapId) {
        String sql = "UPDATE mind_maps SET sync_status = 'SYNCED' WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, mapId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update sync status", e);
        }
    }

    @Override
    public String getSyncStatus(String mapId) {
        String sql = "SELECT sync_status FROM mind_maps WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, mapId);
            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("sync_status");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get sync status", e);
        }
        return "PENDING";
    }
}
