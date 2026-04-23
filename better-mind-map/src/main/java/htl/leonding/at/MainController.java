package htl.leonding.at;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.paint.CycleMethod;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class MainController {

    @FXML private TreeView<String> hierarchyTree;
    @FXML private TabPane tabPane;
    @FXML private HBox sidebarHeader;

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

        // Sync sidebar header height with the TabPane tab-header-area after layout
        tabPane.needsLayoutProperty().addListener((obs, wasNeeded, needed) -> {
            if (!needed) syncSidebarHeaderHeight();
        });
        javafx.application.Platform.runLater(this::syncSidebarHeaderHeight);
    }

    private void syncSidebarHeaderHeight() {
        javafx.scene.Node tabHeader = tabPane.lookup(".tab-header-area");
        if (tabHeader != null && tabHeader.getBoundsInLocal().getHeight() > 0) {
            double h = tabHeader.getBoundsInLocal().getHeight();
            sidebarHeader.setMinHeight(h);
            sidebarHeader.setPrefHeight(h);
            sidebarHeader.setMaxHeight(h);
        }
    }

    public void loadMindMap(MindMap map) {
        currentNode = getRoot(map);
        renderMindMap(map);
    }

    @FXML
    private void onBackToOverview() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("overview-view.fxml"));
            Scene scene = new Scene(loader.load(), 1024, 768);
            Stage stage = (Stage) tabPane.getScene().getWindow();
            stage.setScene(scene);
        } catch (IOException e) {
            throw new RuntimeException("Failed to open overview", e);
        }
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
        canvas.setStyle("-fx-background-color: #f8f9fa;");

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
                        line.setStroke(Color.web("#adb5bd"));
                        line.setStrokeWidth(2);
                        canvas.getChildren().add(line);
                    });
        }

        // Nodes
        for (Node node : map.getNodes()) {
            boolean isCurrent = node == currentNode;
            boolean isRoot = node.getParentId() == null;
            StackPane nodeView = createNodeView(node, map, canvas, isCurrent, isRoot);
            nodeView.setLayoutX(node.getXCoordinate() - NODE_W / 2);
            nodeView.setLayoutY(node.getYCoordinate() - NODE_H / 2);
            canvas.getChildren().add(nodeView);
        }

        refreshTree(map);
    }

    /**
     * Radial layout: root is centered, children are distributed angularly
     * weighted by their subtree leaf-count so no branches overlap.
     */
    private static final double H_SPACING = 200;
    private static final double V_SPACING = 60;

    private void layoutMindMap(MindMap map, double w, double h) {
        Node root = getRoot(map);
        if (root == null) return;

        root.setXCoordinate(w / 2);
        root.setYCoordinate(h / 2);

        List<Node> children = getChildren(map, root);
        if (children.isEmpty()) return;

        int rightCount = (children.size() + 1) / 2;
        List<Node> rightChildren = children.subList(0, rightCount);
        List<Node> leftChildren  = children.subList(rightCount, children.size());

        double rightHeight = subtreeListHeight(map, rightChildren);
        double leftHeight  = subtreeListHeight(map, leftChildren);

        layoutChildren(map, rightChildren, w / 2 + H_SPACING, h / 2 - rightHeight / 2,  1);
        layoutChildren(map, leftChildren,  w / 2 - H_SPACING, h / 2 - leftHeight  / 2, -1);
    }

    private void layoutChildren(MindMap map, List<Node> children, double x, double startY, int dir) {
        double y = startY;
        for (Node child : children) {
            double sh = subtreeHeight(map, child);
            child.setXCoordinate(x);
            child.setYCoordinate(y + sh / 2);

            List<Node> grandchildren = getChildren(map, child);
            if (!grandchildren.isEmpty()) {
                double gcHeight = subtreeListHeight(map, grandchildren);
                layoutChildren(map, grandchildren,
                        x + dir * H_SPACING,
                        child.getYCoordinate() - gcHeight / 2,
                        dir);
            }
            y += sh;
        }
    }

    private double subtreeListHeight(MindMap map, List<Node> nodes) {
        return nodes.stream().mapToDouble(n -> subtreeHeight(map, n)).sum();
    }

    private double subtreeHeight(MindMap map, Node node) {
        List<Node> children = getChildren(map, node);
        if (children.isEmpty()) return V_SPACING;
        return Math.max(V_SPACING, subtreeListHeight(map, children));
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

    private static final double NODE_W = 110;
    private static final double NODE_H = 40;
    private static final double NODE_ARC = 10;

    private StackPane createNodeView(Node node, MindMap map, Pane canvas,
                                     boolean isCurrent, boolean isRoot) {
        StackPane nodeView = new StackPane();

        Rectangle rect = new Rectangle(NODE_W, NODE_H);
        rect.setArcWidth(NODE_ARC * 2);
        rect.setArcHeight(NODE_ARC * 2);

        if (isRoot) {
            rect.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web("#3498db")),
                    new Stop(1, Color.web("#2980b9"))));
            rect.setStroke(isCurrent ? Color.web("#e74c3c") : Color.web("#1a6fa8"));
            rect.setStrokeWidth(isCurrent ? 3 : 2);
        } else {
            rect.setFill(Color.WHITE);
            rect.setStroke(isCurrent ? Color.web("#e74c3c") : Color.web("#b2bec3"));
            rect.setStrokeWidth(isCurrent ? 3 : 1.5);
            rect.setEffect(new javafx.scene.effect.DropShadow(4, 0, 2, Color.web("#00000018")));
        }

        Label label = new Label(node.getText());
        label.setMaxWidth(NODE_W - 12);
        label.setWrapText(true);
        label.setStyle(
            "-fx-font-size: 12px; -fx-text-alignment: center; -fx-alignment: center;" +
            (isRoot ? " -fx-text-fill: white; -fx-font-weight: bold;" : " -fx-text-fill: #2c3e50;")
        );

        nodeView.getChildren().addAll(rect, label);

        nodeView.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                currentNode = node;
                refreshCanvas(canvas, map);
                canvas.requestFocus();
            }
        });

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
