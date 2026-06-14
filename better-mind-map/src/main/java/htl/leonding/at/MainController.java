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
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.stream.Collectors;

public class MainController {

    @FXML private BorderPane rootPane;
    @FXML private TreeView<String> hierarchyTree;
    @FXML private TabPane tabPane;
    @FXML private HBox sidebarHeader;
    @FXML private Label syncStatusLabel;
    @FXML private Button syncBtn;

    private final MindMapRepository repository = new MindMapRepository();
    private final MindMapService service = new MindMapService(repository);
    private final SyncService syncService = new LocalSimulatedSyncService();

    private Node currentNode = null;
    private String userApiKey = null;

    @FXML
    public void initialize() {
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null && newTab.getUserData() instanceof MindMap) {
                MindMap map = (MindMap) newTab.getUserData();
                currentNode = getRoot(map);
                Pane viewport = (Pane) newTab.getContent();
                Pane canvas = (Pane) viewport.getChildren().get(0);
                refreshCanvas(canvas, map);
                updateSyncStatusLabel(map);
            }
        });

        // Sync sidebar header height with the TabPane tab-header-area after layout
        tabPane.needsLayoutProperty().addListener((obs, wasNeeded, needed) -> {
            if (!needed) syncSidebarHeaderHeight();
        });
        javafx.application.Platform.runLater(this::syncSidebarHeaderHeight);

        // Root-level key filter so navigation works regardless of which node has focus
        rootPane.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            Tab selected = tabPane.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            Object content = selected.getContent();
            Object data = selected.getUserData();
            if (content instanceof Pane && data instanceof MindMap) {
                Pane viewport = (Pane) content;
                if (!viewport.getChildren().isEmpty() && viewport.getChildren().get(0) instanceof Pane) {
                    handleKeyPress(e, (MindMap) data, (Pane) viewport.getChildren().get(0));
                }
            }
        });
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
        updateSyncStatusLabel(map);
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

    @FXML
    public void onOpenAiChat() {
        TextInputDialog dialog = new TextInputDialog("");
        dialog.setTitle("✨ AI Mindmap Assistant");
        dialog.setHeaderText("Worüber möchtest du eine Mindmap erstellen?");
        dialog.setContentText("Prompt (z.B. 'Aktien', 'Programmieren', 'Geschichte'):");
        dialog.getDialogPane().setPrefWidth(500);

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(prompt -> {
            if (prompt.trim().isEmpty()) return;
            
            MindMap map = service.createMindMap("AI: " + prompt.substring(0, Math.min(prompt.length(), 20)));
            Node root = getRoot(map);
            
            String apiKey = System.getenv("MINDMAP_AI_KEY");
            
            if (apiKey == null || apiKey.trim().isEmpty()) {
                if (userApiKey != null && !userApiKey.trim().isEmpty()) {
                    apiKey = userApiKey;
                } else {
                    TextInputDialog keyDialog = new TextInputDialog();
                    keyDialog.setTitle("API Key benötigt");
                    keyDialog.setHeaderText("Google Gemini API Key");
                    keyDialog.setContentText("Bitte gib deinen Gemini API Key ein:");
                    keyDialog.getDialogPane().setPrefWidth(400);
                    
                    Optional<String> keyResult = keyDialog.showAndWait();
                    if (keyResult.isPresent() && !keyResult.get().trim().isEmpty()) {
                        userApiKey = keyResult.get().trim();
                        apiKey = userApiKey;
                    }
                }
            }
            
            if (apiKey != null && !apiKey.trim().isEmpty()) {
                callGeminiApi(map, root, prompt, apiKey);
            } else {
                System.out.println("Kein API Key gefunden oder eingegeben. Nutze lokale Mock-KI.");
                generateSmarterMockAiMindMap(map, root, prompt);
            }
            
            currentNode = root;
            renderMindMap(map);
        });
    }

    private void callGeminiApi(MindMap map, Node root, String prompt, String apiKey) {
        try {
            String aiPrompt = "Du bist ein Mindmap-Experte. Erstelle zum Thema '" + prompt + "' eine extrem detaillierte und logisch strukturierte Mindmap mit den wichtigsten Begriffen. " +
                    "WICHTIG: Antworte AUSSCHLIESSLICH im folgenden Format, OHNE Markdown, OHNE Text davor oder danach. " +
                    "Zeile 1 MUSS das Hauptthema sein.\n" +
                    "Zeile 2 und weiter für Kategorien und Unterkategorien:\n" +
                    "Kategorie 1\n- Unterkategorie 1.1\n- Unterkategorie 1.2\nKategorie 2\n- Unterkategorie 2.1";

            // Gemini API JSON (gemini-2.5-flash)
            String jsonPayload = "{\"contents\": [{\"parts\": [{\"text\": \"" + aiPrompt.replace("\"", "\\\"") + "\"}]}]}";

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();

            // Setze root Text schon mal auf den prompt, falls was schief geht
            service.updateNodeText(map, root, prompt.substring(0, Math.min(prompt.length(), 20)));

            int textIndex = responseBody.indexOf("\"text\": \"");
            if (textIndex > -1) {
                int startIndex = textIndex + 9;
                int endIndex = responseBody.indexOf("\"", startIndex);
                if (endIndex == -1) endIndex = responseBody.length() - 1; // Fallback
                
                String content = responseBody.substring(startIndex, endIndex);
                content = content.replace("\\n", "\n").replace("\\\"", "\"").replace("\\*", "");

                Node currentMain = null;
                boolean isFirstLine = true;
                
                for (String line : content.split("\n")) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("```") || line.toLowerCase().contains("hier ist die mindmap")) continue;
                    
                    if (isFirstLine) {
                        service.updateNodeText(map, root, line);
                        isFirstLine = false;
                        continue;
                    }
                    
                    if (line.startsWith("- ")) {
                        if (currentMain != null) {
                            service.addNode(map, currentMain.getId(), line.substring(2).trim());
                        }
                    } else {
                        currentMain = service.addNode(map, root.getId(), line);
                    }
                }
            } else {
                System.out.println("Konnte API Response nicht lesen. Nutze Fallback.");
                generateSmarterMockAiMindMap(map, root, prompt);
            }
        } catch (Exception e) {
            e.printStackTrace();
            generateSmarterMockAiMindMap(map, root, prompt);
        }
    }

    private void generateSmarterMockAiMindMap(MindMap map, Node root, String prompt) {
        String p = prompt.toLowerCase();
        
        if (p.contains("aktien") || p.contains("börse") || p.contains("stock") || p.contains("investieren")) {
            Node basics = service.addNode(map, root.getId(), "Grundlagen");
            service.addNode(map, basics.getId(), "Börse & Handel");
            service.addNode(map, basics.getId(), "Broker");
            service.addNode(map, basics.getId(), "Dividende");

            Node types = service.addNode(map, root.getId(), "Anlageklassen");
            service.addNode(map, types.getId(), "Einzelaktien");
            service.addNode(map, types.getId(), "ETFs");
            service.addNode(map, types.getId(), "Aktienfonds");

            Node strategy = service.addNode(map, root.getId(), "Strategie");
            service.addNode(map, strategy.getId(), "Buy & Hold");
            service.addNode(map, strategy.getId(), "Daytrading");
            service.addNode(map, strategy.getId(), "Value Investing");
            
            Node risks = service.addNode(map, root.getId(), "Risiken");
            service.addNode(map, risks.getId(), "Kursschwankungen");
            service.addNode(map, risks.getId(), "Inflation");
            service.addNode(map, risks.getId(), "Totalverlust");

        } else if (p.contains("java") || p.contains("programmieren") || p.contains("software")) {
            Node concepts = service.addNode(map, root.getId(), "Konzepte");
            service.addNode(map, concepts.getId(), "Objektorientierung");
            service.addNode(map, concepts.getId(), "Datenstrukturen");
            
            Node tools = service.addNode(map, root.getId(), "Tools");
            service.addNode(map, tools.getId(), "IDE (IntelliJ, Eclipse)");
            service.addNode(map, tools.getId(), "Git & GitHub");
            
            Node languages = service.addNode(map, root.getId(), "Sprachen");
            service.addNode(map, languages.getId(), "Java");
            service.addNode(map, languages.getId(), "Python");
            service.addNode(map, languages.getId(), "JavaScript");
            
        } else {
            // Generisches Fallback für alle anderen Prompts
            Node info = service.addNode(map, root.getId(), "Was ist das?");
            service.addNode(map, info.getId(), "Definition");
            service.addNode(map, info.getId(), "Ursprung");
            
            Node proCon = service.addNode(map, root.getId(), "Vor- & Nachteile");
            service.addNode(map, proCon.getId(), "Vorteile");
            service.addNode(map, proCon.getId(), "Nachteile");
            
            Node examples = service.addNode(map, root.getId(), "Beispiele & Nutzung");
            service.addNode(map, examples.getId(), "Praxisbeispiele");
            service.addNode(map, examples.getId(), "Anwendungsfälle");
        }
    }

    private void renderMindMap(MindMap map) {
        Pane viewport = new Pane();
        viewport.setFocusTraversable(true);
        viewport.setOnMouseClicked(e -> viewport.requestFocus());
        viewport.setStyle("-fx-background-color: #f8f9fa;");

        Pane canvas = new Pane();
        viewport.getChildren().add(canvas);

        setupZoomAndPan(viewport, canvas);

        // Re-layout when viewport gets its actual size on first display
        viewport.widthProperty().addListener((obs, oldW, newW) -> {
            if (newW.doubleValue() > 10 && viewport.getHeight() > 10) {
                refreshCanvas(canvas, map);
            }
        });

        Tab tab = new Tab(map.getName());
        tab.setContent(viewport);
        tab.setUserData(map);
        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);
        refreshCanvas(canvas, map);
        viewport.requestFocus();
    }

    private void setupZoomAndPan(Pane viewport, Pane canvas) {
        final double SCALE_DELTA = 1.1;

        viewport.setOnScroll(event -> {
            event.consume();
            if (event.getDeltaY() == 0) return;

            double scaleFactor = (event.getDeltaY() > 0) ? SCALE_DELTA : 1 / SCALE_DELTA;
            double newScale = canvas.getScaleX() * scaleFactor;

            if (newScale < 0.2 || newScale > 5.0) return;

            double f = (scaleFactor - 1);
            double dx = (event.getX() - (canvas.getBoundsInParent().getWidth() / 2 + canvas.getBoundsInParent().getMinX()));
            double dy = (event.getY() - (canvas.getBoundsInParent().getHeight() / 2 + canvas.getBoundsInParent().getMinY()));

            canvas.setScaleX(newScale);
            canvas.setScaleY(newScale);
            canvas.setTranslateX(canvas.getTranslateX() - f * dx);
            canvas.setTranslateY(canvas.getTranslateY() - f * dy);
        });

        final double[] dragContext = new double[2];
        final boolean[] isDragging = new boolean[1];

        viewport.setOnMousePressed(event -> {
            if (event.getTarget() == viewport || event.getTarget() == canvas) {
                if (event.getButton() == MouseButton.PRIMARY || event.getButton() == MouseButton.SECONDARY) {
                    dragContext[0] = event.getSceneX() - canvas.getTranslateX();
                    dragContext[1] = event.getSceneY() - canvas.getTranslateY();
                    viewport.setCursor(javafx.scene.Cursor.CLOSED_HAND);
                    isDragging[0] = true;
                    event.consume();
                }
            }
        });

        viewport.setOnMouseDragged(event -> {
            if (isDragging[0]) {
                canvas.setTranslateX(event.getSceneX() - dragContext[0]);
                canvas.setTranslateY(event.getSceneY() - dragContext[1]);
                event.consume();
            }
        });

        viewport.setOnMouseReleased(event -> {
            if (isDragging[0]) {
                viewport.setCursor(javafx.scene.Cursor.DEFAULT);
                isDragging[0] = false;
                event.consume();
            }
        });
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
            if (isOnLeftSide(map, currentNode)) {
                navigateToParent(map, canvas);
            } else {
                List<Node> children = getChildren(map, currentNode);
                if (!children.isEmpty()) {
                    currentNode = children.get(0);
                    refreshCanvas(canvas, map);
                }
            }
            e.consume();

        } else if (code == KeyCode.LEFT) {
            if (currentNode.getParentId() == null) {
                // Root: go to left-side children
                List<Node> all = getChildren(map, currentNode);
                int rightCount = (all.size() + 1) / 2;
                List<Node> leftChildren = all.subList(rightCount, all.size());
                if (!leftChildren.isEmpty()) {
                    currentNode = leftChildren.get(0);
                    refreshCanvas(canvas, map);
                }
            } else if (isOnLeftSide(map, currentNode)) {
                // Left-side node: go deeper to children
                List<Node> children = getChildren(map, currentNode);
                if (!children.isEmpty()) {
                    currentNode = children.get(0);
                    refreshCanvas(canvas, map);
                }
            } else {
                // Right-side node: go to parent
                navigateToParent(map, canvas);
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
                canvas.requestFocus();
            }
            e.consume();
        }
    }

    private void navigateSibling(MindMap map, Pane canvas, int direction) {
        if (currentNode.getParentId() == null) return;
        boolean onLeft = isOnLeftSide(map, currentNode);
        List<Node> siblings = map.getNodes().stream()
                .filter(n -> currentNode.getParentId().equals(n.getParentId()))
                .filter(n -> isOnLeftSide(map, n) == onLeft)
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

    private void navigateToParent(MindMap map, Pane canvas) {
        if (currentNode.getParentId() == null) return;
        map.getNodes().stream()
                .filter(n -> n.getId().equals(currentNode.getParentId()))
                .findFirst()
                .ifPresent(parent -> {
                    currentNode = parent;
                    refreshCanvas(canvas, map);
                });
    }

    private boolean isOnLeftSide(MindMap map, Node node) {
        if (node.getParentId() == null) return false;
        Node root = getRoot(map);
        return root != null && node.getXCoordinate() < root.getXCoordinate();
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
        Pane viewport = (Pane) canvas.getParent();
        double w = (viewport != null && viewport.getWidth() > 0) ? viewport.getWidth() : 800;
        double h = (viewport != null && viewport.getHeight() > 0) ? viewport.getHeight() : 600;

        // Layout only if root has no coordinates (e.g. initially)
        Node root = getRoot(map);
        if (root != null && root.getXCoordinate() == 0 && root.getYCoordinate() == 0) {
            layoutMindMap(map, w, h);
        }

        canvas.getChildren().clear();
        canvas.setStyle("-fx-background-color: transparent;");

        // Edges (drawn first, appear behind nodes)
        drawLines(canvas, map);

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
        updateSyncStatusLabel(map);
    }

    private void drawLines(Pane canvas, MindMap map) {
        canvas.getChildren().removeIf(n -> n instanceof Line);
        int index = 0;
        for (Node node : map.getNodes()) {
            if (node.getParentId() == null) continue;
            Node parent = map.getNodes().stream()
                    .filter(p -> p.getId().equals(node.getParentId()))
                    .findFirst().orElse(null);
            if (parent != null) {
                Line line = new Line(
                        parent.getXCoordinate(), parent.getYCoordinate(),
                        node.getXCoordinate(), node.getYCoordinate()
                );
                line.setStroke(Color.web("#adb5bd"));
                line.setStrokeWidth(2);
                canvas.getChildren().add(index++, line);
            }
        }
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
            if (e.getButton() == MouseButton.PRIMARY && !e.isConsumed()) {
                currentNode = node;
                refreshCanvas(canvas, map);
                canvas.requestFocus();
            }
        });

        final double[] dragDelta = new double[2];

        nodeView.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                dragDelta[0] = nodeView.getLayoutX() - e.getSceneX();
                dragDelta[1] = nodeView.getLayoutY() - e.getSceneY();
                e.consume();
            }
        });

        nodeView.setOnMouseDragged(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                double newX = e.getSceneX() + dragDelta[0];
                double newY = e.getSceneY() + dragDelta[1];
                nodeView.setLayoutX(newX);
                nodeView.setLayoutY(newY);
                node.setXCoordinate(newX + NODE_W / 2);
                node.setYCoordinate(newY + NODE_H / 2);
                
                drawLines(canvas, map);
                e.consume();
            }
        });

        nodeView.setOnMouseReleased(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                repository.updateNode(node);
                e.consume();
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
                canvas.requestFocus();
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

    private void updateSyncStatusLabel(MindMap map) {
        if (syncStatusLabel != null) {
            String status = map.getSyncStatus();
            syncStatusLabel.setText("Cloud Sync: " + (status != null ? status : "PENDING"));
        }
    }

    @FXML
    private void onSync() {
        Tab selected = tabPane.getSelectionModel().getSelectedItem();
        if (selected == null || !(selected.getUserData() instanceof MindMap)) return;
        MindMap map = (MindMap) selected.getUserData();

        syncStatusLabel.setText("Cloud Sync: SYNCING...");
        syncBtn.setDisable(true);

        javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<>() {
            @Override
            protected Void call() throws Exception {
                Thread.sleep(1500);
                syncService.syncMap(map.getId());
                return null;
            }

            @Override
            protected void succeeded() {
                map.setSyncStatus("SYNCED");
                syncStatusLabel.setText("Cloud Sync: SYNCED");
                syncBtn.setDisable(false);
            }

            @Override
            protected void failed() {
                syncStatusLabel.setText("Cloud Sync: FAILED");
                syncBtn.setDisable(false);
            }
        };
        new Thread(task).start();
    }
}
