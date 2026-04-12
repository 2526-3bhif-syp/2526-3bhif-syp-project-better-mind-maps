package htl.leonding.at;

import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class MindMapServiceTest {

    private Path tempDb;
    private MindMapService service;

    @BeforeEach
    void setUp() throws IOException {
        tempDb = Files.createTempFile("mindmap_test", ".db");
        DatabaseManager.setDbUrl("jdbc:sqlite:" + tempDb.toAbsolutePath());
        DatabaseManager.initialize();
        service = new MindMapService(new MindMapRepository());
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(tempDb);
    }

    // --- createMindMap ---

    @Test
    void createMindMap_validName_returnsMapWithRootNode() {
        MindMap map = service.createMindMap("Test");

        assertNotNull(map);
        assertEquals("Test", map.getName());
        assertEquals(1, map.getNodes().size());

        Node root = map.getNodes().get(0);
        assertNull(root.getParentId());
        assertEquals("Test", root.getText());
    }

    @Test
    void createMindMap_nullName_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> service.createMindMap(null));
    }

    @Test
    void createMindMap_emptyName_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> service.createMindMap("   "));
    }

    @Test
    void createMindMap_persistedInDatabase() {
        MindMap map = service.createMindMap("Persistent");

        MindMapRepository repo = new MindMapRepository();
        var all = repo.findAll();
        assertEquals(1, all.size());
        assertEquals("Persistent", all.get(0).getName());
    }

    // --- addNode ---

    @Test
    void addNode_validParent_returnsNewNode() {
        MindMap map = service.createMindMap("Map");
        Node root = map.getNodes().get(0);

        Node child = service.addNode(map, root.getId(), "Child");

        assertNotNull(child);
        assertEquals("Child", child.getText());
        assertEquals(root.getId(), child.getParentId());
        assertEquals(2, map.getNodes().size());
    }

    @Test
    void addNode_positionedRightOfParent() {
        MindMap map = service.createMindMap("Map");
        Node root = map.getNodes().get(0);

        Node child = service.addNode(map, root.getId(), "Child");

        assertEquals(root.getXCoordinate() + 150, child.getXCoordinate(), 0.001);
    }

    @Test
    void addNode_multipleChildren_stackedVertically() {
        MindMap map = service.createMindMap("Map");
        Node root = map.getNodes().get(0);

        Node child1 = service.addNode(map, root.getId(), "Child 1");
        Node child2 = service.addNode(map, root.getId(), "Child 2");

        assertEquals(child1.getYCoordinate() + 80, child2.getYCoordinate(), 0.001);
    }

    @Test
    void addNode_emptyText_throwsException() {
        MindMap map = service.createMindMap("Map");
        Node root = map.getNodes().get(0);

        assertThrows(IllegalArgumentException.class, () -> service.addNode(map, root.getId(), ""));
    }

    @Test
    void addNode_invalidParentId_throwsException() {
        MindMap map = service.createMindMap("Map");

        assertThrows(IllegalArgumentException.class, () -> service.addNode(map, "nonexistent-id", "Child"));
    }

    @Test
    void addNode_persistedInDatabase() {
        MindMap map = service.createMindMap("Map");
        Node root = map.getNodes().get(0);
        service.addNode(map, root.getId(), "Child");

        MindMap loaded = new MindMapRepository().findAll().get(0);
        assertEquals(2, loaded.getNodes().size());
    }

    // --- updateNodeText ---

    @Test
    void updateNodeText_valid_changesText() {
        MindMap map = service.createMindMap("Map");
        Node root = map.getNodes().get(0);

        service.updateNodeText(map, root, "Updated");

        assertEquals("Updated", root.getText());
    }

    @Test
    void updateNodeText_persistedInDatabase() {
        MindMap map = service.createMindMap("Map");
        Node root = map.getNodes().get(0);

        service.updateNodeText(map, root, "Updated");

        MindMap loaded = new MindMapRepository().findAll().get(0);
        assertEquals("Updated", loaded.getNodes().get(0).getText());
    }

    @Test
    void updateNodeText_emptyText_throwsException() {
        MindMap map = service.createMindMap("Map");
        Node root = map.getNodes().get(0);

        assertThrows(IllegalArgumentException.class, () -> service.updateNodeText(map, root, ""));
    }

    // --- deleteNode ---

    @Test
    void deleteNode_nonRoot_removesNode() {
        MindMap map = service.createMindMap("Map");
        Node root = map.getNodes().get(0);
        Node child = service.addNode(map, root.getId(), "Child");

        service.deleteNode(map, child);

        assertEquals(1, map.getNodes().size());
        assertFalse(map.getNodes().contains(child));
    }

    @Test
    void deleteNode_cascadesDescendants() {
        MindMap map = service.createMindMap("Map");
        Node root = map.getNodes().get(0);
        Node child = service.addNode(map, root.getId(), "Child");
        service.addNode(map, child.getId(), "Grandchild");

        service.deleteNode(map, child);

        assertEquals(1, map.getNodes().size());
    }

    @Test
    void deleteNode_persistedInDatabase() {
        MindMap map = service.createMindMap("Map");
        Node root = map.getNodes().get(0);
        Node child = service.addNode(map, root.getId(), "Child");

        service.deleteNode(map, child);

        MindMap loaded = new MindMapRepository().findAll().get(0);
        assertEquals(1, loaded.getNodes().size());
    }

    @Test
    void deleteNode_rootNode_throwsException() {
        MindMap map = service.createMindMap("Map");
        Node root = map.getNodes().get(0);

        assertThrows(IllegalArgumentException.class, () -> service.deleteNode(map, root));
    }
}
