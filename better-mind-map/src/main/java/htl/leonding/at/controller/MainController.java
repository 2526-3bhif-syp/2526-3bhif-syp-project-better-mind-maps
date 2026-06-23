package htl.leonding.at.controller;
import htl.leonding.at.model.*;
import htl.leonding.at.controller.*;
import htl.leonding.at.service.*;
import htl.leonding.at.repository.*;
import htl.leonding.at.util.*;
import htl.leonding.at.App;


import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.util.Duration;
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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;

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

    @FXML private Button backBtn;
    @FXML private Button newMapBtn;
    @FXML private Button deleteMapBtn;
    @FXML private Button aiBtn;
    @FXML private Button exportBtn;
    @FXML private Button presentBtn;
    @FXML private Label structureLabel;
    @FXML private Label sbAddChild;
    @FXML private Label sbEdit;
    @FXML private Label sbDelete;
    @FXML private Label sbNavigate;
    @FXML private Label sbCycle;
    @FXML private Label sbZoom;
    @FXML private Label sbUndo;
    @FXML private Label sbRedo;

    private boolean isPresentationModeActive = false;
    private VBox activeHud = null;
    private Button activeHudToggle = null;
    private boolean hudExpanded = true;
    private static final int HUD_TAB_W = 26;
    private String currentPresentationTheme = "LIGHT"; // LIGHT, DARK, SEPIA, OCEAN
    private Label hudSelectedLabel;
    private Button hudBtnLight, hudBtnDark, hudBtnSepia, hudBtnOcean;
    private boolean hudPresentationMode = false;

    private final java.util.Deque<List<Node>> undoStack = new java.util.ArrayDeque<>();
    private final java.util.Deque<List<Node>> redoStack = new java.util.ArrayDeque<>();
    private static final int MAX_UNDO_HISTORY = 50;

    private boolean minimapExpanded = true;
    private static final int MM_W = 200;
    private static final int MM_H = 130;

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
        tabPane.setTabDragPolicy(TabPane.TabDragPolicy.REORDER);
        applyLanguage();

        tabPane.getTabs().addListener((javafx.collections.ListChangeListener<Tab>) change -> {
            if (tabPane.getTabs().isEmpty()) {
                onBackToOverview();
            }
        });

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
                MindMap map = (MindMap) data;
                if (!viewport.getChildren().isEmpty() && viewport.getChildren().get(0) instanceof Pane) {
                    Pane canvas = (Pane) viewport.getChildren().get(0);
                    if (e.isControlDown() && !e.isAltDown()) {
                        if (e.getCode() == KeyCode.Z && !e.isShiftDown()) {
                            performUndo(map, canvas);
                            e.consume();
                            return;
                        } else if (e.getCode() == KeyCode.Y
                                || (e.getCode() == KeyCode.Z && e.isShiftDown())) {
                            performRedo(map, canvas);
                            e.consume();
                            return;
                        }
                    }
                    handleKeyPress(e, map, canvas);
                }
            }
        });
    }

    public void applyLanguage() {
        if (backBtn != null)        backBtn.setText(LanguageManager.get("btn.back"));
        if (newMapBtn != null)      newMapBtn.setText(LanguageManager.get("btn.newmap"));
        if (deleteMapBtn != null)   deleteMapBtn.setText(LanguageManager.get("btn.deletemap"));
        if (aiBtn != null)          aiBtn.setText(LanguageManager.get("btn.aigenerate"));
        if (exportBtn != null)      exportBtn.setText(LanguageManager.get("btn.export"));
        if (presentBtn != null)     presentBtn.setText(LanguageManager.get("btn.present"));
        if (structureLabel != null) structureLabel.setText(LanguageManager.get("sidebar.structure"));
        if (sbAddChild != null)     sbAddChild.setText(LanguageManager.get("sb.addchild"));
        if (sbEdit != null)         sbEdit.setText(LanguageManager.get("sb.edit"));
        if (sbDelete != null)       sbDelete.setText(LanguageManager.get("sb.delete"));
        if (sbNavigate != null)     sbNavigate.setText(LanguageManager.get("sb.navigate"));
        if (sbCycle != null)        sbCycle.setText(LanguageManager.get("sb.cycle"));
        if (sbZoom != null)         sbZoom.setText(LanguageManager.get("sb.zoom"));
        if (sbUndo != null)         sbUndo.setText(LanguageManager.get("sb.undo"));
        if (sbRedo != null)         sbRedo.setText(LanguageManager.get("sb.redo"));
        if (syncBtn != null)        syncBtn.setText(LanguageManager.get("btn.sync"));
        if (syncStatusLabel != null) syncStatusLabel.setText(LanguageManager.get("status.pending"));

        // Remove the HUD from its parent before clearing the reference —
        // without this the old HUD stays as a ghost child in the viewport
        if (activeHud != null) {
            Pane hudParent = (Pane) activeHud.getParent();
            if (hudParent != null) hudParent.getChildren().remove(activeHud);
            activeHud = null;
        }
        if (activeHudToggle != null) {
            Pane hudParent = (Pane) activeHudToggle.getParent();
            if (hudParent != null) hudParent.getChildren().remove(activeHudToggle);
            activeHudToggle = null;
        }
        hudSelectedLabel = null;
        hudBtnLight = hudBtnDark = hudBtnSepia = hudBtnOcean = null;

        // Refresh current tab canvas to rebuild context menus and HUD
        if (tabPane != null) {
            Tab selected = tabPane.getSelectionModel().getSelectedItem();
            if (selected != null && selected.getUserData() instanceof MindMap
                    && selected.getContent() instanceof Pane) {
                Pane viewport = (Pane) selected.getContent();
                if (!viewport.getChildren().isEmpty()
                        && viewport.getChildren().get(0) instanceof Pane) {
                    refreshCanvas((Pane) viewport.getChildren().get(0),
                            (MindMap) selected.getUserData());
                }
            }
        }
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

    private void applyTheme(Dialog<?> dialog) {
        String css = App.class.getResource("styles.css").toExternalForm();
        dialog.getDialogPane().getStylesheets().add(css);
        dialog.getDialogPane().getStyleClass().add("dialog-pane");
    }

    public void loadMindMap(MindMap map) {
        currentNode = getRoot(map);
        renderMindMap(map);
        updateSyncStatusLabel(map);

        // Attach tutorial when navigated from overview (no pre-existing mainController)
        if (TutorialManager.isActive()) {
            Platform.runLater(() -> {
                if (rootPane.getScene() != null)
                    TutorialManager.attachToEditorScene(
                        (javafx.scene.layout.Pane) rootPane.getScene().getRoot());
            });
        }
    }

    public Scene getRootScene() {
        return rootPane.getScene();
    }

    public javafx.scene.Parent getRootNode() {
        // Remove from tutorial wrapper if still inside one
        if (rootPane.getParent() instanceof Pane p) p.getChildren().remove(rootPane);
        return rootPane;
    }

    public void openMapAsTab(MindMap map) {
        for (Tab tab : tabPane.getTabs()) {
            if (tab.getUserData() instanceof MindMap && ((MindMap) tab.getUserData()).getId().equals(map.getId())) {
                tab.setUserData(map);
                applyTabHeader(tab, map);
                tabPane.getSelectionModel().select(tab);
                return;
            }
        }
        currentNode = getRoot(map);
        renderMindMap(map);

        // Attach tutorial overlay to editor scene for interactive steps
        if (TutorialManager.isActive()) {
            Platform.runLater(() -> {
                if (tabPane.getScene() != null)
                    TutorialManager.attachToEditorScene(
                        (javafx.scene.layout.Pane) tabPane.getScene().getRoot());
            });
        }
    }

    @FXML
    private void onBackToOverview() {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("overview-view.fxml"));
            javafx.scene.Parent root = loader.load();
            OverviewController controller = loader.getController();
            controller.setMainController(this);
            Stage stage = (Stage) tabPane.getScene().getWindow();
            stage.getScene().setRoot(root);
            WindowsDarkMode.applyToAllWindows();
        } catch (IOException e) {
            throw new RuntimeException("Failed to open overview", e);
        }
    }

    @FXML
    public void onCreateNewMap() {
        TextInputDialog nameDialog = new TextInputDialog();
        nameDialog.setTitle(LanguageManager.get("dlg.newmap.title"));
        nameDialog.setHeaderText(LanguageManager.get("dlg.newmap.header"));
        nameDialog.setContentText(LanguageManager.get("dlg.newmap.content"));
        applyTheme(nameDialog);
        Optional<String> nameResult = nameDialog.showAndWait();
        if (nameResult.isEmpty() || nameResult.get().trim().isEmpty()) return;
        MindMap map = service.createMindMap(nameResult.get().trim());
        currentNode = getRoot(map);
        renderMindMap(map);
    }

    @FXML
    private void onDeleteCurrentMap() {
        Tab selected = tabPane.getSelectionModel().getSelectedItem();
        if (selected == null || !(selected.getUserData() instanceof MindMap)) return;
        MindMap map = (MindMap) selected.getUserData();

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle(LanguageManager.get("dlg.delete.title"));
        confirm.setHeaderText(LanguageManager.getf("dlg.delete.header", map.getName()));
        confirm.setContentText(LanguageManager.get("dlg.delete.content"));
        applyTheme(confirm);
        confirm.showAndWait()
            .filter(btn -> btn == ButtonType.OK)
            .ifPresent(btn -> {
                service.deleteMindMap(map.getId());
                tabPane.getTabs().remove(selected);
            });
    }

    private static final String[] AI_PALETTE = {
        "#4f46e5", "#0891b2", "#059669", "#d97706", "#dc2626", "#7c3aed", "#db2777", "#0284c7"
    };

    @FXML
    public void onOpenAiChat() {
        // Use the currently open map; if none, create a new one
        Tab activeTab = tabPane.getSelectionModel().getSelectedItem();
        MindMap activeMap;
        if (activeTab != null && activeTab.getUserData() instanceof MindMap) {
            activeMap = (MindMap) activeTab.getUserData();
        } else {
            TextInputDialog nameDialog = new TextInputDialog();
            nameDialog.setTitle(LanguageManager.get("dlg.newmap.title"));
            nameDialog.setHeaderText(LanguageManager.get("dlg.createfirst"));
            nameDialog.setContentText(LanguageManager.get("dlg.newmap.content"));
            applyTheme(nameDialog);
            Optional<String> r = nameDialog.showAndWait();
            if (r.isEmpty() || r.get().trim().isEmpty()) return;
            activeMap = service.createMindMap(r.get().trim());
            currentNode = getRoot(activeMap);
            renderMindMap(activeMap);
        }

        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("ai-chat-view.fxml"));
            javafx.scene.Parent chatRoot = loader.load();
            AiChatController chatCtrl = loader.getController();
            chatCtrl.loadExistingMap(activeMap);
            if (userApiKey != null) chatCtrl.setApiKey(userApiKey);

            Stage stage = (Stage) rootPane.getScene().getWindow();
            final MindMap mapRef = activeMap;

            chatCtrl.setReturnScene(rootPane.getScene(), () -> Platform.runLater(() -> {
                // Rebuild the tab so the updated map is re-rendered
                tabPane.getTabs().removeIf(t ->
                    t.getUserData() instanceof MindMap &&
                    ((MindMap) t.getUserData()).getId().equals(mapRef.getId()));
                currentNode = getRoot(mapRef);
                renderMindMap(mapRef);
            }));

            stage.getScene().setRoot(chatRoot);
            WindowsDarkMode.applyToAllWindows();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String escapeJson(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        return sb.toString();
    }

    private String buildAiPrompt(String topic) {
        return "You are a mind map expert. Create a detailed mind map about '" + topic + "'. " +
               "IMPORTANT: Respond ONLY in the following format, NO markdown, NO text before or after. " +
               "Line 1 = main topic. Then categories and subcategories with '- ' prefix:\n" +
               "Main Topic\nCategory 1\n- Subcategory 1.1\n- Subcategory 1.2\nCategory 2\n- Subcategory 2.1";
    }

    private void applyAiStyling(MindMap map, Node root, String content) {
        int paletteIdx = 0;
        Node currentMain = null;
        String currentColor = AI_PALETTE[0];
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
                    Node child = service.addNode(map, currentMain.getId(), line.substring(2).trim());
                    child.setShape("ROUNDED_RECT");
                    child.setColor(currentColor);
                    repository.updateNode(child);
                }
            } else {
                currentColor = AI_PALETTE[paletteIdx % AI_PALETTE.length];
                paletteIdx++;
                currentMain = service.addNode(map, root.getId(), line);
                currentMain.setShape("PILL");
                currentMain.setColor(currentColor);
                repository.updateNode(currentMain);
            }
        }
    }

    private void callGeminiApi(MindMap map, Node root, String prompt, String apiKey) {
        try {
            String aiPrompt = buildAiPrompt(prompt);
            String jsonPayload = "{\"contents\":[{\"parts\":[{\"text\":\"" + escapeJson(aiPrompt) + "\"}]}]}";

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();

            service.updateNodeText(map, root, prompt.substring(0, Math.min(prompt.length(), 30)));

            String content = extractTextFromJson(responseBody);
            if (content != null) {
                applyAiStyling(map, root, content);
            } else {
                generateSmarterMockAiMindMap(map, root, prompt);
            }
        } catch (Exception e) {
            e.printStackTrace();
            generateSmarterMockAiMindMap(map, root, prompt);
        }
    }

    private static final String[] CLAUDE_MODELS = {
        "claude-haiku-4-5-20251001",
        "claude-3-5-haiku-20241022",
        "claude-3-haiku-20240307"
    };

    private void callClaudeApi(MindMap map, Node root, String prompt, String apiKey) {
        try {
            String aiPrompt = buildAiPrompt(prompt);
            String escaped = escapeJson(aiPrompt);

            HttpClient client = HttpClient.newHttpClient();
            String responseBody = "";
            int statusCode = 500;

            for (String model : CLAUDE_MODELS) {
                String jsonBody = "{\"model\":\"" + model + "\",\"max_tokens\":2048," +
                                  "\"messages\":[{\"role\":\"user\",\"content\":\"" + escaped + "\"}]}";
                System.out.println("=== Claude request (model=" + model + ") ===");
                System.out.println(jsonBody);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.anthropic.com/v1/messages"))
                        .header("Content-Type", "application/json")
                        .header("x-api-key", apiKey)
                        .header("anthropic-version", "2023-06-01")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                statusCode = response.statusCode();
                responseBody = response.body();
                System.out.println("Claude model " + model + " → " + statusCode);
                System.out.println("Claude response: " + responseBody);
                if (statusCode == 200) break;
            }

            if (statusCode != 200) {
                String finalErr = "Claude API Fehler (" + statusCode + "):\n" + responseBody;
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Claude API Fehler");
                alert.setHeaderText("Anfrage fehlgeschlagen (HTTP " + statusCode + ")");
                alert.setContentText(responseBody.length() > 400 ? responseBody.substring(0, 400) + "…" : responseBody);
                applyTheme(alert);
                alert.showAndWait();
                System.err.println(finalErr);
                generateSmarterMockAiMindMap(map, root, prompt);
                return;
            }

            service.updateNodeText(map, root, prompt.substring(0, Math.min(prompt.length(), 30)));

            String content = extractTextFromJson(responseBody);
            if (content != null) {
                applyAiStyling(map, root, content);
            } else {
                System.err.println("Claude: konnte Text nicht parsen. Response: " + responseBody.substring(0, Math.min(500, responseBody.length())));
                generateSmarterMockAiMindMap(map, root, prompt);
            }
        } catch (Exception e) {
            e.printStackTrace();
            generateSmarterMockAiMindMap(map, root, prompt);
        }
    }

    private String extractTextFromJson(String json) {
        int idx = json.indexOf("\"text\": \"");
        if (idx == -1) idx = json.indexOf("\"text\":\"");
        if (idx == -1) return null;
        int start = json.indexOf("\"", idx + 7) + 1;
        int end = start;
        while (end < json.length()) {
            if (json.charAt(end) == '"' && json.charAt(end - 1) != '\\') break;
            end++;
        }
        if (end >= json.length()) return null;
        return json.substring(start, end)
                   .replace("\\n", "\n")
                   .replace("\\\"", "\"")
                   .replace("\\\\", "\\")
                   .replace("\\*", "");
    }

    private void generateSmarterMockAiMindMap(MindMap map, Node root, String prompt) {
        String p = prompt.toLowerCase();
        
        if (p.contains("aktien") || p.contains("börse") || p.contains("stock") || p.contains("invest")) {
            Node basics = service.addNode(map, root.getId(), "Basics");
            service.addNode(map, basics.getId(), "Stock Exchange & Trading");
            service.addNode(map, basics.getId(), "Broker");
            service.addNode(map, basics.getId(), "Dividends");

            Node types = service.addNode(map, root.getId(), "Asset Classes");
            service.addNode(map, types.getId(), "Individual Stocks");
            service.addNode(map, types.getId(), "ETFs");
            service.addNode(map, types.getId(), "Mutual Funds");

            Node strategy = service.addNode(map, root.getId(), "Strategy");
            service.addNode(map, strategy.getId(), "Buy & Hold");
            service.addNode(map, strategy.getId(), "Day Trading");
            service.addNode(map, strategy.getId(), "Value Investing");

            Node risks = service.addNode(map, root.getId(), "Risks");
            service.addNode(map, risks.getId(), "Price Volatility");
            service.addNode(map, risks.getId(), "Inflation");
            service.addNode(map, risks.getId(), "Total Loss");

        } else if (p.contains("java") || p.contains("programming") || p.contains("software") || p.contains("programmieren")) {
            Node concepts = service.addNode(map, root.getId(), "Concepts");
            service.addNode(map, concepts.getId(), "Object-Oriented Programming");
            service.addNode(map, concepts.getId(), "Data Structures");

            Node tools = service.addNode(map, root.getId(), "Tools");
            service.addNode(map, tools.getId(), "IDE (IntelliJ, Eclipse)");
            service.addNode(map, tools.getId(), "Git & GitHub");

            Node languages = service.addNode(map, root.getId(), "Languages");
            service.addNode(map, languages.getId(), "Java");
            service.addNode(map, languages.getId(), "Python");
            service.addNode(map, languages.getId(), "JavaScript");

        } else {
            Node info = service.addNode(map, root.getId(), "What is it?");
            service.addNode(map, info.getId(), "Definition");
            service.addNode(map, info.getId(), "Origin");

            Node proCon = service.addNode(map, root.getId(), "Pros & Cons");
            service.addNode(map, proCon.getId(), "Advantages");
            service.addNode(map, proCon.getId(), "Disadvantages");

            Node examples = service.addNode(map, root.getId(), "Examples & Usage");
            service.addNode(map, examples.getId(), "Real-world Examples");
            service.addNode(map, examples.getId(), "Use Cases");
        }
    }

    private void renderMindMap(MindMap map) {
        Pane viewport = new Pane();
        viewport.setFocusTraversable(true);
        viewport.setOnMouseClicked(e -> viewport.requestFocus());
        viewport.setStyle("-fx-background-color: #f1f5f9;");

        Pane canvas = new Pane();
        viewport.getChildren().add(canvas);

        setupZoomAndPan(viewport, canvas, map);

        // Re-layout when viewport gets its actual size on first display
        viewport.widthProperty().addListener((obs, oldW, newW) -> {
            if (newW.doubleValue() > 10 && viewport.getHeight() > 10) {
                refreshCanvas(canvas, map);
            }
        });

        Tab tab = new Tab();
        tab.setContent(viewport);
        tab.setUserData(map);
        tab.setClosable(true);
        tab.setOnCloseRequest(e -> {
            // just close the tab, the mind map stays in the database
        });
        applyTabHeader(tab, map);
        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);
        refreshCanvas(canvas, map);
        viewport.requestFocus();

        // Fit the entire map into view once the viewport has its actual size
        Platform.runLater(() -> Platform.runLater(() ->
            Platform.runLater(() -> fitMapToViewport(viewport, canvas, map))
        ));
    }

    private void applyTabHeader(Tab tab, MindMap map) {
        javafx.scene.control.Label starLbl = new javafx.scene.control.Label(map.isPinned() ? "★" : "☆");
        starLbl.setStyle((map.isPinned() ? "-fx-text-fill: #f59e0b;" : "-fx-text-fill: #64748b;")
                + " -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 0 4 0 0;");
        starLbl.setOnMouseClicked(e -> {
            map.setPinned(!map.isPinned());
            repository.updatePinned(map.getId(), map.isPinned());
            applyTabHeader(tab, map);
            e.consume();
        });
        tab.setText(map.getName());
        tab.setGraphic(starLbl);
    }

    // Key: instead of canvas.setScaleX() — which scales around getBoundsInLocal().center (a dynamic
    // value that shifts whenever a node is added/moved) — we add an explicit Scale transform with a
    // fixed pivot at canvas-local (0,0).  The transform chain then gives exactly:
    //   screenX = worldX * scale + translateX
    // with no dynamic pivot correction needed anywhere.
    private static javafx.scene.transform.Scale getOrAddScaleTransform(Pane canvas) {
        for (javafx.scene.transform.Transform t : canvas.getTransforms()) {
            if (t instanceof javafx.scene.transform.Scale s && s.getPivotX() == 0 && s.getPivotY() == 0)
                return s;
        }
        javafx.scene.transform.Scale s = new javafx.scene.transform.Scale(1.0, 1.0, 0.0, 0.0);
        canvas.getTransforms().add(0, s);
        return s;
    }

    static double getCanvasScale(Pane canvas) {
        for (javafx.scene.transform.Transform t : canvas.getTransforms()) {
            if (t instanceof javafx.scene.transform.Scale s && s.getPivotX() == 0 && s.getPivotY() == 0)
                return s.getX();
        }
        return 1.0;
    }

    private void setupZoomAndPan(Pane viewport, Pane canvas, MindMap map) {
        final double SCALE_DELTA = 1.1;

        // Install the fixed-pivot Scale transform (idempotent if called again).
        javafx.scene.transform.Scale scaleXform = getOrAddScaleTransform(canvas);

        viewport.setOnScroll(event -> {
            event.consume();
            if (event.getDeltaY() == 0) return;

            double scaleFactor = event.getDeltaY() > 0 ? SCALE_DELTA : 1.0 / SCALE_DELTA;
            double newScale = scaleXform.getX() * scaleFactor;
            if (newScale < 0.2 || newScale > 5.0) return;

            // screenX = worldX * scale + tx  (exact, pivot is fixed at 0,0).
            // Keep the point under the cursor fixed: compute its world coords, then
            // set tx so that same world point maps back to the same screen position.
            double mx = event.getX();
            double my = event.getY();
            double worldMX = (mx - canvas.getTranslateX()) / scaleXform.getX();
            double worldMY = (my - canvas.getTranslateY()) / scaleXform.getY();

            scaleXform.setX(newScale);
            scaleXform.setY(newScale);
            canvas.setTranslateX(mx - worldMX * newScale);
            canvas.setTranslateY(my - worldMY * newScale);
            refreshMinimapOnly(viewport, canvas, map);
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
                refreshMinimapOnly(viewport, canvas, map);
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

    private void refreshMinimapOnly(Pane viewport, Pane canvas, MindMap map) {
        viewport.getChildren().stream()
                .filter(n -> "minimap-box".equals(n.getId()) && n instanceof VBox)
                .findFirst()
                .ifPresent(n -> ((VBox) n).getChildren().stream()
                        .filter(c -> c instanceof Canvas)
                        .findFirst()
                        .ifPresent(c -> drawMinimapContent((Canvas) c, viewport, canvas, map)));
    }

    private void handleKeyPress(KeyEvent e, MindMap map, Pane canvas) {
        if (currentNode == null) return;

        KeyCode code = e.getCode();

        if (code == KeyCode.ENTER || code == KeyCode.INSERT) {
            promptAddChild(map, canvas, currentNode);
            e.consume();

        } else if (code == KeyCode.TAB) {
            List<Node> nodes = getNodesInDfsOrder(map);
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
                saveUndoSnapshot(map);
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
        dialog.setTitle(LanguageManager.get("dlg.addchild.title"));
        dialog.setHeaderText(LanguageManager.getf("dlg.addchild.header", parent.getText()));
        dialog.setContentText("Text:");
        applyTheme(dialog);
        dialog.showAndWait().ifPresent(text -> {
            saveUndoSnapshot(map);
            Node newNode = service.addNode(map, parent.getId(), text);
            currentNode = newNode;
            refreshCanvas(canvas, map);
            canvas.requestFocus();
            TutorialManager.onAction(TutorialManager.TutorialAction.NODE_ADDED);
        });
    }

    private void promptEditNode(MindMap map, Pane canvas, Node node) {
        TextInputDialog dialog = new TextInputDialog(node.getText());
        dialog.setTitle(LanguageManager.get("dlg.editnode.title"));
        dialog.setHeaderText(LanguageManager.get("dlg.editnode.header"));
        dialog.setContentText(LanguageManager.get("dlg.editnode.content"));
        applyTheme(dialog);
        dialog.showAndWait().ifPresent(text -> {
            saveUndoSnapshot(map);
            service.updateNodeText(map, node, text);
            refreshCanvas(canvas, map);
            canvas.requestFocus();
        });
    }

    // ── Undo / Redo ─────────────────────────────────────────────────────────

    private void saveUndoSnapshot(MindMap map) {
        undoStack.push(deepCopyNodes(map.getNodes()));
        if (undoStack.size() > MAX_UNDO_HISTORY) undoStack.pollLast();
        redoStack.clear();
    }

    private List<Node> deepCopyNodes(List<Node> nodes) {
        List<Node> copy = new ArrayList<>();
        for (Node n : nodes) {
            Node c = new Node(n.getId(), n.getText(), n.getParentId(),
                              n.getXCoordinate(), n.getYCoordinate(),
                              n.getTextSize(), n.getColor());
            c.setShape(n.getShape());
            c.setDescription(n.getDescription());
            c.setIcon(n.getIcon());
            c.setBadge(n.getBadge());
            copy.add(c);
        }
        return copy;
    }

    private void performUndo(MindMap map, Pane canvas) {
        if (undoStack.isEmpty()) return;
        redoStack.push(deepCopyNodes(map.getNodes()));
        applySnapshot(map, undoStack.pop());
        refreshCanvas(canvas, map);
    }

    private void performRedo(MindMap map, Pane canvas) {
        if (redoStack.isEmpty()) return;
        undoStack.push(deepCopyNodes(map.getNodes()));
        applySnapshot(map, redoStack.pop());
        refreshCanvas(canvas, map);
    }

    private void applySnapshot(MindMap map, List<Node> snapshot) {
        Set<String> snapshotIds = new HashSet<>();
        for (Node n : snapshot) snapshotIds.add(n.getId());
        for (Node n : map.getNodes()) {
            if (!snapshotIds.contains(n.getId())) repository.deleteNode(n.getId());
        }
        for (Node n : snapshot) repository.saveNode(map.getId(), n);
        map.getNodes().clear();
        map.getNodes().addAll(snapshot);
        if (currentNode != null) {
            String id = currentNode.getId();
            currentNode = map.getNodes().stream()
                    .filter(n -> n.getId().equals(id))
                    .findFirst()
                    .orElse(map.getNodes().isEmpty() ? null : map.getNodes().get(0));
        }
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
            if (!isPresentationModeActive) showMinimap(viewport, canvas, map);
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

    private List<Node> getNodesInDfsOrder(MindMap map) {
        List<Node> result = new ArrayList<>();
        Node root = getRoot(map);
        if (root == null) return new ArrayList<>(map.getNodes());
        dfsCollect(map, root, result);
        return result;
    }

    private void dfsCollect(MindMap map, Node node, List<Node> result) {
        result.add(node);
        List<Node> children = new ArrayList<>(getChildren(map, node));
        children.sort((a, b) -> {
            double angleA = clockwiseAngle(node, a);
            double angleB = clockwiseAngle(node, b);
            return Double.compare(angleA, angleB);
        });
        for (Node child : children) {
            dfsCollect(map, child, result);
        }
    }

    private double clockwiseAngle(Node from, Node to) {
        double dx = to.getXCoordinate() - from.getXCoordinate();
        double dy = to.getYCoordinate() - from.getYCoordinate();
        // atan2(dx, -dy): 0 = top, π/2 = right, π = bottom, normalised to [0, 2π)
        return (Math.atan2(dx, -dy) + 2 * Math.PI) % (2 * Math.PI);
    }

    private void centerOnCurrentNode(Pane viewport, Pane canvas) {
        if (currentNode == null || viewport.getWidth() <= 0) return;
        double scale = getCanvasScale(canvas);
        // Fixed-pivot Scale: screenX = worldX*scale + tx  →  tx = vpW/2 - worldX*scale
        canvas.setTranslateX(viewport.getWidth()  / 2.0 - currentNode.getXCoordinate() * scale);
        canvas.setTranslateY(viewport.getHeight() / 2.0 - currentNode.getYCoordinate() * scale);
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
        if (shapeName == null) shapeName = "ROUNDED_RECT";
        switch (shapeName) {
            case "PILL": {
                Rectangle r = new Rectangle(nodeW, nodeH);
                r.setArcWidth(nodeH); r.setArcHeight(nodeH);
                return r;
            }
            case "ELLIPSE":
                return new Ellipse(nodeW / 2, nodeH / 2);
            case "DIAMOND":
                return new Polygon(nodeW/2,0, nodeW,nodeH/2, nodeW/2,nodeH, 0,nodeH/2);
            case "HEXAGON": {
                double q = nodeH * 0.26;
                return new Polygon(
                    nodeW*0.5,0, nodeW,q, nodeW,nodeH-q,
                    nodeW*0.5,nodeH, 0,nodeH-q, 0,q);
            }
            case "STAR": {
                double[] pts = new double[20];
                double cx = nodeW/2, cy = nodeH/2;
                double outer = Math.min(nodeW, nodeH)/2 * 0.9;
                double inner = outer * 0.42;
                for (int i = 0; i < 10; i++) {
                    double a = Math.PI * i / 5 - Math.PI / 2;
                    double r = (i % 2 == 0) ? outer : inner;
                    pts[i*2] = cx + r * Math.cos(a);
                    pts[i*2+1] = cy + r * Math.sin(a);
                }
                return new Polygon(pts);
            }
            case "PARALLELOGRAM": {
                double sk = nodeH * 0.28;
                return new Polygon(sk,0, nodeW,0, nodeW-sk,nodeH, 0,nodeH);
            }
            case "OCTAGON": {
                double cut = Math.min(nodeW, nodeH) * 0.22;
                return new Polygon(
                    cut,0, nodeW-cut,0, nodeW,cut, nodeW,nodeH-cut,
                    nodeW-cut,nodeH, cut,nodeH, 0,nodeH-cut, 0,cut);
            }
            default: { // ROUNDED_RECT
                Rectangle r = new Rectangle(nodeW, nodeH);
                r.setArcWidth(NODE_ARC * 2); r.setArcHeight(NODE_ARC * 2);
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

        if (isRoot && (node.getColor() == null || node.getColor().isEmpty())) {
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

        String shapeName = node.getShape();
        double labelMaxW = "DIAMOND".equals(shapeName) ? nodeW * 0.52
                         : "ELLIPSE".equals(shapeName)  ? nodeW * 0.68
                         : nodeW - 16;

        // Icon prefix
        String icon = node.getIcon();
        String displayText = (icon != null && !icon.isEmpty()) ? icon + "  " + node.getText() : node.getText();

        Label label = new Label(displayText);
        label.setMaxWidth(labelMaxW);
        label.setWrapText(true);
        label.setTextFill(textColor);
        label.setStyle(
            "-fx-font-size: " + node.getTextSize() + "px; -fx-text-alignment: center; -fx-alignment: center;" +
            (isRoot && (node.getColor() == null || node.getColor().isEmpty()) ? " -fx-font-weight: bold;" : "")
        );

        nodeView.getChildren().addAll(rect, label);

        // Badge overlay (top-right corner)
        String badge = node.getBadge();
        if (badge != null && !badge.isEmpty()) {
            String[] bd = findBadge(badge);
            if (bd != null) {
                Label badgeLbl = new Label(bd[0]);
                badgeLbl.setStyle("-fx-font-size: 11px; -fx-padding: 1 4; -fx-background-radius: 6;"
                        + " -fx-background-color: " + bd[2] + "; -fx-text-fill: white;");
                StackPane.setAlignment(badgeLbl, Pos.TOP_RIGHT);
                StackPane.setMargin(badgeLbl, new Insets(-6, -6, 0, 0));
                nodeView.getChildren().add(badgeLbl);
            }
        }

        nodeView.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && !e.isConsumed()) {
                currentNode = node;
                refreshCanvas(canvas, map);
                if (e.getClickCount() == 2) {
                    showDescriptionPopup(node, canvas, map);
                }
                canvas.requestFocus();
            }
        });

        // Stores the scene position from the previous drag event so we can compute incremental deltas.
        // Dividing the screen delta by scaleX converts it to world units at any zoom level.
        // We avoid canvas.sceneToLocal() because moving a child changes the Pane's bounds, which
        // shifts JavaFX's scale pivot and makes sceneToLocal() return wrong values mid-drag.
        final double[] lastScene = new double[2];
        final boolean[] wasDragged = {false};

        nodeView.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                wasDragged[0] = false;
                lastScene[0] = e.getSceneX();
                lastScene[1] = e.getSceneY();
                e.consume();
            }
        });

        nodeView.setOnMouseDragged(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                if (!wasDragged[0]) {
                    wasDragged[0] = true;
                    saveUndoSnapshot(map);
                }
                double scale = getCanvasScale(canvas);
                double dx = (e.getSceneX() - lastScene[0]) / scale;
                double dy = (e.getSceneY() - lastScene[1]) / scale;
                lastScene[0] = e.getSceneX();
                lastScene[1] = e.getSceneY();
                double newX = nodeView.getLayoutX() + dx;
                double newY = nodeView.getLayoutY() + dy;
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

        MenuItem addChild = new MenuItem(LanguageManager.get("menu.addchild"));
        addChild.setOnAction(e -> promptAddChild(map, canvas, node));

        MenuItem editText = new MenuItem(LanguageManager.get("menu.edittext"));
        editText.setOnAction(e -> promptEditNode(map, canvas, node));

        MenuItem deleteNode = new MenuItem(LanguageManager.get("menu.deletenode"));
        deleteNode.setDisable(node.getParentId() == null);
        deleteNode.setOnAction(e -> {
            if (node.getParentId() != null) {
                saveUndoSnapshot(map);
                String parentId = node.getParentId();
                service.deleteNode(map, node);
                currentNode = map.getNodes().stream()
                        .filter(n -> n.getId().equals(parentId))
                        .findFirst().orElse(null);
                refreshCanvas(canvas, map);
                canvas.requestFocus();
            }
        });

        MenuItem editDesc = new MenuItem(LanguageManager.get("menu.editdesc"));
        editDesc.setOnAction(e -> showEditDescriptionDialog(node, canvas, map));

        MenuItem styleNode = new MenuItem(LanguageManager.get("menu.editstyle"));
        styleNode.setOnAction(e -> NodeStyleEditor.show(node, repository,
                () -> {
                    refreshCanvas(canvas, map);
                    TutorialManager.onAction(TutorialManager.TutorialAction.NODE_STYLED);
                }, canvas.getScene().getWindow()));

        MenuItem duplicate = new MenuItem(LanguageManager.get("menu.duplicate"));
        duplicate.setOnAction(e -> {
            if (node.getParentId() != null) {
                saveUndoSnapshot(map);
                Node dup = service.addNode(map, node.getParentId(), node.getText());
                dup.setShape(node.getShape());
                dup.setColor(node.getColor());
                dup.setTextSize(node.getTextSize());
                dup.setIcon(node.getIcon());
                dup.setBadge(node.getBadge());
                dup.setDescription(node.getDescription());
                repository.updateNode(dup);
                currentNode = dup;
                refreshCanvas(canvas, map);
            }
        });

        contextMenu.getItems().addAll(
            addChild, editText,
            new SeparatorMenuItem(),
            styleNode,
            new SeparatorMenuItem(),
            editDesc, duplicate,
            new SeparatorMenuItem(),
            deleteNode
        );
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
        String shape = node.getShape() == null ? "ROUNDED_RECT" : node.getShape();
        switch (shape) {
            case "DIAMOND": case "ELLIPSE": case "HEXAGON": case "OCTAGON":
                minW = Math.max(minW, 110); maxW = 230; textW *= 1.25; break;
            case "STAR":
                minW = Math.max(minW, 110); maxW = 200; break;
            case "PARALLELOGRAM":
                minW = Math.max(minW, 100); maxW = 220; textW *= 1.1; break;
        }
        return Math.max(minW, Math.min(maxW, textW));
    }

    private double getNodeHeight(Node node) {
        String text = node.getText() == null ? "" : node.getText();
        double fontSize = node.getTextSize();
        double nodeW = getNodeWidth(node);
        String shape = node.getShape() == null ? "ROUNDED_RECT" : node.getShape();
        double innerW = "DIAMOND".equals(shape) ? nodeW * 0.5
                      : "ELLIPSE".equals(shape)  ? nodeW * 0.65
                      : "STAR".equals(shape)     ? nodeW * 0.45
                      : "HEXAGON".equals(shape)  ? nodeW * 0.70
                      : nodeW - 20;
        double avgCharW = fontSize * 0.57;
        double charsPerLine = Math.max(1, innerW / avgCharW);
        int lines = Math.max(1, (int) Math.ceil(text.length() / charsPerLine));
        double lineH = fontSize + 5;
        double minH = fontSize + 20;
        if ("DIAMOND".equals(shape))       minH = Math.max(minH, nodeW * 0.6);
        if ("ELLIPSE".equals(shape))        minH = Math.max(minH, nodeW * 0.5);
        if ("STAR".equals(shape))           minH = Math.max(minH, nodeW * 0.9);
        if ("HEXAGON".equals(shape))        minH = Math.max(minH, nodeW * 0.6);
        return Math.max(minH, lines * lineH + 16);
    }

    private String[] findBadge(String key) {
        for (String[] b : NodeStyleEditor.BADGES) {
            if (b[1].equals(key)) return b;
        }
        return null;
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

    // ── Export ──────────────────────────────────────────────────────────────

    @FXML
    public void onExportMindMap() {
        Tab selected = tabPane.getSelectionModel().getSelectedItem();
        if (selected == null || !(selected.getUserData() instanceof MindMap)) {
            Alert a = new Alert(Alert.AlertType.WARNING);
            a.setTitle(LanguageManager.get("dlg.export.title"));
            a.setHeaderText(null);
            a.setContentText(LanguageManager.get("dlg.export.nomap"));
            applyTheme(a);
            a.showAndWait();
            return;
        }
        MindMap map = (MindMap) selected.getUserData();
        Pane viewport = (Pane) selected.getContent();
        Pane canvas   = (Pane) viewport.getChildren().get(0);

        // Format choice
        Alert fmt = new Alert(Alert.AlertType.CONFIRMATION);
        fmt.setTitle(LanguageManager.get("dlg.export.title"));
        fmt.setHeaderText(LanguageManager.get("dlg.export.header"));
        ButtonType btnPng    = new ButtonType("PNG");
        ButtonType btnPdf    = new ButtonType("PDF");
        ButtonType btnCancel = new ButtonType("Cancel", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
        fmt.getButtonTypes().setAll(btnPng, btnPdf, btnCancel);
        applyTheme(fmt);
        Optional<ButtonType> fmtResult = fmt.showAndWait();
        if (fmtResult.isEmpty() || fmtResult.get() == btnCancel) return;
        boolean isPng = fmtResult.get() == btnPng;

        // File chooser
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle(LanguageManager.get("dlg.export.saveas"));
        String safeName = map.getName().replaceAll("[^a-zA-Z0-9_\\-]", "_");
        fc.setInitialFileName(safeName);
        if (isPng) {
            fc.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("PNG Image (*.png)", "*.png"));
        } else {
            fc.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("PDF Document (*.pdf)", "*.pdf"));
        }
        java.io.File file = fc.showSaveDialog(rootPane.getScene().getWindow());
        if (file == null) return;

        try {
            javafx.scene.image.WritableImage fxImg = snapshotFullMap(map, canvas);
            java.awt.image.BufferedImage bImg = toBufferedImage(fxImg);
            if (isPng) {
                javax.imageio.ImageIO.write(bImg, "PNG", file);
            } else {
                saveAsPdf(bImg, file);
            }
            Alert ok = new Alert(Alert.AlertType.INFORMATION);
            ok.setTitle(LanguageManager.get("dlg.export.success.title"));
            ok.setHeaderText(null);
            ok.setContentText(LanguageManager.getf("dlg.export.success.msg", file.getName()));
            applyTheme(ok);
            ok.showAndWait();
        } catch (Exception ex) {
            ex.printStackTrace();
            Alert err = new Alert(Alert.AlertType.ERROR);
            err.setTitle(LanguageManager.get("dlg.export.error.title"));
            err.setHeaderText(null);
            err.setContentText(ex.getMessage());
            applyTheme(err);
            err.showAndWait();
        }
    }

    private javafx.scene.image.WritableImage snapshotFullMap(MindMap map, Pane canvas) {
        if (map.getNodes().isEmpty()) return null;

        final double PADDING = 60;
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (Node n : map.getNodes()) {
            double hw = getNodeWidth(n)  / 2.0;
            double hh = getNodeHeight(n) / 2.0;
            minX = Math.min(minX, n.getXCoordinate() - hw);
            minY = Math.min(minY, n.getYCoordinate() - hh);
            maxX = Math.max(maxX, n.getXCoordinate() + hw);
            maxY = Math.max(maxY, n.getYCoordinate() + hh);
        }
        double imgW = maxX - minX + 2 * PADDING;
        double imgH = maxY - minY + 2 * PADDING;

        Pane viewport = (Pane) canvas.getParent();

        // Save canvas transform
        javafx.scene.transform.Scale st = getOrAddScaleTransform(canvas);
        double savedSX = st.getX(), savedSY = st.getY();
        double savedTX = canvas.getTranslateX(), savedTY = canvas.getTranslateY();

        // Scale=1 + translate so content's top-left lands at (PADDING, PADDING) in viewport coords.
        // viewport_coord = local_coord * scale + translateX  →  minX * 1 + tx = PADDING → tx = -minX + PADDING
        st.setX(1.0); st.setY(1.0);
        canvas.setTranslateX(-minX + PADDING);
        canvas.setTranslateY(-minY + PADDING);

        // Temporarily hide HUD / minimap overlay children
        List<javafx.scene.Node> hidden = new ArrayList<>();
        for (javafx.scene.Node child : new ArrayList<>(viewport.getChildren())) {
            if (child != canvas && child.isVisible()) {
                child.setVisible(false);
                hidden.add(child);
            }
        }

        String bg;
        switch (currentPresentationTheme) {
            case "DARK":  bg = "#0d1117"; break;
            case "SEPIA": bg = "#f5f0e8"; break;
            case "OCEAN": bg = "#e0f7ff"; break;
            default:      bg = "#f1f5f9"; break;
        }
        javafx.scene.SnapshotParameters params = new javafx.scene.SnapshotParameters();
        params.setFill(Color.web(bg));
        // Snapshot the viewport from (0,0): canvas is repositioned so content fills this area
        params.setViewport(new javafx.geometry.Rectangle2D(0, 0, imgW, imgH));
        javafx.scene.image.WritableImage image = viewport.snapshot(params, null);

        // Restore overlays
        for (javafx.scene.Node child : hidden) child.setVisible(true);
        // Restore canvas transform
        st.setX(savedSX); st.setY(savedSY);
        canvas.setTranslateX(savedTX); canvas.setTranslateY(savedTY);

        return image;
    }

    private java.awt.image.BufferedImage toBufferedImage(javafx.scene.image.WritableImage fxImg) {
        int w = (int) fxImg.getWidth();
        int h = (int) fxImg.getHeight();
        int[] pixels = new int[w * h];
        fxImg.getPixelReader().getPixels(
            0, 0, w, h,
            javafx.scene.image.PixelFormat.getIntArgbInstance(),
            pixels, 0, w);
        java.awt.image.BufferedImage img =
            new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        img.setRGB(0, 0, w, h, pixels, 0, w);
        return img;
    }

    private void saveAsPdf(java.awt.image.BufferedImage bImg, java.io.File file) throws Exception {
        try (org.apache.pdfbox.pdmodel.PDDocument doc = new org.apache.pdfbox.pdmodel.PDDocument()) {
            org.apache.pdfbox.pdmodel.common.PDRectangle rect =
                new org.apache.pdfbox.pdmodel.common.PDRectangle(bImg.getWidth(), bImg.getHeight());
            org.apache.pdfbox.pdmodel.PDPage page = new org.apache.pdfbox.pdmodel.PDPage(rect);
            doc.addPage(page);
            org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject pdImg =
                org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory.createFromImage(doc, bImg);
            try (org.apache.pdfbox.pdmodel.PDPageContentStream cs =
                    new org.apache.pdfbox.pdmodel.PDPageContentStream(doc, page)) {
                cs.drawImage(pdImg, 0, 0, bImg.getWidth(), bImg.getHeight());
            }
            doc.save(file);
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
        hudExpanded = false;
        tabPane.getStyleClass().add("presentation-mode");

        // Attach ESC/fullscreen-exit listener before going fullscreen
        stage.fullScreenProperty().addListener(new javafx.beans.value.ChangeListener<Boolean>() {
            @Override
            public void changed(javafx.beans.value.ObservableValue<? extends Boolean> obs, Boolean wasFS, Boolean isFS) {
                if (!isFS && isPresentationModeActive) {
                    exitPresentationMode(stage);
                    stage.fullScreenProperty().removeListener(this);
                }
            }
        });

        // Fade out toolbar / sidebar / statusbar, then go fullscreen once invisible
        FadeTransition ft1 = new FadeTransition(Duration.millis(180), toolbar);
        ft1.setToValue(0);
        FadeTransition ft2 = new FadeTransition(Duration.millis(180), sidebar);
        ft2.setToValue(0);
        FadeTransition ft3 = new FadeTransition(Duration.millis(180), statusbar);
        ft3.setToValue(0);
        ParallelTransition fadeOut = new ParallelTransition(ft1, ft2, ft3);
        fadeOut.setOnFinished(ev -> {
            toolbar.setVisible(false);   toolbar.setManaged(false);   toolbar.setOpacity(1);
            sidebar.setVisible(false);   sidebar.setManaged(false);   sidebar.setOpacity(1);
            statusbar.setVisible(false); statusbar.setManaged(false); statusbar.setOpacity(1);
            stage.setFullScreenExitHint(LanguageManager.get("hud.exitHint"));
            stage.setFullScreen(true);

            Tab selected = tabPane.getSelectionModel().getSelectedItem();
            if (selected != null && selected.getUserData() instanceof MindMap) {
                MindMap map = (MindMap) selected.getUserData();
                Pane viewport = (Pane) selected.getContent();
                Pane canvas  = (Pane) viewport.getChildren().get(0);
                refreshCanvas(canvas, map);
                // Wait two pulses for fullscreen layout to settle, then re-apply theme and animate
                javafx.application.Platform.runLater(() ->
                    javafx.application.Platform.runLater(() -> {
                        String themeColor;
                        switch (map.getTheme()) {
                            case "DARK":  themeColor = "#0d1117"; break;
                            case "SEPIA": themeColor = "#f5f0e8"; break;
                            case "OCEAN": themeColor = "#e0f7ff"; break;
                            default:      themeColor = "#f1f5f9"; break;
                        }
                        viewport.setStyle("-fx-background-color: " + themeColor + ";");
                        animateCenterCanvasInViewport(viewport, canvas, map, 420);
                    }));
            }
        });
        fadeOut.play();
    }


    private void fitMapToViewport(Pane viewport, Pane canvas, MindMap map) {
        if (map.getNodes().isEmpty() || viewport.getWidth() <= 0 || viewport.getHeight() <= 0) return;

        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (Node n : map.getNodes()) {
            double hw = getNodeWidth(n)  / 2.0;
            double hh = getNodeHeight(n) / 2.0;
            minX = Math.min(minX, n.getXCoordinate() - hw);
            minY = Math.min(minY, n.getYCoordinate() - hh);
            maxX = Math.max(maxX, n.getXCoordinate() + hw);
            maxY = Math.max(maxY, n.getYCoordinate() + hh);
        }

        double contentW = maxX - minX;
        double contentH = maxY - minY;
        if (contentW <= 0 || contentH <= 0) return;

        final double PADDING = 80;
        double scaleX = (viewport.getWidth()  - PADDING * 2) / contentW;
        double scaleY = (viewport.getHeight() - PADDING * 2) / contentH;
        double scale  = Math.max(0.15, Math.min(1.5, Math.min(scaleX, scaleY)));

        javafx.scene.transform.Scale scaleXform = getOrAddScaleTransform(canvas);
        scaleXform.setX(scale);
        scaleXform.setY(scale);

        double cx = (minX + maxX) / 2.0;
        double cy = (minY + maxY) / 2.0;
        canvas.setTranslateX(viewport.getWidth()  / 2.0 - cx * scale);
        canvas.setTranslateY(viewport.getHeight() / 2.0 - cy * scale);

        refreshMinimapOnly(viewport, canvas, map);
    }

    private void exitPresentationMode(Stage stage) {
        isPresentationModeActive = false;
        hudExpanded = false;

        // Remove HUD immediately (before layout changes)
        if (activeHud != null) {
            Pane parent = (Pane) activeHud.getParent();
            if (parent != null) parent.getChildren().remove(activeHud);
            activeHud = null;
            hudSelectedLabel = null;
            hudBtnLight = hudBtnDark = hudBtnSepia = hudBtnOcean = null;
        }
        if (activeHudToggle != null) {
            Pane parent = (Pane) activeHudToggle.getParent();
            if (parent != null) parent.getChildren().remove(activeHudToggle);
            activeHudToggle = null;
        }

        tabPane.getStyleClass().remove("presentation-mode");

        // Restore toolbar/sidebar/statusbar invisibly so layout can already compute correct sizes
        toolbar.setOpacity(0);   toolbar.setVisible(true);   toolbar.setManaged(true);
        sidebar.setOpacity(0);   sidebar.setVisible(true);   sidebar.setManaged(true);
        statusbar.setOpacity(0); statusbar.setVisible(true); statusbar.setManaged(true);

        if (stage.isFullScreen()) stage.setFullScreen(false);

        Tab selected = tabPane.getSelectionModel().getSelectedItem();
        if (selected == null || !(selected.getUserData() instanceof MindMap)) return;
        MindMap map = (MindMap) selected.getUserData();
        Pane viewport = (Pane) selected.getContent();
        Pane canvas   = (Pane) viewport.getChildren().get(0);
        refreshCanvas(canvas, map);

        // Once the viewport resizes (fullscreen exit is async), fade UI in and animate canvas
        Runnable finish = () -> {
            FadeTransition ft1 = new FadeTransition(Duration.millis(220), toolbar);   ft1.setToValue(1);
            FadeTransition ft2 = new FadeTransition(Duration.millis(220), sidebar);   ft2.setToValue(1);
            FadeTransition ft3 = new FadeTransition(Duration.millis(220), statusbar); ft3.setToValue(1);
            new ParallelTransition(ft1, ft2, ft3).play();
            animateCenterCanvasInViewport(viewport, canvas, map, 380);
        };

        final boolean[] done = {false};
        final java.util.concurrent.atomic.AtomicReference<javafx.beans.value.ChangeListener<Number>> ref =
                new java.util.concurrent.atomic.AtomicReference<>();
        javafx.beans.value.ChangeListener<Number> listener = (obs, oldW, newW) -> {
            if (!done[0]) {
                done[0] = true;
                viewport.widthProperty().removeListener(ref.get());
                javafx.application.Platform.runLater(finish);
            }
        };
        ref.set(listener);
        viewport.widthProperty().addListener(listener);

        // Fallback: if viewport width never fires (already correct size), run after layout passes
        javafx.application.Platform.runLater(() ->
            javafx.application.Platform.runLater(() ->
                javafx.application.Platform.runLater(() -> {
                    viewport.widthProperty().removeListener(ref.get());
                    if (!done[0]) { done[0] = true; finish.run(); }
                })
            )
        );
    }

    private void animateCenterCanvasInViewport(Pane viewport, Pane canvas, MindMap map, int ms) {
        if (map.getNodes().isEmpty() || viewport.getWidth() <= 0 || viewport.getHeight() <= 0) return;
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (Node n : map.getNodes()) {
            minX = Math.min(minX, n.getXCoordinate());
            minY = Math.min(minY, n.getYCoordinate());
            maxX = Math.max(maxX, n.getXCoordinate());
            maxY = Math.max(maxY, n.getYCoordinate());
        }
        double scale = getCanvasScale(canvas);
        double targetX = viewport.getWidth()  / 2.0 - (minX + maxX) / 2.0 * scale;
        double targetY = viewport.getHeight() / 2.0 - (minY + maxY) / 2.0 * scale;
        TranslateTransition tt = new TranslateTransition(Duration.millis(ms), canvas);
        tt.setToX(targetX);
        tt.setToY(targetY);
        tt.setInterpolator(Interpolator.EASE_BOTH);
        tt.play();
    }

    private void showPresentationHud(Pane viewport, Pane canvas, MindMap map) {
        // Fast-path: if HUD already lives in this viewport and mode hasn't changed,
        // just update the mutable parts in-place (no remove/re-add = no flash).
        // In presentation mode there is no node section, so ignore node-presence changes.
        boolean hasNode = !isPresentationModeActive && (currentNode != null);
        boolean hadNode = !hudPresentationMode  && (hudSelectedLabel != null);
        if (activeHud != null && activeHud.getParent() == viewport
                && hudPresentationMode == isPresentationModeActive
                && hasNode == hadNode) {
            if (hudSelectedLabel != null && currentNode != null)
                hudSelectedLabel.setText(currentNode.getText());
            updateHudThemeButtons();
            return;
        }

        // ── Full rebuild ──────────────────────────────────────────────────────
        if (activeHud != null) {
            Pane p = (Pane) activeHud.getParent();
            if (p != null) p.getChildren().remove(activeHud);
            activeHud = null;
        }
        if (activeHudToggle != null) {
            Pane p = (Pane) activeHudToggle.getParent();
            if (p != null) p.getChildren().remove(activeHudToggle);
            activeHudToggle = null;
        }
        hudSelectedLabel = null;
        hudBtnLight = hudBtnDark = hudBtnSepia = hudBtnOcean = null;
        hudPresentationMode = isPresentationModeActive;

        VBox hud = new VBox();
        hud.getStyleClass().add("presentation-hud");

        Label title = new Label(isPresentationModeActive
                ? LanguageManager.get("hud.presentationCtrl")
                : LanguageManager.get("hud.nodeStyling"));
        title.getStyleClass().add("hud-title");
        hud.getChildren().add(title);

        if (!isPresentationModeActive) {
            if (currentNode != null) {
                VBox nodeSection = new VBox(10);
                nodeSection.getStyleClass().add("hud-section");

                Label selectedLabel = new Label(currentNode.getText());
                selectedLabel.getStyleClass().add("hud-label");
                selectedLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #f1f5f9; -fx-font-size: 13px;");
                selectedLabel.setMaxWidth(200);
                selectedLabel.setWrapText(true);
                hudSelectedLabel = selectedLabel;

                HBox colorStrip = new HBox(4);
                colorStrip.setAlignment(Pos.CENTER_LEFT);
                for (String hex : NodeStyleEditor.PRESETS) {
                    javafx.scene.shape.Rectangle sw = new javafx.scene.shape.Rectangle(14, 14);
                    sw.setArcWidth(4); sw.setArcHeight(4);
                    sw.setFill(javafx.scene.paint.Color.web(hex));
                    sw.setCursor(javafx.scene.Cursor.HAND);
                    sw.setOnMouseClicked(e -> {
                        if (currentNode != null) {
                            saveUndoSnapshot(map);
                            currentNode.setColor(hex);
                            repository.updateNode(currentNode);
                            refreshCanvas(canvas, map);
                        }
                    });
                    colorStrip.getChildren().add(sw);
                }

                Button openEditor = new Button(LanguageManager.get("hud.fullEditor"));
                openEditor.setStyle("-fx-background-color: #6366f1; -fx-text-fill: white; "
                        + "-fx-font-size: 12px; -fx-padding: 7 14; -fx-background-radius: 8; "
                        + "-fx-cursor: hand; -fx-border-width: 0;");
                openEditor.setOnAction(e -> {
                    if (currentNode != null)
                        NodeStyleEditor.show(currentNode, repository,
                                () -> refreshCanvas(canvas, map),
                                canvas.getScene().getWindow());
                });

                nodeSection.getChildren().addAll(selectedLabel, colorStrip, openEditor);
                hud.getChildren().add(nodeSection);
            } else {
                Label noSelectLabel = new Label(LanguageManager.get("hud.noselection"));
                noSelectLabel.getStyleClass().add("hud-label");
                noSelectLabel.setStyle("-fx-font-style: italic; -fx-text-fill: #a5b1c2;");
                hud.getChildren().add(noSelectLabel);
            }
        }

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: rgba(255,255,255,0.1);");
        hud.getChildren().add(sep);

        VBox themeSection = new VBox();
        themeSection.getStyleClass().add("hud-section");
        Label themeLabel = new Label(LanguageManager.get("hud.canvastheme"));
        themeLabel.getStyleClass().add("hud-label");
        themeSection.getChildren().add(themeLabel);

        HBox themeBtnBox = new HBox();
        themeBtnBox.setSpacing(6);
        hudBtnLight = new Button(LanguageManager.get("theme.light"));
        hudBtnLight.getStyleClass().add("btn-hud");
        hudBtnLight.setOnAction(e -> { map.setTheme("LIGHT"); repository.updateTheme(map.getId(), "LIGHT"); refreshCanvas(canvas, map); });
        hudBtnDark = new Button(LanguageManager.get("theme.dark"));
        hudBtnDark.getStyleClass().add("btn-hud");
        hudBtnDark.setOnAction(e -> { map.setTheme("DARK"); repository.updateTheme(map.getId(), "DARK"); refreshCanvas(canvas, map); });
        hudBtnSepia = new Button(LanguageManager.get("theme.sepia"));
        hudBtnSepia.getStyleClass().add("btn-hud");
        hudBtnSepia.setOnAction(e -> { map.setTheme("SEPIA"); repository.updateTheme(map.getId(), "SEPIA"); refreshCanvas(canvas, map); });
        hudBtnOcean = new Button(LanguageManager.get("theme.ocean"));
        hudBtnOcean.getStyleClass().add("btn-hud");
        hudBtnOcean.setOnAction(e -> { map.setTheme("OCEAN"); repository.updateTheme(map.getId(), "OCEAN"); refreshCanvas(canvas, map); });
        updateHudThemeButtons();

        themeBtnBox.getChildren().addAll(hudBtnLight, hudBtnDark, hudBtnSepia, hudBtnOcean);
        themeSection.getChildren().add(themeBtnBox);
        hud.getChildren().add(themeSection);

        if (isPresentationModeActive) {
            Button btnExit = new Button(LanguageManager.get("btn.exitpresentation"));
            btnExit.getStyleClass().add("btn-hud-danger");
            btnExit.setMaxWidth(Double.MAX_VALUE);
            btnExit.setOnAction(e -> exitPresentationMode((Stage) viewport.getScene().getWindow()));
            hud.getChildren().add(btnExit);
        }

        // ── Toggle tab ────────────────────────────────────────────────────────
        Button toggleTab = new Button(hudExpanded ? "›" : "‹");
        String tabStyle = "-fx-background-color: rgba(14,20,35,0.92);"
                + "-fx-text-fill: #94a3b8; -fx-font-size: 18px; -fx-font-weight: bold;"
                + "-fx-min-width: " + HUD_TAB_W + "; -fx-pref-width: " + HUD_TAB_W + ";"
                + "-fx-min-height: 56; -fx-pref-height: 56;"
                + "-fx-background-radius: 10 0 0 10;"
                + "-fx-border-color: rgba(255,255,255,0.09); -fx-border-radius: 10 0 0 10;"
                + "-fx-border-width: 1 0 1 1; -fx-cursor: hand; -fx-padding: 0;";
        toggleTab.setStyle(tabStyle);
        toggleTab.setOnMouseEntered(e ->
            toggleTab.setStyle(tabStyle.replace("rgba(14,20,35,0.92)", "rgba(30,36,51,0.98)")));
        toggleTab.setOnMouseExited(e -> toggleTab.setStyle(tabStyle));

        toggleTab.setOnAction(e -> {
            if (hudExpanded) {
                // Collapse: animate off-screen, then bind so it tracks any width changes.
                hud.translateXProperty().unbind();
                TranslateTransition tt = new TranslateTransition(Duration.millis(180), hud);
                tt.setFromX(hud.getTranslateX());
                tt.setToX(hud.getWidth() + HUD_TAB_W + 16);
                tt.setInterpolator(Interpolator.EASE_OUT);
                tt.setOnFinished(ev ->
                    hud.translateXProperty().bind(hud.widthProperty().add(HUD_TAB_W + 16)));
                tt.play();
                toggleTab.setText("‹");
                hudExpanded = false;
            } else {
                // Expand: unbind the off-screen binding before animating to 0.
                hud.translateXProperty().unbind();
                TranslateTransition tt = new TranslateTransition(Duration.millis(180), hud);
                tt.setFromX(hud.getTranslateX());
                tt.setToX(0);
                tt.setInterpolator(Interpolator.EASE_OUT);
                tt.play();
                toggleTab.setText("›");
                hudExpanded = true;
            }
        });

        // ── Layout ────────────────────────────────────────────────────────────
        hud.layoutXProperty().bind(
            viewport.widthProperty().subtract(hud.widthProperty()).subtract(HUD_TAB_W + 8));
        hud.setLayoutY(20);
        toggleTab.layoutXProperty().bind(viewport.widthProperty().subtract(HUD_TAB_W + 4));
        toggleTab.setLayoutY(28);

        // Collapsed state: bind translateX = hudWidth + HUD_TAB_W + 16 so it stays
        // exactly off-screen even when the viewport or HUD width changes (fullscreen etc.).
        if (!hudExpanded) {
            hud.translateXProperty().bind(hud.widthProperty().add(HUD_TAB_W + 16));
        }

        viewport.getChildren().addAll(hud, toggleTab);
        activeHud = hud;
        activeHudToggle = toggleTab;
    }

    private void updateHudThemeButtons() {
        if (hudBtnLight == null) return;
        String active = "-fx-background-color: rgba(255,255,255,0.35);";
        hudBtnLight.setStyle(currentPresentationTheme.equals("LIGHT")  ? active : "");
        hudBtnDark.setStyle(currentPresentationTheme.equals("DARK")   ? active : "");
        hudBtnSepia.setStyle(currentPresentationTheme.equals("SEPIA") ? active : "");
        hudBtnOcean.setStyle(currentPresentationTheme.equals("OCEAN") ? active : "");
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
            suppressTreeSelection = false;
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
            String key = "sync." + (status != null ? status.toLowerCase() : "pending");
            syncStatusLabel.setText(LanguageManager.get(key));
        }
    }

    @FXML
    private void onSync() {
        Tab selected = tabPane.getSelectionModel().getSelectedItem();
        if (selected == null || !(selected.getUserData() instanceof MindMap)) return;
        MindMap map = (MindMap) selected.getUserData();

        syncStatusLabel.setText(LanguageManager.get("sync.syncing"));
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
                syncStatusLabel.setText(LanguageManager.get("sync.synced"));
                syncBtn.setDisable(false);
            }

            @Override
            protected void failed() {
                syncStatusLabel.setText(LanguageManager.get("sync.failed"));
                syncBtn.setDisable(false);
            }
        };
        new Thread(task).start();
    }

    // ── Description popup (double-click) ─────────────────────────────────────

    private void showDescriptionPopup(Node node, Pane canvas, MindMap map) {
        Stage popup = new Stage();
        popup.initStyle(StageStyle.TRANSPARENT);
        popup.initOwner(rootPane.getScene().getWindow());

        // Outer: transparent, provides space for dropshadow
        StackPane outerRoot = new StackPane();
        outerRoot.setStyle("-fx-background-color: transparent;");
        outerRoot.setPadding(new Insets(16));

        VBox card = new VBox(0);
        card.getStyleClass().add("desc-popup-card");
        card.setPrefWidth(460);
        card.setMaxWidth(460);

        // ── Header ──
        HBox header = new HBox(10);
        header.getStyleClass().add("desc-popup-header");
        header.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label(node.getDescription().isEmpty() ? "🗒" : "📝");
        icon.setStyle("-fx-font-size: 15px;");

        Label titleLabel = new Label(node.getText());
        titleLabel.getStyleClass().add("desc-popup-title");
        HBox.setHgrow(titleLabel, Priority.ALWAYS);
        titleLabel.setMaxWidth(Double.MAX_VALUE);

        Button closeBtn = new Button("✕");
        closeBtn.getStyleClass().add("desc-close-btn");
        closeBtn.setOnAction(e -> popup.close());

        header.getChildren().addAll(icon, titleLabel, closeBtn);

        // ── Body ──
        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("desc-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        String desc = node.getDescription();
        if (desc == null || desc.trim().isEmpty()) {
            Label empty = new Label(LanguageManager.get("desc.empty"));
            empty.getStyleClass().add("desc-empty-label");
            empty.setWrapText(true);
            empty.setPadding(new Insets(12, 14, 12, 14));
            scroll.setContent(empty);
        } else {
            Label body = new Label(desc);
            body.getStyleClass().add("desc-body-label");
            body.setWrapText(true);
            body.setPadding(new Insets(12, 14, 12, 14));
            scroll.setContent(body);
        }

        // ── Footer ──
        HBox footer = new HBox(8);
        footer.getStyleClass().add("desc-popup-footer");
        footer.setAlignment(Pos.CENTER_RIGHT);

        Button editBtn = new Button(LanguageManager.get("desc.edit"));
        editBtn.getStyleClass().add("btn-primary");
        editBtn.setStyle("-fx-font-size: 12px; -fx-padding: 6 14;");
        editBtn.setOnAction(e -> {
            popup.close();
            showEditDescriptionDialog(node, canvas, map);
        });

        Button closeFooterBtn = new Button(LanguageManager.get("desc.close"));
        closeFooterBtn.getStyleClass().add("btn-ghost");
        closeFooterBtn.setStyle("-fx-font-size: 12px; -fx-padding: 6 14;");
        closeFooterBtn.setOnAction(e -> popup.close());

        footer.getChildren().addAll(editBtn, closeFooterBtn);

        card.getChildren().addAll(header, scroll, footer);
        outerRoot.getChildren().add(card);

        Scene scene = new Scene(outerRoot);
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().add(App.class.getResource("styles.css").toExternalForm());
        popup.setScene(scene);
        popup.show();

        // Draggable via header
        final double[] dragOffset = {0, 0};
        header.setCursor(javafx.scene.Cursor.MOVE);
        header.setOnMousePressed(e -> {
            dragOffset[0] = e.getScreenX() - popup.getX();
            dragOffset[1] = e.getScreenY() - popup.getY();
        });
        header.setOnMouseDragged(e -> {
            popup.setX(e.getScreenX() - dragOffset[0]);
            popup.setY(e.getScreenY() - dragOffset[1]);
        });

        // Center on owner
        javafx.application.Platform.runLater(() -> {
            Stage owner = (Stage) rootPane.getScene().getWindow();
            popup.setX(owner.getX() + (owner.getWidth()  - popup.getWidth())  / 2);
            popup.setY(owner.getY() + (owner.getHeight() - popup.getHeight()) / 2);
        });
    }

    // ── Minimap ──────────────────────────────────────────────────────────────

    private void showMinimap(Pane viewport, Pane canvas, MindMap map) {
        // If box already exists just refresh canvas content — no rebuild, no re-animation
        VBox existing = (VBox) viewport.getChildren().stream()
                .filter(n -> "minimap-box".equals(n.getId()) && n instanceof VBox)
                .findFirst().orElse(null);
        if (existing != null) {
            existing.getChildren().stream()
                    .filter(c -> c instanceof Canvas)
                    .findFirst()
                    .ifPresent(c -> drawMinimapContent((Canvas) c, viewport, canvas, map));
            return;
        }

        // ── Build box ────────────────────────────────────────────────────────
        Label titleLbl = new Label("🗺  Minimap");
        titleLbl.setStyle("-fx-text-fill: #475569; -fx-font-size: 11px; -fx-font-weight: bold;");
        HBox.setHgrow(titleLbl, Priority.ALWAYS);

        Button toggleBtn = new Button("−");
        toggleBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #1e293b; "
                + "-fx-font-size: 14px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 0 4; -fx-border-width: 0;");

        HBox header = new HBox(4, titleLbl, toggleBtn);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(5, 8, 5, 10));
        header.setStyle("-fx-background-color: #e2e8f0; "
                + "-fx-border-color: #cbd5e1; -fx-border-width: 0 0 1 0; -fx-cursor: hand;");

        VBox box = new VBox(0, header);
        box.setId("minimap-box");
        box.setStyle("-fx-background-color: #f8fafc; "
                + "-fx-border-color: #cbd5e1; -fx-border-width: 1; "
                + "-fx-background-radius: 8; -fx-border-radius: 8;");
        box.setEffect(new javafx.scene.effect.DropShadow(10, 0, 3, Color.web("#00000033")));

        // ── Canvas ───────────────────────────────────────────────────────────
        Canvas mmCanvas = new Canvas(MM_W, MM_H);
        drawMinimapContent(mmCanvas, viewport, canvas, map);
        mmCanvas.setCursor(javafx.scene.Cursor.CROSSHAIR);
        mmCanvas.setOnMouseClicked(e ->
                navigateFromMinimap(e.getX(), e.getY(), viewport, canvas, map));

        // Clip used for fold animation
        Rectangle mmClip = new Rectangle(MM_W, MM_H);
        mmCanvas.setClip(mmClip);
        box.getChildren().add(mmCanvas);

        // ── Toggle: fold / unfold with clip animation ─────────────────────
        toggleBtn.setOnAction(e -> {
            if (box.getChildren().contains(mmCanvas)) {
                // Collapse
                minimapExpanded = false;
                toggleBtn.setText("+");
                Timeline collapse = new Timeline(
                    new KeyFrame(Duration.ZERO,        new KeyValue(mmClip.heightProperty(), MM_H)),
                    new KeyFrame(Duration.millis(200), new KeyValue(mmClip.heightProperty(), 0, Interpolator.EASE_OUT))
                );
                collapse.setOnFinished(ev -> box.getChildren().remove(mmCanvas));
                collapse.play();
            } else {
                // Expand
                minimapExpanded = true;
                toggleBtn.setText("−");
                drawMinimapContent(mmCanvas, viewport, canvas, map);
                mmClip.setHeight(0);
                box.getChildren().add(mmCanvas);
                Timeline expand = new Timeline(
                    new KeyFrame(Duration.ZERO,        new KeyValue(mmClip.heightProperty(), 0)),
                    new KeyFrame(Duration.millis(220), new KeyValue(mmClip.heightProperty(), MM_H, Interpolator.EASE_OUT))
                );
                expand.play();
            }
        });

        // Clicking anywhere on header opens the minimap when collapsed
        header.setOnMouseClicked(e -> {
            if (!minimapExpanded) toggleBtn.fire();
        });

        // ── Position ─────────────────────────────────────────────────────────
        box.setLayoutX(20);
        box.layoutYProperty().bind(
                viewport.heightProperty().subtract(box.heightProperty()).subtract(20));

        // ── Slide-in from bottom on first appearance ─────────────────────────
        box.setTranslateY(MM_H + 40);
        box.setOpacity(0);
        viewport.getChildren().add(box);

        Timeline slideIn = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(box.translateYProperty(), MM_H + 40),
                new KeyValue(box.opacityProperty(), 0)),
            new KeyFrame(Duration.millis(300),
                new KeyValue(box.translateYProperty(), 0, Interpolator.EASE_OUT),
                new KeyValue(box.opacityProperty(), 1, Interpolator.EASE_OUT))
        );
        slideIn.play();
    }

    private double[] computeMiniTransform(MindMap map, Pane viewport, Pane canvas) {
        List<Node> nodes = map.getNodes();
        if (nodes.isEmpty()) return null;
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (Node n : nodes) {
            minX = Math.min(minX, n.getXCoordinate());
            minY = Math.min(minY, n.getYCoordinate());
            maxX = Math.max(maxX, n.getXCoordinate());
            maxY = Math.max(maxY, n.getYCoordinate());
        }
        // Expand bounding box to include current viewport so the red dot is always in frame
        if (viewport != null && canvas != null && viewport.getWidth() > 0) {
            try {
                javafx.geometry.Point2D tl = canvas.parentToLocal(0, 0);
                javafx.geometry.Point2D br = canvas.parentToLocal(viewport.getWidth(), viewport.getHeight());
                minX = Math.min(minX, tl.getX()); minY = Math.min(minY, tl.getY());
                maxX = Math.max(maxX, br.getX()); maxY = Math.max(maxY, br.getY());
            } catch (Exception ignored) {}
        }
        double pad = 16;
        double rangeX = Math.max(1, maxX - minX);
        double rangeY = Math.max(1, maxY - minY);
        double scale = Math.min((MM_W - pad * 2) / rangeX, (MM_H - pad * 2) / rangeY);
        double ox = (MM_W - rangeX * scale) / 2.0 - minX * scale;
        double oy = (MM_H - rangeY * scale) / 2.0 - minY * scale;
        return new double[]{scale, ox, oy};
    }

    private void drawMinimapContent(Canvas mmCanvas, Pane viewport, Pane canvas, MindMap map) {
        GraphicsContext gc = mmCanvas.getGraphicsContext2D();
        double mw = mmCanvas.getWidth(), mh = mmCanvas.getHeight();

        // Light background
        gc.setFill(Color.web("#f8fafc"));
        gc.fillRect(0, 0, mw, mh);

        double[] t = computeMiniTransform(map, viewport, canvas);
        if (t == null) return;
        double scale = t[0], ox = t[1], oy = t[2];

        // Connections
        gc.setStroke(Color.web("#cbd5e1"));
        gc.setLineWidth(1.0);
        for (Node n : map.getNodes()) {
            if (n.getParentId() == null) continue;
            Node parent = map.getNodes().stream()
                    .filter(p -> p.getId().equals(n.getParentId())).findFirst().orElse(null);
            if (parent != null) {
                gc.strokeLine(
                        parent.getXCoordinate() * scale + ox, parent.getYCoordinate() * scale + oy,
                        n.getXCoordinate() * scale + ox,      n.getYCoordinate() * scale + oy);
            }
        }

        // Nodes — same color logic as createNodeView, selected gets indigo border
        boolean isDark = "DARK".equals(currentPresentationTheme);
        for (Node n : map.getNodes()) {
            boolean isRoot    = n.getParentId() == null;
            boolean isCurrent = n == currentNode;
            double nx = n.getXCoordinate() * scale + ox;
            double ny = n.getYCoordinate() * scale + oy;
            double nw = isRoot ? 12 : 8, nh = isRoot ? 7 : 5;

            Color fillColor, strokeColor;
            double strokeW;
            if (isRoot && (n.getColor() == null || n.getColor().equals("#ffffff"))) {
                fillColor   = Color.web("#6366f1");
                strokeColor = isCurrent ? Color.web("#a5b4fc") : Color.web("#4338ca");
                strokeW     = isCurrent ? 2.0 : 1.0;
            } else {
                String col = n.getColor();
                if (col == null || col.isEmpty()) col = isDark ? "#1e2433" : "#ffffff";
                if (isDark && (col.equals("#ffffff") || col.equals("#1e2433"))) col = "#1e2433";
                // white is invisible on light minimap background — use a visible neutral
                if ("#ffffff".equalsIgnoreCase(col)) col = "#e2e8f0";
                try { fillColor = Color.web(col); } catch (Exception ex) { fillColor = Color.web("#e2e8f0"); }
                strokeColor = isCurrent ? Color.web("#6366f1") : Color.web("#cbd5e1");
                strokeW     = isCurrent ? 2.0 : 0.5;
            }
            gc.setFill(fillColor);
            gc.fillRoundRect(nx - nw / 2.0, ny - nh / 2.0, nw, nh, 3, 3);
            gc.setStroke(strokeColor);
            gc.setLineWidth(strokeW);
            gc.strokeRoundRect(nx - nw / 2.0, ny - nh / 2.0, nw, nh, 3, 3);
        }

        // "You are here" red dot — viewport center mapped via parentToLocal (handles scale pivot correctly)
        // The bounding box already includes the viewport, so the dot is always within minimap bounds.
        if (viewport.getWidth() > 0) {
            try {
                javafx.geometry.Point2D worldCenter = canvas.parentToLocal(
                        viewport.getWidth() / 2.0, viewport.getHeight() / 2.0);
                double dotX = worldCenter.getX() * scale + ox;
                double dotY = worldCenter.getY() * scale + oy;
                gc.setFill(Color.web("#ef4444"));
                gc.fillOval(dotX - 5, dotY - 5, 10, 10);
                gc.setStroke(Color.WHITE);
                gc.setLineWidth(1.5);
                gc.strokeOval(dotX - 5, dotY - 5, 10, 10);
            } catch (Exception ignored) {}
        }
    }

    private void navigateFromMinimap(double mmX, double mmY,
                                      Pane viewport, Pane canvas, MindMap map) {
        double[] t = computeMiniTransform(map, viewport, canvas);
        if (t == null) return;
        // Convert minimap click → world coordinates
        double worldX = (mmX - t[1]) / t[0];
        double worldY = (mmY - t[2]) / t[0];
        // Find where (worldX, worldY) currently appears on screen, then shift to viewport center.
        // Using localToParent avoids the scale-pivot assumption that would make a formula wrong.
        try {
            javafx.geometry.Point2D screen = canvas.localToParent(worldX, worldY);
            double dx = viewport.getWidth()  / 2.0 - screen.getX();
            double dy = viewport.getHeight() / 2.0 - screen.getY();
            canvas.setTranslateX(canvas.getTranslateX() + dx);
            canvas.setTranslateY(canvas.getTranslateY() + dy);
        } catch (Exception ignored) {}
        refreshMinimapOnly(viewport, canvas, map);
    }

    private void showEditDescriptionDialog(Node node, Pane canvas, MindMap map) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle(LanguageManager.get("dlg.desc.title"));
        dialog.setHeaderText(LanguageManager.getf("dlg.desc.header", node.getText()));
        applyTheme(dialog);
        dialog.getDialogPane().setPrefWidth(500);

        TextArea area = new TextArea(node.getDescription());
        area.setPromptText(LanguageManager.get("dlg.desc.prompt"));
        area.setPrefRowCount(10);
        area.setWrapText(true);
        area.getStyleClass().add("desc-textarea");
        VBox.setVgrow(area, Priority.ALWAYS);

        VBox content = new VBox(area);
        content.setPadding(new Insets(4, 0, 0, 0));
        dialog.getDialogPane().setContent(content);

        ButtonType saveType   = new ButtonType(LanguageManager.get("dlg.desc.save"), ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType(LanguageManager.get("dlg.desc.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, cancelType);
        dialog.setResultConverter(btn -> btn == saveType ? area.getText() : null);

        dialog.showAndWait().ifPresent(desc -> {
            node.setDescription(desc);
            repository.updateNode(node);
            refreshCanvas(canvas, map);
        });
    }
}
