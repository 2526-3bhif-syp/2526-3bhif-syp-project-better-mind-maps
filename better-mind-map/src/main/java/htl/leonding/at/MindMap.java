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

    public String getId() { return _id; }
    public String getName() { return _name; }

    public void addNode(Node node) {
        nodes.add(node);
    }

    public List<Node> getNodes() {
        return nodes;
    }
}


