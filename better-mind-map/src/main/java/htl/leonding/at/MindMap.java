package htl.leonding.at;

import java.util.ArrayList;
import java.util.List;

public class MindMap {

    private String _id;
    private String _name;
    private List<Node> nodes;

    public MindMap(String id, String name) {
        this._id = id;
        this._name = name;
        this.nodes = new ArrayList<Node>();
    }

    public String getId() {
        return _id;
    }

    public String getName() {
        return _name;
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
