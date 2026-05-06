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

        double candidateX = 0;
        double candidateY = 0;
        
        int i = 0;
        double angleStep = Math.PI / 4; // 45 Grad Schritte (8 Positionen pro Kreis)
        double currentRadius = 150;

        while (true) {
            candidateX = parent.getXCoordinate() + currentRadius * Math.cos(i * angleStep);
            candidateY = parent.getYCoordinate() + currentRadius * Math.sin(i * angleStep);
            
            boolean collision = false;
            for (Node n : map.getNodes()) {
                if (Math.abs(n.getXCoordinate() - candidateX) < 120 && Math.abs(n.getYCoordinate() - candidateY) < 60) {
                    collision = true;
                    break;
                }
            }
            
            if (!collision) {
                break; // Nearest free spot found
            }
            
            i++;
            // Wenn wir einen vollen Kreis (8 Positionen) geprüft haben, machen wir den Suchradius größer
            if (i % 8 == 0) {
                currentRadius += 80;
            }
        }

        Node node = new Node(UUID.randomUUID().toString(), text, parentId, candidateX, candidateY);
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

    public void deleteMindMap(String mapId) {
        repository.deleteMindMap(mapId);
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
