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
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
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

    @FXML private HBox toolbar;
    @FXML private VBox sidebar;
    @FXML private HBox statusbar;

    private boolean isPresentationModeActive = false;
    private VBox activeHud = null;
    private String currentPresentationTheme = "LIGHT"; // LIGHT, DARK, SEPIA, OCEAN

    private final MindMapRepository repository = new MindMapRepository();
    private final MindMapService service = new MindMapService(repository);
    private final SyncService syncService = new LocalSimulatedSyncService();

    private Node currentNode = null;
    private String userApiKey = null;
    private final Map<TreeItem<String>, String> treeItemToNodeId = new HashMap<>();
    private boolean suppressTreeSelection = false;
    private final Set<String> expandedNodeIds = new HashSet<>();

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

        hierarchyTree.getSelectionModel().selectedItemProperty().addListener((obs, oldItem, newItem) -> {
            if (suppressTreeSelection || newItem == null) return;
            String nodeId = treeItemToNodeId.get(newItem);
            if (nodeId == null) return;
            Tab selected = tabPane.getSelectionModel().getSelectedItem();
            if (selected == null || !(selected.getUserData() instanceof MindMap)) return;
            MindMap map = (MindMap) selected.getUserData();
            Node node = map.getNodes().stream()
                    .filter(n -> n.getId().equals(nodeId))
                    .findFirst().orElse(null);
            if (node == null || node == currentNode) return;
            currentNode = node;
            Pane viewport = (Pane) selected.getContent();
            if (!viewport.getChildren().isEmpty()) {
                refreshCanvas((Pane) viewport.getChildren().get(0), map);
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

    public Scene getRootScene() {
        return rootPane.getScene();
    }

    public void openMapAsTab(MindMap map) {
        for (Tab tab : tabPane.getTabs()) {
            if (tab.getUserData() instanceof MindMap && ((MindMap) tab.getUserData()).getId().equals(map.getId())) {
                tabPane.getSelectionModel().select(tab);
                return;
            }
        }
        currentNode = getRoot(map);
        renderMindMap(map);
    }

    @FXML
    private void onBackToOverview() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("overview-view.fxml"));
            Scene scene = new Scene(loader.load(), 1024, 768);
            OverviewController controller = loader.getController();
            controller.setMainController(this);
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
        viewport.setStyle("-fx-background-color: #f1f5f9;");

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
        tab.setClosable(true);
        tab.setOnCloseRequest(e -> {
            e.consume();
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Mind Map löschen");
            confirm.setHeaderText("\"" + map.getName() + "\" löschen?");
            confirm.setContentText("Die Mind Map wird dauerhaft gelöscht und kann nicht wiederhergestellt werden.");
            confirm.showAndWait().ifPresent(btn -> {
                if (btn == ButtonType.OK) {
                    tabPane.getTabs().remove(tab);
                    repository.deleteMindMap(map.getId());
                }
            });
        });
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

        } else if (code == KeyCode.RIGHT || code == KeyCode.LEFT || code == KeyCode.UP || code == KeyCode.DOWN) {
            navigateSpatial(map, canvas, code);
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

    private void navigateSpatial(MindMap map, Pane canvas, KeyCode code) {
        if (currentNode == null) return;

        double ax = currentNode.getXCoordinate();
        double ay = currentNode.getYCoordinate();

        Node bestCandidate = null;
        double bestScore = Double.MAX_VALUE;

        // Weight factor to penalize orthogonal deviation
        double k = 2.0;

        for (Node node : map.getNodes()) {
            if (node == currentNode) continue;

            double bx = node.getXCoordinate();
            double by = node.getYCoordinate();

            double dx = bx - ax;
            double dy = by - ay;
            double score = Double.MAX_VALUE;

            switch (code) {
                case RIGHT:
                    if (dx > 5.0) {
                        score = dx + k * Math.abs(dy);
                    }
                    break;
                case LEFT:
                    if (dx < -5.0) {
                        score = -dx + k * Math.abs(dy);
                    }
                    break;
                case DOWN:
                    if (dy > 5.0) {
                        score = dy + k * Math.abs(dx);
                    }
                    break;
                case UP:
                    if (dy < -5.0) {
                        score = -dy + k * Math.abs(dx);
                    }
                    break;
                default:
                    break;
            }

            if (score < bestScore) {
                bestScore = score;
                bestCandidate = node;
            }
        }

        if (bestCandidate != null) {
            currentNode = bestCandidate;
            refreshCanvas(canvas, map);
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
        currentPresentationTheme = map.getTheme();

        Pane viewport = (Pane) canvas.getParent();
        double w = (viewport != null && viewport.getWidth() > 0) ? viewport.getWidth() : 800;
        double h = (viewport != null && viewport.getHeight() > 0) ? viewport.getHeight() : 600;

        // Layout only if root has no coordinates (e.g. initially)
        Node root = getRoot(map);
        if (root != null && root.getXCoordinate() == 0 && root.getYCoordinate() == 0) {
            layoutMindMap(map, w, h);
        }

        if (viewport != null) {
            switch (currentPresentationTheme) {
                case "DARK":
                    viewport.setStyle("-fx-background-color: #0d1117;");
                    break;
                case "SEPIA":
                    viewport.setStyle("-fx-background-color: #f5f0e8;");
                    break;
                case "OCEAN":
                    viewport.setStyle("-fx-background-color: #e0f7ff;");
                    break;
                case "LIGHT":
                default:
                    viewport.setStyle("-fx-background-color: #f1f5f9;");
                    break;
            }
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
            double nodeW = getNodeWidth(node);
            double nodeH = getNodeHeight(node);
            nodeView.setLayoutX(node.getXCoordinate() - nodeW / 2);
            nodeView.setLayoutY(node.getYCoordinate() - nodeH / 2);
            canvas.getChildren().add(nodeView);
        }

        // Always show the floating HUD overlay
        if (viewport != null) {
            showPresentationHud(viewport, canvas, map);
        }

        refreshTree(map);
        updateSyncStatusLabel(map);
    }

    private void drawLines(Pane canvas, MindMap map) {
        Color lineColor;
        switch (currentPresentationTheme) {
            case "DARK":
                lineColor = Color.web("#2a3245");
                break;
            case "SEPIA":
                lineColor = Color.web("#c5b49a");
                break;
            case "OCEAN":
                lineColor = Color.web("#93c5fd");
                break;
            case "LIGHT":
            default:
                lineColor = Color.web("#cbd5e1");
                break;
        }

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
                line.setStroke(lineColor);
                line.setStrokeWidth(1.5);
                line.setOpacity(0.8);
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

    private Shape buildNodeShape(String shapeName, double nodeW, double nodeH) {
        switch (shapeName) {
            case "PILL": {
                Rectangle r = new Rectangle(nodeW, nodeH);
                r.setArcWidth(nodeH);
                r.setArcHeight(nodeH);
                return r;
            }
            case "ELLIPSE":
                return new Ellipse(nodeW / 2, nodeH / 2);
            case "DIAMOND":
                return new Polygon(
                    nodeW / 2, 0.0,
                    nodeW,     nodeH / 2,
                    nodeW / 2, nodeH,
                    0.0,       nodeH / 2
                );
            default: { // ROUNDED_RECT
                Rectangle r = new Rectangle(nodeW, nodeH);
                r.setArcWidth(NODE_ARC * 2);
                r.setArcHeight(NODE_ARC * 2);
                return r;
            }
        }
    }

    private StackPane createNodeView(Node node, MindMap map, Pane canvas,
                                     boolean isCurrent, boolean isRoot) {
        StackPane nodeView = new StackPane();

        double nodeW = getNodeWidth(node);
        double nodeH = getNodeHeight(node);
        nodeView.setPrefSize(nodeW, nodeH);
        nodeView.setMinSize(nodeW, nodeH);
        nodeView.setMaxSize(nodeW, nodeH);

        Shape rect = buildNodeShape(node.getShape(), nodeW, nodeH);

        boolean isDarkCanvas = "DARK".equals(currentPresentationTheme);
        Color textColor;

        if (isRoot && (node.getColor() == null || node.getColor().equals("#ffffff"))) {
            // Root node: vivid indigo gradient
            rect.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web("#6366f1")),
                    new Stop(1, Color.web("#4338ca"))));
            if (isCurrent) {
                rect.setStroke(Color.web("#a5b4fc"));
                rect.setStrokeWidth(2.5);
                rect.setEffect(new javafx.scene.effect.DropShadow(18, 0, 4, Color.web("#6366f155")));
            } else {
                rect.setStroke(Color.web("#4338ca"));
                rect.setStrokeWidth(1.5);
                rect.setEffect(new javafx.scene.effect.DropShadow(12, 0, 3, Color.web("#6366f133")));
            }
            textColor = Color.WHITE;
        } else {
            String colStr = node.getColor();
            if (colStr == null || colStr.isEmpty()) colStr = isDarkCanvas ? "#1e2433" : "#ffffff";

            if (isDarkCanvas && (colStr.equals("#ffffff") || colStr.equals("#1e2433"))) {
                rect.setFill(Color.web("#1e2433"));
            } else {
                rect.setFill(Color.web(colStr));
            }

            if (isCurrent) {
                rect.setStroke(Color.web("#6366f1"));
                rect.setStrokeWidth(2.5);
                rect.setEffect(new javafx.scene.effect.DropShadow(14, 0, 3, Color.web("#6366f144")));
            } else {
                Color borderColor = isDarkCanvas ? Color.web("#2a3245") : Color.web("#e2e8f0");
                rect.setStroke(borderColor);
                rect.setStrokeWidth(1.5);
                rect.setEffect(new javafx.scene.effect.DropShadow(6, 0, 2, Color.web("#00000022")));
            }
            textColor = isDarkCanvas && (colStr.equals("#1e2433") || colStr.equals("#ffffff"))
                    ? Color.web("#e2e8f0")
                    : getContrastColor(colStr);
        }

        Label label = new Label(node.getText());
        String shapeName = node.getShape();
        double labelMaxW = "DIAMOND".equals(shapeName) ? nodeW * 0.52
                         : "ELLIPSE".equals(shapeName)  ? nodeW * 0.68
                         : nodeW - 14;
        label.setMaxWidth(labelMaxW);
        label.setWrapText(true);
        label.setTextFill(textColor);
        label.setStyle(
            "-fx-font-size: " + node.getTextSize() + "px; -fx-text-alignment: center; -fx-alignment: center;" +
            (isRoot && (node.getColor() == null || node.getColor().equals("#ffffff")) ? " -fx-font-weight: bold;" : "")
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
                node.setXCoordinate(newX + nodeW / 2);
                node.setYCoordinate(newY + nodeH / 2);
                
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

    private double getNodeWidth(Node node) {
        String text = node.getText() == null ? "" : node.getText();
        double fontSize = node.getTextSize();
        double avgCharW = fontSize * 0.57;
        double textW = text.length() * avgCharW + 28;
        double minW = 90 + Math.max(0, (fontSize - 12) * 4);
        double maxW = 210;
        // Diamond/Ellipse need more horizontal room for readable text
        String shape = node.getShape();
        if ("DIAMOND".equals(shape) || "ELLIPSE".equals(shape)) {
            minW = Math.max(minW, 110);
            maxW = 230;
            textW *= 1.25;
        }
        return Math.max(minW, Math.min(maxW, textW));
    }

    private double getNodeHeight(Node node) {
        String text = node.getText() == null ? "" : node.getText();
        double fontSize = node.getTextSize();
        double nodeW = getNodeWidth(node);
        // Effective inner width depends on shape
        String shape = node.getShape();
        double innerW = "DIAMOND".equals(shape) ? nodeW * 0.5
                      : "ELLIPSE".equals(shape)  ? nodeW * 0.65
                      : nodeW - 20;
        double avgCharW = fontSize * 0.57;
        double charsPerLine = Math.max(1, innerW / avgCharW);
        int lines = Math.max(1, (int) Math.ceil(text.length() / charsPerLine));
        double lineH = fontSize + 5;
        double minH = fontSize + 20;
        // Diamond/Ellipse need extra vertical space
        if ("DIAMOND".equals(shape)) minH = Math.max(minH, nodeW * 0.6);
        if ("ELLIPSE".equals(shape))  minH = Math.max(minH, nodeW * 0.5);
        return Math.max(minH, lines * lineH + 16);
    }

    private Color getContrastColor(String hexColor) {
        if (hexColor == null || hexColor.isEmpty() || hexColor.equals("#ffffff")) {
            return Color.web("#2c3e50");
        }
        try {
            Color color = Color.web(hexColor);
            double brightness = 0.299 * color.getRed() + 0.587 * color.getGreen() + 0.114 * color.getBlue();
            return brightness < 0.5 ? Color.WHITE : Color.web("#2c3e50");
        } catch (Exception e) {
            return Color.web("#2c3e50");
        }
    }

    @FXML
    public void onTogglePresentationMode() {
        Stage stage = (Stage) rootPane.getScene().getWindow();
        if (!isPresentationModeActive) {
            enterPresentationMode(stage);
        } else {
            exitPresentationMode(stage);
        }
    }

    private void enterPresentationMode(Stage stage) {
        isPresentationModeActive = true;
        stage.setFullScreen(true);

        // Hide toolbar, sidebar, statusbar
        toolbar.setVisible(false);
        toolbar.setManaged(false);
        sidebar.setVisible(false);
        sidebar.setManaged(false);
        statusbar.setVisible(false);
        statusbar.setManaged(false);

        // Add CSS class to tabPane
        tabPane.getStyleClass().add("presentation-mode");

        // Listen for ESC key or window loss of focus/fullscreen change
        stage.fullScreenProperty().addListener(new javafx.beans.value.ChangeListener<Boolean>() {
            @Override
            public void changed(javafx.beans.value.ObservableValue<? extends Boolean> obs, Boolean wasFS, Boolean isFS) {
                if (!isFS && isPresentationModeActive) {
                    exitPresentationMode(stage);
                    stage.fullScreenProperty().removeListener(this);
                }
            }
        });

        // Re-render current tab's canvas to draw HUD, then center the diagram
        Tab selected = tabPane.getSelectionModel().getSelectedItem();
        if (selected != null && selected.getUserData() instanceof MindMap) {
            MindMap map = (MindMap) selected.getUserData();
            Pane viewport = (Pane) selected.getContent();
            Pane canvas = (Pane) viewport.getChildren().get(0);
            refreshCanvas(canvas, map);
            // Double runLater to wait for fullscreen layout to settle
            javafx.application.Platform.runLater(() ->
                javafx.application.Platform.runLater(() ->
                    centerCanvasInViewport(viewport, canvas, map)
                )
            );
        }
    }

    private void centerCanvasInViewport(Pane viewport, Pane canvas, MindMap map) {
        if (map.getNodes().isEmpty() || viewport.getWidth() <= 0 || viewport.getHeight() <= 0) return;
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (Node n : map.getNodes()) {
            minX = Math.min(minX, n.getXCoordinate());
            minY = Math.min(minY, n.getYCoordinate());
            maxX = Math.max(maxX, n.getXCoordinate());
            maxY = Math.max(maxY, n.getYCoordinate());
        }
        double contentCenterX = (minX + maxX) / 2;
        double contentCenterY = (minY + maxY) / 2;
        canvas.setTranslateX(viewport.getWidth()  / 2 - contentCenterX * canvas.getScaleX());
        canvas.setTranslateY(viewport.getHeight() / 2 - contentCenterY * canvas.getScaleY());
    }

    private void exitPresentationMode(Stage stage) {
        isPresentationModeActive = false;
        if (stage.isFullScreen()) {
            stage.setFullScreen(false);
        }

        // Show toolbar, sidebar, statusbar
        toolbar.setVisible(true);
        toolbar.setManaged(true);
        sidebar.setVisible(true);
        sidebar.setManaged(true);
        statusbar.setVisible(true);
        statusbar.setManaged(true);

        // Remove CSS class from tabPane
        tabPane.getStyleClass().remove("presentation-mode");

        // Remove active HUD
        if (activeHud != null) {
            Pane parent = (Pane) activeHud.getParent();
            if (parent != null) {
                parent.getChildren().remove(activeHud);
            }
            activeHud = null;
        }

        // Re-render current tab's canvas, then re-center once the viewport has settled
        Tab selected = tabPane.getSelectionModel().getSelectedItem();
        if (selected != null && selected.getUserData() instanceof MindMap) {
            MindMap map = (MindMap) selected.getUserData();
            Pane viewport = (Pane) selected.getContent();
            Pane canvas = (Pane) viewport.getChildren().get(0);
            refreshCanvas(canvas, map);

            // stage.setFullScreen(false) is async — wait for the viewport to actually resize
            final boolean[] centered = {false};
            final java.util.concurrent.atomic.AtomicReference<javafx.beans.value.ChangeListener<Number>> ref =
                    new java.util.concurrent.atomic.AtomicReference<>();
            javafx.beans.value.ChangeListener<Number> listener = (obs, oldW, newW) -> {
                if (!centered[0]) {
                    centered[0] = true;
                    viewport.widthProperty().removeListener(ref.get());
                    javafx.application.Platform.runLater(() -> centerCanvasInViewport(viewport, canvas, map));
                }
            };
            ref.set(listener);
            viewport.widthProperty().addListener(listener);
            // Fallback: center after layout passes if viewport width never changed
            javafx.application.Platform.runLater(() ->
                javafx.application.Platform.runLater(() ->
                    javafx.application.Platform.runLater(() -> {
                        viewport.widthProperty().removeListener(ref.get());
                        if (!centered[0]) {
                            centered[0] = true;
                            centerCanvasInViewport(viewport, canvas, map);
                        }
                    })
                )
            );
        }
    }

    private void showPresentationHud(Pane viewport, Pane canvas, MindMap map) {
        if (activeHud != null) {
            Pane parent = (Pane) activeHud.getParent();
            if (parent != null) parent.getChildren().remove(activeHud);
            activeHud = null;
        }

        VBox hud = new VBox();
        hud.getStyleClass().add("presentation-hud");

        // Title
        Label title = new Label(isPresentationModeActive ? "PRESENTATION CONTROLS" : "NODE STYLING");
        title.getStyleClass().add("hud-title");
        hud.getChildren().add(title);

        // Selected Node Section — only visible outside presentation mode
        if (!isPresentationModeActive) {
            if (currentNode != null) {
                VBox nodeSection = new VBox();
                nodeSection.getStyleClass().add("hud-section");

                Label selectedLabel = new Label("Selected Node: " + currentNode.getText());
                selectedLabel.getStyleClass().add("hud-label");
                selectedLabel.setStyle("-fx-font-weight: bold;");
                nodeSection.getChildren().add(selectedLabel);

                // Font Size Controls
                HBox sizeBox = new HBox();
                sizeBox.setSpacing(10);
                sizeBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                Label sizeLabel = new Label("Text Size: " + (int) currentNode.getTextSize() + "px");
                sizeLabel.getStyleClass().add("hud-label");

                Button btnMinus = new Button("A-");
                btnMinus.getStyleClass().add("btn-hud");
                btnMinus.setOnAction(e -> {
                    double newSize = Math.max(8.0, currentNode.getTextSize() - 2.0);
                    currentNode.setTextSize(newSize);
                    repository.updateNode(currentNode);
                    refreshCanvas(canvas, map);
                });

                Button btnPlus = new Button("A+");
                btnPlus.getStyleClass().add("btn-hud");
                btnPlus.setOnAction(e -> {
                    double newSize = Math.min(36.0, currentNode.getTextSize() + 2.0);
                    currentNode.setTextSize(newSize);
                    repository.updateNode(currentNode);
                    refreshCanvas(canvas, map);
                });

                sizeBox.getChildren().addAll(btnMinus, btnPlus, sizeLabel);
                nodeSection.getChildren().add(sizeBox);

                // Color preset swatches
                HBox colorBox = new HBox();
                colorBox.setSpacing(6);
                colorBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                Label colorLabel = new Label("Color: ");
                colorLabel.getStyleClass().add("hud-label");
                colorBox.getChildren().add(colorLabel);

                String[] colorPresets = {"#ffffff", "#3498db", "#2ecc71", "#f1c40f", "#e67e22", "#e74c3c", "#9b59b6"};
                for (String col : colorPresets) {
                    Button swatch = new Button();
                    swatch.getStyleClass().add("color-swatch");
                    swatch.setStyle("-fx-background-color: " + col + ";");
                    swatch.setOnAction(e -> {
                        currentNode.setColor(col);
                        repository.updateNode(currentNode);
                        refreshCanvas(canvas, map);
                    });
                    colorBox.getChildren().add(swatch);
                }
                nodeSection.getChildren().add(colorBox);

                // Shape picker
                HBox shapeBox = new HBox();
                shapeBox.setSpacing(5);
                shapeBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                Label shapeLabel = new Label("Shape: ");
                shapeLabel.getStyleClass().add("hud-label");
                shapeBox.getChildren().add(shapeLabel);

                String[][] shapes = {
                    {"ROUNDED_RECT", "▭"},
                    {"PILL",         "⬬"},
                    {"ELLIPSE",      "⬭"},
                    {"DIAMOND",      "◇"}
                };
                for (String[] s : shapes) {
                    String shapeKey = s[0];
                    Button shapeBtn = new Button(s[1]);
                    shapeBtn.getStyleClass().add("btn-hud");
                    shapeBtn.setStyle("-fx-font-size: 14px; -fx-padding: 3 8;" +
                        (currentNode.getShape().equals(shapeKey)
                            ? " -fx-border-color: #6366f1; -fx-text-fill: #a5b4fc;"
                            : ""));
                    shapeBtn.setOnAction(e -> {
                        currentNode.setShape(shapeKey);
                        repository.updateNode(currentNode);
                        refreshCanvas(canvas, map);
                    });
                    shapeBox.getChildren().add(shapeBtn);
                }
                nodeSection.getChildren().add(shapeBox);

                hud.getChildren().add(nodeSection);
            } else {
                Label noSelectLabel = new Label("Select a node to style it");
                noSelectLabel.getStyleClass().add("hud-label");
                noSelectLabel.setStyle("-fx-font-style: italic; -fx-text-fill: #a5b1c2;");
                hud.getChildren().add(noSelectLabel);
            }
        }

        // Separator
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: rgba(255,255,255,0.1);");
        hud.getChildren().add(sep);

        // Global Canvas Theme Section
        VBox themeSection = new VBox();
        themeSection.getStyleClass().add("hud-section");
        
        Label themeLabel = new Label("Canvas Theme:");
        themeLabel.getStyleClass().add("hud-label");
        themeSection.getChildren().add(themeLabel);

        HBox themeBtnBox = new HBox();
        themeBtnBox.setSpacing(6);
        
        Button btnLight = new Button("Light");
        btnLight.getStyleClass().add("btn-hud");
        if (currentPresentationTheme.equals("LIGHT")) btnLight.setStyle("-fx-background-color: rgba(255,255,255,0.35);");
        btnLight.setOnAction(e -> {
            map.setTheme("LIGHT");
            repository.updateTheme(map.getId(), "LIGHT");
            refreshCanvas(canvas, map);
        });

        Button btnDark = new Button("Dark");
        btnDark.getStyleClass().add("btn-hud");
        if (currentPresentationTheme.equals("DARK")) btnDark.setStyle("-fx-background-color: rgba(255,255,255,0.35);");
        btnDark.setOnAction(e -> {
            map.setTheme("DARK");
            repository.updateTheme(map.getId(), "DARK");
            refreshCanvas(canvas, map);
        });

        Button btnSepia = new Button("Sepia");
        btnSepia.getStyleClass().add("btn-hud");
        if (currentPresentationTheme.equals("SEPIA")) btnSepia.setStyle("-fx-background-color: rgba(255,255,255,0.35);");
        btnSepia.setOnAction(e -> {
            map.setTheme("SEPIA");
            repository.updateTheme(map.getId(), "SEPIA");
            refreshCanvas(canvas, map);
        });

        Button btnOcean = new Button("Ocean");
        btnOcean.getStyleClass().add("btn-hud");
        if (currentPresentationTheme.equals("OCEAN")) btnOcean.setStyle("-fx-background-color: rgba(255,255,255,0.35);");
        btnOcean.setOnAction(e -> {
            map.setTheme("OCEAN");
            repository.updateTheme(map.getId(), "OCEAN");
            refreshCanvas(canvas, map);
        });

        themeBtnBox.getChildren().addAll(btnLight, btnDark, btnSepia, btnOcean);
        themeSection.getChildren().add(themeBtnBox);
        hud.getChildren().add(themeSection);

        // Exit Button (only in presentation mode)
        if (isPresentationModeActive) {
            Button btnExit = new Button("Exit Presentation");
            btnExit.getStyleClass().add("btn-hud-danger");
            btnExit.setMaxWidth(Double.MAX_VALUE);
            btnExit.setOnAction(e -> {
                Stage stage = (Stage) viewport.getScene().getWindow();
                exitPresentationMode(stage);
            });
            hud.getChildren().add(btnExit);
        }

        // Bind layout to keep HUD in top-right corner
        hud.layoutXProperty().bind(viewport.widthProperty().subtract(hud.widthProperty()).subtract(20));
        hud.setLayoutY(20);

        viewport.getChildren().add(hud);
        activeHud = hud;
    }

    // ── Hierarchy tree ───────────────────────────────────────────────────────

    private void refreshTree(MindMap map) {
        // Save expansion state before rebuilding (uses treeItemToNodeId while still valid)
        if (hierarchyTree.getRoot() != null) {
            saveExpansionState(hierarchyTree.getRoot());
        }

        treeItemToNodeId.clear();
        Map<String, TreeItem<String>> itemMap = new HashMap<>();
        TreeItem<String> rootItem = null;
        TreeItem<String> currentItem = null;

        for (Node node : map.getNodes()) {
            TreeItem<String> item = new TreeItem<>(node.getText());
            itemMap.put(node.getId(), item);
            treeItemToNodeId.put(item, node.getId());
            if (node.getParentId() == null) rootItem = item;
            if (node == currentNode) currentItem = item;
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
            // Restore expansion: root always expanded, others only if previously expanded
            rootItem.setExpanded(true);
            for (Map.Entry<String, TreeItem<String>> entry : itemMap.entrySet()) {
                if (expandedNodeIds.contains(entry.getKey())) {
                    entry.getValue().setExpanded(true);
                }
            }

            suppressTreeSelection = true;
            hierarchyTree.setRoot(rootItem);
            if (currentItem != null) {
                hierarchyTree.getSelectionModel().select(currentItem);
            }
            javafx.application.Platform.runLater(() -> suppressTreeSelection = false);
        }
    }

    private void saveExpansionState(TreeItem<String> item) {
        String id = treeItemToNodeId.get(item);
        if (id != null) {
            if (item.isExpanded()) expandedNodeIds.add(id);
            else expandedNodeIds.remove(id);
        }
        for (TreeItem<String> child : item.getChildren()) {
            saveExpansionState(child);
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
