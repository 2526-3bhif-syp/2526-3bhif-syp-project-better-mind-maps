package htl.leonding.at;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;

import java.util.*;
import java.util.stream.Collectors;

public class MainController {

    @FXML
    private TreeView<String> hierarchyTree;
    @FXML
    private TabPane tabPane;

    private final MindMapRepository repository = new MindMapRepository();
    private final MindMapService service = new MindMapService(repository);

    private Node currentNode = null;

    @FXML
    public void initialize() {
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null && newTab.getUserData() instanceof MindMap) {
                MindMap map = (MindMap) newTab.getUserData();
                currentNode = getRoot(map);
                Pane canvas = (Pane) newTab.getContent();
                refreshCanvas(canvas, map);
            }
        });
    }

    @FXML
    public void onCreateNewMap() {
        TextInputDialog dialog = new TextInputDialog("New Map");
        dialog.setTitle("New Mind Map");
        dialog.setHeaderText("Create a new Mind Map");
        dialog.setContentText("Please enter the name:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            MindMap map = service.createMindMap(name);
            currentNode = getRoot(map);
            renderMindMap(map);
        });
    }

    private void renderMindMap(MindMap map) {
        Pane canvas = new Pane();
        canvas.setFocusTraversable(true);
        canvas.setOnMouseClicked(e -> canvas.requestFocus());
        canvas.setOnKeyPressed(e -> handleKeyPress(e, map, canvas));

        // Re-layout when canvas gets its actual size on first display
        canvas.widthProperty().addListener((obs, oldW, newW) -> {
            if (newW.doubleValue() > 10 && canvas.getHeight() > 10) {
                refreshCanvas(canvas, map);
            }
        });

        Tab tab = new Tab(map.getName());
        tab.setContent(canvas);
        tab.setUserData(map);
        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);
        refreshCanvas(canvas, map);
        canvas.requestFocus();
    }

    private void handleKeyPress(KeyEvent e, MindMap map, Pane canvas) {
        if (currentNode == null) return;

        KeyCode code = e.getCode();

        if (code == KeyCode.ENTER || code == KeyCode.INSERT) {
            promptAddChild(map, canvas, currentNode);
            e.consume();

        } else if (code == KeyCode.TAB) {
            List<Node> nodes = map.getNodes();
            int idx = nodes.indexOf(currentNode);
            if (e.isShiftDown()) {
                idx = (idx - 1 + nodes.size()) % nodes.size();
            } else {
                idx = (idx + 1) % nodes.size();
            }
            currentNode = nodes.get(idx);
            refreshCanvas(canvas, map);
            e.consume();

        } else if (code == KeyCode.RIGHT) {
            List<Node> children = getChildren(map, currentNode);
            if (!children.isEmpty()) {
                currentNode = children.get(0);
                refreshCanvas(canvas, map);
            }
            e.consume();

        } else if (code == KeyCode.LEFT) {
            if (currentNode.getParentId() != null) {
                map.getNodes().stream()
                        .filter(n -> n.getId().equals(currentNode.getParentId()))
                        .findFirst()
                        .ifPresent(parent -> {
                            currentNode = parent;
                            refreshCanvas(canvas, map);
                        });
            }
            e.consume();

        } else if (code == KeyCode.UP) {
            navigateSibling(map, canvas, -1);
            e.consume();

        } else if (code == KeyCode.DOWN) {
            navigateSibling(map, canvas, 1);
            e.consume();

        } else if (code == KeyCode.F2) {
            promptEditNode(map, canvas, currentNode);
            e.consume();

        } else if (code == KeyCode.DELETE) {
            if (currentNode.getParentId() != null) {
                String parentId = currentNode.getParentId();
                service.deleteNode(map, currentNode);
                currentNode = map.getNodes().stream()
                        .filter(n -> n.getId().equals(parentId))
                        .findFirst()
                        .orElse(map.getNodes().isEmpty() ? null : map.getNodes().get(0));
                refreshCanvas(canvas, map);
            }
            e.consume();
        }
    }

    private void navigateSibling(MindMap map, Pane canvas, int direction) {
        if (currentNode.getParentId() == null) return;
        List<Node> siblings = map.getNodes().stream()
                .filter(n -> currentNode.getParentId().equals(n.getParentId()))
                .collect(Collectors.toList());
        int idx = siblings.indexOf(currentNode);
        if (idx >= 0) {
            int newIdx = idx + direction;
            if (newIdx >= 0 && newIdx < siblings.size()) {
                currentNode = siblings.get(newIdx);
                refreshCanvas(canvas, map);
            }
        }
    }

    private void promptAddChild(MindMap map, Pane canvas, Node parent) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Child Node");
        dialog.setHeaderText("Add a child to \"" + parent.getText() + "\"");
        dialog.setContentText("Node text:");
        dialog.showAndWait().ifPresent(text -> {
            Node newNode = service.addNode(map, parent.getId(), text);
            currentNode = newNode;
            refreshCanvas(canvas, map);
            canvas.requestFocus();
        });
    }

    private void promptEditNode(MindMap map, Pane canvas, Node node) {
        TextInputDialog dialog = new TextInputDialog(node.getText());
        dialog.setTitle("Edit Node");
        dialog.setHeaderText("Edit node text");
        dialog.setContentText("New text:");
        dialog.showAndWait().ifPresent(text -> {
            service.updateNodeText(map, node, text);
            refreshCanvas(canvas, map);
            canvas.requestFocus();
        });
    }

    // ── Layout ──────────────────────────────────────────────────────────────

    private void refreshCanvas(Pane canvas, MindMap map) {
        double w = canvas.getWidth() > 0 ? canvas.getWidth()
                : (canvas.getScene() != null ? canvas.getScene().getWidth() - 220 : 800);
        double h = canvas.getHeight() > 0 ? canvas.getHeight()
                : (canvas.getScene() != null ? canvas.getScene().getHeight() - 80 : 600);

        layoutMindMap(map, w, h);

        canvas.getChildren().clear();

        // Edges (drawn first, appear behind nodes)
        for (Node node : map.getNodes()) {
            if (node.getParentId() == null) continue;
            map.getNodes().stream()
                    .filter(p -> p.getId().equals(node.getParentId()))
                    .findFirst()
                    .ifPresent(parent -> {
                        Line line = new Line(
                                parent.getXCoordinate(), parent.getYCoordinate(),
                                node.getXCoordinate(), node.getYCoordinate()
                        );
                        line.setStroke(Color.DARKGRAY);
                        line.setStrokeWidth(1.5);
                        canvas.getChildren().add(line);
                    });
        }

        // Nodes
        for (Node node : map.getNodes()) {
            boolean isCurrent = node == currentNode;
            boolean isRoot = node.getParentId() == null;
            StackPane nodeView = createNodeView(node, map, canvas, isCurrent, isRoot);
            nodeView.setLayoutX(node.getXCoordinate() - 52);
            nodeView.setLayoutY(node.getYCoordinate() - 26);
            canvas.getChildren().add(nodeView);
        }

        refreshTree(map);
    }

    /**
     * Radial layout: root is centered, children are distributed angularly
     * weighted by their subtree leaf-count so no branches overlap.
     */
    private void layoutMindMap(MindMap map, double w, double h) {
        Node root = getRoot(map);
        if (root == null) return;

        root.setXCoordinate(w / 2);
        root.setYCoordinate(h / 2);

        double radius = Math.min(w, h) * 0.28;
        positionChildren(map, root, 0, 2 * Math.PI, radius);
    }

    private void positionChildren(MindMap map, Node parent,
                                  double startAngle, double endAngle, double radius) {
        List<Node> children = getChildren(map, parent);
        if (children.isEmpty()) return;

        double range = endAngle - startAngle;
        int totalLeaves = countLeaves(map, parent);
        double currentAngle = startAngle;

        for (Node child : children) {
            int childLeaves = countLeaves(map, child);
            double childRange = range * (double) childLeaves / totalLeaves;
            double midAngle = currentAngle + childRange / 2;

            child.setXCoordinate(parent.getXCoordinate() + radius * Math.cos(midAngle));
            child.setYCoordinate(parent.getYCoordinate() + radius * Math.sin(midAngle));

            positionChildren(map, child, currentAngle, currentAngle + childRange, radius * 0.58);
            currentAngle += childRange;
        }
    }

    private int countLeaves(MindMap map, Node node) {
        List<Node> children = getChildren(map, node);
        if (children.isEmpty()) return 1;
        return children.stream().mapToInt(c -> countLeaves(map, c)).sum();
    }

    private List<Node> getChildren(MindMap map, Node parent) {
        return map.getNodes().stream()
                .filter(n -> parent.getId().equals(n.getParentId()))
                .collect(Collectors.toList());
    }

    private Node getRoot(MindMap map) {
        return map.getNodes().stream()
                .filter(n -> n.getParentId() == null)
                .findFirst().orElse(null);
    }

    // ── Node view ────────────────────────────────────────────────────────────

    private StackPane createNodeView(Node node, MindMap map, Pane canvas,
                                     boolean isCurrent, boolean isRoot) {
        StackPane nodeView = new StackPane();

        Ellipse ellipse = new Ellipse(52, 26);
        ellipse.setFill(isRoot ? Color.web("#FFD700") : Color.WHITE);

        if (isCurrent) {
            ellipse.setStroke(Color.DODGERBLUE);
            ellipse.setStrokeWidth(3.5);
        } else if (isRoot) {
            ellipse.setStroke(Color.GOLDENROD);
            ellipse.setStrokeWidth(2);
        } else {
            ellipse.setStroke(Color.web("#555555"));
            ellipse.setStrokeWidth(1.5);
        }

        Label label = new Label(node.getText());
        label.setMaxWidth(96);
        label.setWrapText(true);
        label.setStyle("-fx-font-size: 12px; -fx-alignment: center; -fx-text-alignment: center;");

        nodeView.getChildren().addAll(ellipse, label);

        // Click to select
        nodeView.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                currentNode = node;
                refreshCanvas(canvas, map);
                canvas.requestFocus();
            }
        });

        // Context menu
        ContextMenu contextMenu = new ContextMenu();

        MenuItem addChild = new MenuItem("Add Child Node  [Enter]");
        addChild.setOnAction(e -> promptAddChild(map, canvas, node));

        MenuItem editText = new MenuItem("Edit Text  [F2]");
        editText.setOnAction(e -> promptEditNode(map, canvas, node));

        MenuItem deleteNode = new MenuItem("Delete Node  [Del]");
        deleteNode.setDisable(node.getParentId() == null);
        deleteNode.setOnAction(e -> {
            if (node.getParentId() != null) {
                String parentId = node.getParentId();
                service.deleteNode(map, node);
                currentNode = map.getNodes().stream()
                        .filter(n -> n.getId().equals(parentId))
                        .findFirst().orElse(null);
                refreshCanvas(canvas, map);
            }
        });

        contextMenu.getItems().addAll(addChild, editText, deleteNode);
        nodeView.setOnContextMenuRequested(e ->
                contextMenu.show(nodeView, e.getScreenX(), e.getScreenY())
        );

        return nodeView;
    }

    // ── Hierarchy tree ───────────────────────────────────────────────────────

    private void refreshTree(MindMap map) {
        Map<String, TreeItem<String>> itemMap = new HashMap<>();
        TreeItem<String> rootItem = null;

        for (Node node : map.getNodes()) {
            TreeItem<String> item = new TreeItem<>(node.getText());
            itemMap.put(node.getId(), item);
            if (node.getParentId() == null) rootItem = item;
        }

        for (Node node : map.getNodes()) {
            if (node.getParentId() != null) {
                TreeItem<String> parentItem = itemMap.get(node.getParentId());
                TreeItem<String> childItem = itemMap.get(node.getId());
                if (parentItem != null && childItem != null) {
                    parentItem.getChildren().add(childItem);
                }
            }
        }

        if (rootItem != null) {
            rootItem.setExpanded(true);
            hierarchyTree.setRoot(rootItem);
        }
    }
}
