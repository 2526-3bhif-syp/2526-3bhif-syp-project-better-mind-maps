package htl.leonding.at;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MindMapService {

    private final MindMapRepository repository;

    public MindMapService(MindMapRepository repository) {
        this.repository = repository;
    }

    public MindMap createMindMap(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name darf nicht leer sein");
        }
        MindMap mindMap = new MindMap(UUID.randomUUID().toString(), name);
        Node root = new Node(UUID.randomUUID().toString(), name, null, 400, 300);
        mindMap.addNode(root);
        repository.save(mindMap);
        return mindMap;
    }

    public Node addNode(MindMap map, String parentId, String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Text darf nicht leer sein");
        }
        Node parent = map.getNodes().stream()
                .filter(n -> n.getId().equals(parentId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Parent node not found"));

        double x = parent.getXCoordinate() + 150;
        double y = parent.getYCoordinate();

        Node node = new Node(UUID.randomUUID().toString(), text, parentId, x, y);
        map.addNode(node);
        repository.saveNode(map.getId(), node);
        return node;
    }

    public void updateNodeText(MindMap map, Node node, String newText) {
        if (newText == null || newText.trim().isEmpty()) {
            throw new IllegalArgumentException("Text darf nicht leer sein");
        }
        node.setText(newText);
        repository.updateNode(node);
    }

    public void deleteNode(MindMap map, Node node) {
        if (node.getParentId() == null) {
            throw new IllegalArgumentException("Root node cannot be deleted");
        }
        List<String> idsToDelete = new ArrayList<>();
        collectDescendantIds(map, node.getId(), idsToDelete);
        idsToDelete.add(node.getId());
        for (String id : idsToDelete) {
            map.removeNode(id);
            repository.deleteNode(id);
        }
    }

    private void collectDescendantIds(MindMap map, String nodeId, List<String> result) {
        for (Node n : new ArrayList<>(map.getNodes())) {
            if (nodeId.equals(n.getParentId())) {
                result.add(n.getId());
                collectDescendantIds(map, n.getId(), result);
            }
        }
    }
}
