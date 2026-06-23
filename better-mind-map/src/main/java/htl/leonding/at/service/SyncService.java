package htl.leonding.at.service;
import htl.leonding.at.model.*;
import htl.leonding.at.controller.*;
import htl.leonding.at.service.*;
import htl.leonding.at.repository.*;
import htl.leonding.at.util.*;
import htl.leonding.at.App;


public interface SyncService {
    void syncMap(String mapId);
    String getSyncStatus(String mapId);
}
