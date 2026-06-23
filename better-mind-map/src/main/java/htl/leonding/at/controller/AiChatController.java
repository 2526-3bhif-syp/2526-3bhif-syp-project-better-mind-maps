package htl.leonding.at;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AiChatController {

    @FXML private VBox chatContainer;
    @FXML private ScrollPane chatScrollPane;
    @FXML private TextField promptField;
    @FXML private Label mapContextLabel;
    @FXML private Label statusLabel;
    @FXML private VBox welcomeOverlay;
    @FXML private FlowPane suggestionChips;
    @FXML private HBox suggestionRow;
    @FXML private Button backBtn;
    @FXML private Label aiTitleLabel;
    @FXML private Label aiWelcomeTitleLabel;
    @FXML private Label aiSubtitleLabel;

    private final MindMapRepository repository = new MindMapRepository();
    private final MindMapService service = new MindMapService(repository);
    private static String userApiKey = null;

    private javafx.scene.Parent returnRoot = null;
    private Runnable returnCallback = null;

    private MindMap currentMap = null;
    private Node currentRoot = null;
    private String currentMapText = "";

    private static final String[] AI_PALETTE = {
        "#4f46e5", "#0891b2", "#059669", "#d97706", "#dc2626", "#7c3aed", "#db2777", "#0284c7"
    };

    private static final String[] SUGGESTION_KEYS = {"ai.s1","ai.s2","ai.s3","ai.s4","ai.s5","ai.s6"};
    private static final String[] REPLY_KEYS      = {"ai.r1","ai.r2","ai.r3","ai.r4"};

    // ── Public API ────────────────────────────────────────────────────────────

    public void setApiKey(String key) { userApiKey = key; }

    public void setReturnScene(Scene scene, Runnable callback) {
        this.returnRoot = scene.getRoot(); // capture root before it gets swapped
        this.returnCallback = callback;
    }

    public void loadExistingMap(MindMap map) {
        this.currentMap = map;
        if (!map.getNodes().isEmpty()) {
            this.currentRoot = map.getNodes().stream()
                .filter(n -> n.getParentId() == null).findFirst()
                .orElse(map.getNodes().get(0));
            StringBuilder sb = new StringBuilder();
            buildTextTree(sb, currentRoot, 0);
            this.currentMapText = sb.toString();
        }
        Platform.runLater(() -> {
            if (mapContextLabel != null)
                mapContextLabel.setText("Mind map: " + map.getName());
        });
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        chatContainer.heightProperty().addListener((obs, o, n) -> chatScrollPane.setVvalue(1.0));
        applyLanguage();
        setupWelcomeSuggestions();
    }

    private void applyLanguage() {
        if (backBtn != null)           backBtn.setText(LanguageManager.get("ai.back"));
        if (aiTitleLabel != null)      aiTitleLabel.setText(LanguageManager.get("ai.title"));
        if (aiWelcomeTitleLabel != null) aiWelcomeTitleLabel.setText(LanguageManager.get("ai.title"));
        if (aiSubtitleLabel != null)   aiSubtitleLabel.setText(LanguageManager.get("ai.subtitle"));
        if (statusLabel != null)       statusLabel.setText(LanguageManager.get("ai.ready"));
        if (mapContextLabel != null)   mapContextLabel.setText(LanguageManager.get("ai.nomapLoaded"));
        if (promptField != null)       promptField.setPromptText(LanguageManager.get("ai.input.prompt"));
    }

    private void setupWelcomeSuggestions() {
        if (suggestionChips == null) return;
        for (String key : SUGGESTION_KEYS) {
            String text = LanguageManager.get(key);
            Button chip = makeChip(text, false);
            // strip leading emoji for the prompt text (everything after first space)
            String prompt = text.contains("  ") ? text.substring(text.indexOf("  ") + 2) : text;
            chip.setOnAction(e -> { promptField.setText(prompt); onSend(); });
            suggestionChips.getChildren().add(chip);
        }
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    @FXML
    private void onBack() {
        Stage stage = (Stage) chatContainer.getScene().getWindow();
        if (returnRoot != null) {
            stage.getScene().setRoot(returnRoot);
            WindowsDarkMode.applyToAllWindows();
            if (returnCallback != null) returnCallback.run();
        } else {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("overview-view.fxml"));
                stage.getScene().setRoot(loader.load());
                WindowsDarkMode.applyToAllWindows();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // ── Send ──────────────────────────────────────────────────────────────────

    @FXML
    private void onSend() {
        String prompt = promptField.getText().trim();
        if (prompt.isEmpty() || promptField.isDisabled()) return;
        promptField.clear();

        // !apikey: shortcut
        if (prompt.startsWith("!apikey:")) {
            String key = prompt.substring(8).trim();
            if (!key.isEmpty()) {
                userApiKey = key;
                addAiMessage(LanguageManager.getf("ai.keyset", key.startsWith("sk-ant-") ? "Claude" : "Gemini"));
            }
            return;
        }

        hideWelcome();
        addUserMessage(prompt);
        clearQuickReplies();
        promptField.setDisable(true);
        setStatus(LanguageManager.get("ai.thinking.status"), "#f59e0b");

        // Resolve API key on FX thread
        String apiKey = System.getenv("MINDMAP_AI_KEY");
        if (apiKey == null || apiKey.trim().isEmpty()) apiKey = userApiKey;
        if (apiKey == null || apiKey.trim().isEmpty()) {
            TextInputDialog keyDialog = new TextInputDialog();
            keyDialog.setTitle(LanguageManager.get("ai.apikey.title"));
            keyDialog.setHeaderText(LanguageManager.get("ai.apikey.header"));
            keyDialog.setContentText(LanguageManager.get("ai.apikey.content"));
            keyDialog.getDialogPane().setPrefWidth(440);
            Optional<String> r = keyDialog.showAndWait();
            if (r.isPresent() && !r.get().trim().isEmpty()) {
                userApiKey = r.get().trim();
                apiKey = userApiKey;
            }
        }

        if (currentMap == null) {
            currentMap = service.createMindMap("AI: " + prompt.substring(0, Math.min(prompt.length(), 20)));
            currentRoot = currentMap.getNodes().get(0);
            if (mapContextLabel != null)
                mapContextLabel.setText("Mindmap: " + currentMap.getName());
        }

        addThinkingBubble();
        final String finalKey = apiKey;

        if (finalKey == null || finalKey.trim().isEmpty()) {
            removeThinkingBubble();
            promptField.setDisable(false);
            setStatus(LanguageManager.get("ai.ready.status"), "#10b981");
            addAiMessage(LanguageManager.get("ai.nokey"));
            return;
        }

        new Thread(() -> {
            try {
                if (finalKey.startsWith("sk-ant-")) callClaudeApi(prompt, finalKey);
                else callGeminiApi(prompt, finalKey);
            } finally {
                Platform.runLater(() -> {
                    removeThinkingBubble();
                    promptField.setDisable(false);
                    setStatus(LanguageManager.get("ai.ready.status"), "#10b981");
                    showQuickReplies();
                });
            }
        }).start();
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    private void hideWelcome() {
        if (welcomeOverlay != null) {
            welcomeOverlay.setVisible(false);
            welcomeOverlay.setManaged(false);
        }
    }

    private void setStatus(String text, String color) {
        Platform.runLater(() -> {
            if (statusLabel != null) {
                statusLabel.setText(text);
                statusLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12px;");
            }
        });
    }

    private void addThinkingBubble() {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setId("thinking-bubble");
        Label avatar = new Label("✨");
        avatar.setStyle("-fx-font-size: 20px;");
        Label bubble = new Label(LanguageManager.get("ai.thinking.bubble"));
        bubble.setStyle("-fx-background-color: #1a2236; -fx-text-fill: #475569; -fx-padding: 12 18; "
                + "-fx-background-radius: 18 18 18 4; -fx-font-size: 14px; -fx-font-style: italic;");
        bubble.setWrapText(true);
        bubble.setMaxWidth(600);
        row.getChildren().addAll(avatar, bubble);
        chatContainer.getChildren().add(row);
    }

    private void removeThinkingBubble() {
        chatContainer.getChildren().removeIf(n -> "thinking-bubble".equals(n.getId()));
    }

    private void clearQuickReplies() {
        if (suggestionRow != null) suggestionRow.getChildren().clear();
    }

    private void showQuickReplies() {
        if (suggestionRow == null) return;
        suggestionRow.getChildren().clear();
        String[] replies = {LanguageManager.get("ai.r1"), LanguageManager.get("ai.r2"),
                            LanguageManager.get("ai.r3"), LanguageManager.get("ai.r4")};
        for (String text : replies) {
            Button chip = makeChip(text, true);
            chip.setOnAction(e -> { promptField.setText(text.substring(text.indexOf(' ') + 1).trim()); onSend(); });
            suggestionRow.getChildren().add(chip);
        }
    }

    private void addUserMessage(String text) {
        Label bubble = new Label(text);
        bubble.setStyle("-fx-background-color: #6366f1; -fx-text-fill: white; -fx-padding: 12 18; "
                + "-fx-background-radius: 18 18 4 18; -fx-font-size: 14px;");
        bubble.setWrapText(true);
        bubble.setMaxWidth(580);
        HBox row = new HBox(bubble);
        row.setAlignment(Pos.CENTER_RIGHT);
        chatContainer.getChildren().add(row);
    }

    private void addAiMessage(String text) {
        Platform.runLater(() -> {
            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            Label avatar = new Label("✨");
            avatar.setStyle("-fx-font-size: 20px;");
            avatar.setMinWidth(32);
            Label bubble = new Label(text);
            bubble.setStyle("-fx-background-color: #1a2236; -fx-text-fill: #cbd5e1; -fx-padding: 12 18; "
                    + "-fx-background-radius: 18 18 18 4; -fx-font-size: 14px;");
            bubble.setWrapText(true);
            bubble.setMaxWidth(580);
            row.getChildren().addAll(avatar, bubble);
            chatContainer.getChildren().add(row);
        });
    }

    private Button makeChip(String text, boolean small) {
        Button btn = new Button(text);
        String base = "-fx-background-color: #1e2433; -fx-text-fill: #94a3b8; "
                + "-fx-font-size: " + (small ? "12" : "13") + "px; "
                + "-fx-padding: " + (small ? "6 12" : "9 18") + "; "
                + "-fx-background-radius: 20; -fx-cursor: hand; -fx-border-width: 0;";
        String hover = "-fx-background-color: #2d3a52; -fx-text-fill: #e2e8f0; "
                + "-fx-font-size: " + (small ? "12" : "13") + "px; "
                + "-fx-padding: " + (small ? "6 12" : "9 18") + "; "
                + "-fx-background-radius: 20; -fx-cursor: hand; -fx-border-width: 0;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e -> btn.setStyle(base));
        return btn;
    }

    // ── AI logic ──────────────────────────────────────────────────────────────

    private void buildTextTree(StringBuilder sb, Node node, int depth) {
        sb.append(depth == 0 ? "" : (depth == 1 ? "  " : "    - ")).append(node.getText()).append("\n");
        List<Node> children = currentMap.getNodes().stream()
                .filter(n -> node.getId().equals(n.getParentId())).collect(Collectors.toList());
        for (Node child : children) buildTextTree(sb, child, depth + 1);
    }

    private String buildAiPrompt(String userRequest) {
        String context = currentMapText.isEmpty()
                ? "No mind map exists yet. Create a new one."
                : "Current mind map:\n" + currentMapText + "\n\nUpdate it based on the request.";
        return "You are a mind map expert. " + context
                + "\nUser: '" + userRequest + "'\n"
                + "IMPORTANT: Respond ONLY in this format, without Markdown:\n"
                + "Main Topic\nCategory 1\n- Subcategory 1.1\n- Subcategory 1.2\nCategory 2\n- Subcategory 2.1";
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

    private String extractTextFromJson(String json) {
        int idx = json.indexOf("\"text\": \"");
        if (idx == -1) idx = json.indexOf("\"text\":\"");
        if (idx == -1) return null;
        int start = json.indexOf('"', idx + 7) + 1;
        int end = start;
        while (end < json.length()) {
            if (json.charAt(end) == '"' && json.charAt(end - 1) != '\\') break;
            end++;
        }
        if (end >= json.length()) return null;
        return json.substring(start, end)
                   .replace("\\n", "\n").replace("\\\"", "\"")
                   .replace("\\\\", "\\").replace("\\*", "");
    }

    private void applyAiContent(String content) {
        currentMapText = content;
        clearChildren();
        int paletteIdx = 0;
        Node currentMain = null;
        String currentColor = AI_PALETTE[0];
        boolean firstLine = true;
        for (String raw : content.split("\n")) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("```")
                    || line.toLowerCase().contains("hier ist die mindmap")) continue;
            if (firstLine) {
                service.updateNodeText(currentMap, currentRoot, line);
                firstLine = false;
                continue;
            }
            if (line.startsWith("- ")) {
                if (currentMain != null) {
                    Node child = service.addNode(currentMap, currentMain.getId(), line.substring(2).trim());
                    child.setShape("ROUNDED_RECT");
                    child.setColor(currentColor);
                    repository.updateNode(child);
                }
            } else {
                currentColor = AI_PALETTE[paletteIdx % AI_PALETTE.length];
                paletteIdx++;
                currentMain = service.addNode(currentMap, currentRoot.getId(), line);
                currentMain.setShape("PILL");
                currentMain.setColor(currentColor);
                repository.updateNode(currentMain);
            }
        }
    }

    private void clearChildren() {
        if (currentRoot == null) return;
        List<Node> children = currentMap.getNodes().stream()
                .filter(n -> currentRoot.getId().equals(n.getParentId())).collect(Collectors.toList());
        for (Node child : children) service.deleteNode(currentMap, child);
    }

    private long countCategories(String content) {
        return content.lines().filter(l -> !l.trim().isEmpty() && !l.trim().startsWith("-")).count() - 1;
    }

    // ── API: Claude ───────────────────────────────────────────────────────────

    private static final String[] CLAUDE_MODELS = {
        "claude-haiku-4-5-20251001", "claude-3-5-haiku-20241022", "claude-3-haiku-20240307"
    };

    private void callClaudeApi(String prompt, String apiKey) {
        try {
            String escaped = escapeJson(buildAiPrompt(prompt));
            HttpClient client = HttpClient.newHttpClient();
            String responseBody = "";
            int statusCode = 500;

            for (String model : CLAUDE_MODELS) {
                String body = "{\"model\":\"" + model + "\",\"max_tokens\":2048,"
                        + "\"messages\":[{\"role\":\"user\",\"content\":\"" + escaped + "\"}]}";
                System.out.println("=== Claude (Chat) " + model + " ===");
                System.out.println(body);
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.anthropic.com/v1/messages"))
                        .header("Content-Type", "application/json")
                        .header("x-api-key", apiKey)
                        .header("anthropic-version", "2023-06-01")
                        .POST(HttpRequest.BodyPublishers.ofString(body)).build();
                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                statusCode = resp.statusCode();
                responseBody = resp.body();
                System.out.println("Claude (Chat) " + model + " → " + statusCode);
                System.out.println("Claude (Chat) response: " + responseBody);
                if (statusCode == 200) break;
            }

            if (statusCode != 200) {
                addAiMessage(LanguageManager.getf("ai.claudeError", statusCode, responseBody));
                return;
            }
            String content = extractTextFromJson(responseBody);
            if (content != null) {
                applyAiContent(content);
                addAiMessage(LanguageManager.getf("ai.updated",
                        countCategories(content), prompt.substring(0, Math.min(prompt.length(), 30))));
            } else {
                addAiMessage(LanguageManager.get("ai.error"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            addAiMessage(LanguageManager.getf("ai.connectionError", e.getMessage()));
        }
    }

    // ── API: Gemini ───────────────────────────────────────────────────────────

    private void callGeminiApi(String prompt, String apiKey) {
        try {
            String escaped = escapeJson(buildAiPrompt(prompt));
            String json = "{\"contents\":[{\"parts\":[{\"text\":\"" + escaped + "\"}]}]}";
            HttpClient client = HttpClient.newHttpClient();
            String[] models = {"gemini-2.5-flash", "gemini-2.0-flash"};
            String responseBody = "";
            int statusCode = 500;

            for (String model : models) {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models/"
                                + model + ":generateContent?key=" + apiKey.trim()))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json)).build();
                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                responseBody = resp.body();
                statusCode = resp.statusCode();
                if (statusCode == 200) break;
                if (statusCode == 429) try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
            }

            if (statusCode != 200) {
                addAiMessage(LanguageManager.getf("ai.geminiError", statusCode, responseBody));
                return;
            }
            String content = extractTextFromJson(responseBody);
            if (content != null) {
                applyAiContent(content);
                addAiMessage(LanguageManager.getf("ai.updated",
                        countCategories(content), prompt.substring(0, Math.min(prompt.length(), 30))));
            } else {
                addAiMessage(LanguageManager.get("ai.error"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            addAiMessage(LanguageManager.getf("ai.connectionError", e.getMessage()));
        }
    }
}
