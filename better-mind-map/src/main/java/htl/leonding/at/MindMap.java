package htl.leonding.at;

import java.util.ArrayList;
import java.util.List;

public class MindMap {

    private String _id;
    private String _name;
    private List<Node> nodes;
    private String userId;
    private String syncStatus;
    private String theme = "LIGHT";

    public MindMap(String id, String name) {
        this._id = id;
        this._name = name;
        this.nodes = new ArrayList<Node>();
        this.syncStatus = "PENDING";
    }

    public String getId() {
        return _id;
    }

    public String getName() {
        return _name;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(String syncStatus) {
        this.syncStatus = syncStatus;
    }

    public String getTheme() {
        return theme != null ? theme : "LIGHT";
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public void addNode(Node node) {
        nodes.add(node);
    }

    public void removeNode(String nodeId) {
        nodes.removeIf(n -> n.getId().equals(nodeId));
    }

    public List<Node> getNodes() {
        return nodes;
    }

    public static MindMap createNew(String name) {
        String id = java.util.UUID.randomUUID().toString();
        MindMap map = new MindMap(id, name);
        Node root = new Node(java.util.UUID.randomUUID().toString(), name, null, 400, 300);
        map.addNode(root);
        return map;
    }
}
