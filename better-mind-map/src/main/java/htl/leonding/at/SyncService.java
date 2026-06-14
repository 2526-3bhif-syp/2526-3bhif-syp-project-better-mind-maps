package htl.leonding.at;

public interface SyncService {
    void syncMap(String mapId);
    String getSyncStatus(String mapId);
}
