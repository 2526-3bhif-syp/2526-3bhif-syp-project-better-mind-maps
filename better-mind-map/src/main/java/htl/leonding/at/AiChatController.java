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
    @FXML private Button openMapButton;

    private final MindMapRepository repository = new MindMapRepository();
    private final MindMapService service = new MindMapService(repository);
    private static String userApiKey = null;

    private Stage parentStage;
    private Runnable onMapUpdated;

    private MindMap currentMap = null;
    private Node currentRoot = null;
    private String currentMapText = "";
    private final StringBuilder conversationHistory = new StringBuilder();

    private static final String[] AI_PALETTE = {
        "#4f46e5", "#0891b2", "#059669", "#d97706", "#dc2626", "#7c3aed", "#db2777", "#0284c7"
    };

    public void setParentStage(Stage stage) { this.parentStage = stage; }
    public void setOnMapUpdated(Runnable r) { this.onMapUpdated = r; }

    public void loadExistingMap(MindMap map) {
        this.currentMap = map;
        if (!map.getNodes().isEmpty()) {
            this.currentRoot = map.getNodes().stream()
                .filter(n -> n.getParentId() == null).findFirst()
                .orElse(map.getNodes().get(0));
            StringBuilder sb = new StringBuilder();
            buildTextRepresentation(sb, currentRoot, 0);
            this.currentMapText = sb.toString();
        }
        Platform.runLater(() -> {
            chatContainer.getChildren().clear();
            addAiMessage("Ich sehe deine Mindmap '" + map.getName() + "'. Was moechtest du aendern?");
        });
    }

    private void buildTextRepresentation(StringBuilder sb, Node node, int depth) {
        sb.append(depth == 0 ? "" : (depth == 1 ? "" : "- ")).append(node.getText()).append("\n");
        List<Node> children = currentMap.getNodes().stream()
                .filter(n -> node.getId().equals(n.getParentId()))
                .collect(Collectors.toList());
        for (Node child : children) buildTextRepresentation(sb, child, depth + 1);
    }

    @FXML
    public void initialize() {
        chatContainer.heightProperty().addListener((obs, o, n) -> chatScrollPane.setVvalue(1.0));
        addAiMessage("Hallo! Ich bin dein AI Mindmap Assistant. Worueber moechtest du eine Mindmap erstellen?");
    }

    @FXML
    private void onSend() {
        String prompt = promptField.getText().trim();
        if (prompt.isEmpty()) return;
        promptField.clear();
        addUserMessage(prompt);
        promptField.setDisable(true);
        addAiMessage("Ich bearbeite deine Anfrage... Bitte warten.");

        new Thread(() -> {
            if (currentMap == null) {
                currentMap = service.createMindMap("AI: " + prompt.substring(0, Math.min(prompt.length(), 20)));
                currentRoot = currentMap.getNodes().get(0);
            }

            String apiKey = System.getenv("MINDMAP_AI_KEY");
            if (apiKey == null || apiKey.trim().isEmpty()) {
                if (userApiKey != null && !userApiKey.trim().isEmpty()) {
                    apiKey = userApiKey;
                } else {
                    Platform.runLater(() -> {
                        TextInputDialog keyDialog = new TextInputDialog();
                        keyDialog.setTitle("API Key benoetigt");
                        keyDialog.setHeaderText("Gemini oder Claude API Key");
                        keyDialog.setContentText("API Key (Gemini oder sk-ant-... fuer Claude):");
                        keyDialog.getDialogPane().setPrefWidth(440);
                        Optional<String> keyResult = keyDialog.showAndWait();
                        if (keyResult.isPresent() && !keyResult.get().trim().isEmpty()) {
                            userApiKey = keyResult.get().trim();
                            new Thread(() -> processAiRequest(prompt, userApiKey)).start();
                        } else {
                            new Thread(() -> processAiRequest(prompt, null)).start();
                        }
                    });
                    return;
                }
            }
            processAiRequest(prompt, apiKey);
        }).start();
    }

    private void processAiRequest(String prompt, String apiKey) {
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            if (apiKey.startsWith("sk-ant-")) {
                callClaudeApi(prompt, apiKey);
            } else {
                callGeminiApi(prompt, apiKey);
            }
        } else {
            generateMockAiMindMap(prompt);
        }

        Platform.runLater(() -> {
            promptField.setDisable(false);
            if (onMapUpdated != null) {
                onMapUpdated.run();
                addAiMessage("Fertig! Deine Mindmap wurde aktualisiert.");
            } else {
                openMapButton.setVisible(true);
                openMapButton.setManaged(true);
                addAiMessage("Fertig! Klicke auf 'Mindmap oeffnen' oder stelle weitere Fragen.");
            }
        });
    }

    // ── AI prompt builder ────────────────────────────────────────────────────

    private String buildAiPrompt(String prompt) {
        String context = currentMapText.isEmpty()
                ? "Es existiert noch keine Mindmap."
                : "Aktuelle Mindmap:\n" + currentMapText + "\n\nAktualisiere sie basierend auf dem Nutzer-Wunsch.";
        return "Du bist ein Mindmap-Experte. " + context + "\n" +
               "Nutzer-Wunsch: '" + prompt + "'\n" +
               "WICHTIG: Antworte AUSSCHLIESSLICH ohne Markdown:\n" +
               "Hauptthema\nKategorie 1\n- Unterkategorie 1.1\n- Unterkategorie 1.2\nKategorie 2\n- Unterkategorie 2.1";
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
                   .replace("\\n", "\n").replace("\\\"", "\"")
                   .replace("\\\\", "\\").replace("\\*", "");
    }

    private void applyAiContent(String content) {
        currentMapText = content;
        clearMindMapChildren();
        int paletteIdx = 0;
        Node currentMain = null;
        String currentColor = AI_PALETTE[0];
        boolean isFirstLine = true;

        for (String line : content.split("\n")) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("```")
                    || line.toLowerCase().contains("hier ist die mindmap")) continue;
            if (isFirstLine) {
                service.updateNodeText(currentMap, currentRoot, line);
                isFirstLine = false;
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

    // ── API calls ─────────────────────────────────────────────────────────────

    private void callGeminiApi(String prompt, String apiKey) {
        try {
            String safe = buildAiPrompt(prompt).replace("\"", "'").replace("\n", "\\n").replace("\r", "");
            String json = "{\"contents\":[{\"parts\":[{\"text\":\"" + safe + "\"}]}]}";

            HttpClient client = HttpClient.newHttpClient();
            String[] models = {"gemini-2.5-flash", "gemini-2.0-flash"};
            String responseBody = "";
            int statusCode = 500;

            for (String model : models) {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models/"
                                + model + ":generateContent?key=" + apiKey.trim()))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();
                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                responseBody = resp.body();
                statusCode = resp.statusCode();
                if (statusCode == 200) break;
                if (statusCode == 429) try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
            }

            if (statusCode != 200) {
                final String err = responseBody;
                final int code = statusCode;
                Platform.runLater(() -> addAiMessage("API Fehler (" + code + "): " + err));
                generateMockAiMindMap(prompt);
                return;
            }

            String content = extractTextFromJson(responseBody);
            if (content != null) applyAiContent(content);
            else generateMockAiMindMap(prompt);
        } catch (Exception e) {
            e.printStackTrace();
            generateMockAiMindMap(prompt);
        }
    }

    private void callClaudeApi(String prompt, String apiKey) {
        try {
            String escaped = buildAiPrompt(prompt)
                    .replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
            String jsonBody = "{\"model\":\"claude-haiku-4-5-20251001\",\"max_tokens\":2048,"
                    + "\"messages\":[{\"role\":\"user\",\"content\":\"" + escaped + "\"}]}";

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.anthropic.com/v1/messages"))
                    .header("Content-Type", "application/json")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                final String err = resp.body();
                Platform.runLater(() -> addAiMessage("Claude Fehler: " + err));
                generateMockAiMindMap(prompt);
                return;
            }
            String content = extractTextFromJson(resp.body());
            if (content != null) applyAiContent(content);
            else generateMockAiMindMap(prompt);
        } catch (Exception e) {
            e.printStackTrace();
            generateMockAiMindMap(prompt);
        }
    }

    private void generateMockAiMindMap(String prompt) {
        clearMindMapChildren();
        Node info = service.addNode(currentMap, currentRoot.getId(), "Info");
        info.setShape("PILL"); info.setColor(AI_PALETTE[0]);
        repository.updateNode(info);
        Node ex = service.addNode(currentMap, info.getId(), "Beispiel");
        ex.setColor(AI_PALETTE[0]); repository.updateNode(ex);
        currentMapText = "Mock\nInfo\n- Beispiel";
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void clearMindMapChildren() {
        List<Node> children = currentMap.getNodes().stream()
                .filter(n -> currentRoot.getId().equals(n.getParentId()))
                .collect(Collectors.toList());
        for (Node child : children) service.deleteNode(currentMap, child);
    }

    private void addUserMessage(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-background-color: #6366f1; -fx-text-fill: white; -fx-padding: 10; -fx-background-radius: 10; -fx-font-size: 14px;");
        label.setWrapText(true);
        label.setMaxWidth(420);
        HBox container = new HBox(label);
        container.setAlignment(Pos.CENTER_RIGHT);
        Platform.runLater(() -> chatContainer.getChildren().add(container));
        conversationHistory.append("User: ").append(text).append("\n");
    }

    private void addAiMessage(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-background-color: #1e2433; -fx-text-fill: #cbd5e1; -fx-padding: 10; -fx-background-radius: 10; -fx-font-size: 14px;");
        label.setWrapText(true);
        label.setMaxWidth(420);
        HBox container = new HBox(label);
        container.setAlignment(Pos.CENTER_LEFT);
        Platform.runLater(() -> chatContainer.getChildren().add(container));
        conversationHistory.append("AI: ").append(text).append("\n");
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    @FXML
    private void onOpenMindmap() {
        if (currentMap == null) return;

        if (onMapUpdated != null) {
            Stage window = (Stage) chatContainer.getScene().getWindow();
            if (window != null) window.close();
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("main-view.fxml"));
            Scene scene = new Scene(loader.load(), 1024, 768);
            MainController controller = loader.getController();
            controller.loadMindMap(currentMap);

            Stage stage = parentStage != null ? parentStage : (Stage) chatContainer.getScene().getWindow();
            stage.setScene(scene);
            Platform.runLater(() -> { stage.setMaximized(true); WindowsDarkMode.applyToAllWindows(); });

            if (parentStage != null) {
                ((Stage) chatContainer.getScene().getWindow()).close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
