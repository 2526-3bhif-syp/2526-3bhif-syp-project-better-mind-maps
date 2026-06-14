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
    
    // State for iterative generation
    private MindMap currentMap = null;
    private Node currentRoot = null;
    private String currentMapText = "";
    private final StringBuilder conversationHistory = new StringBuilder();

    public void setParentStage(Stage stage) {
        this.parentStage = stage;
    }

    public void setOnMapUpdated(Runnable onMapUpdated) {
        this.onMapUpdated = onMapUpdated;
    }

    public void loadExistingMap(MindMap map) {
        this.currentMap = map;
        if (!map.getNodes().isEmpty()) {
            this.currentRoot = map.getNodes().stream().filter(n -> n.getParentId() == null).findFirst().orElse(map.getNodes().get(0));
            // Create a simple text representation of the current map for the AI
            StringBuilder sb = new StringBuilder();
            buildTextRepresentation(sb, currentRoot, 0);
            this.currentMapText = sb.toString();
        }
        
        Platform.runLater(() -> {
            chatContainer.getChildren().clear();
            addAiMessage("Ich sehe, du hast bereits eine Mindmap geöffnet ('" + map.getName() + "'). Was möchtest du daran ändern oder hinzufügen?");
        });
    }

    private void buildTextRepresentation(StringBuilder sb, Node node, int depth) {
        if (depth == 0) {
            sb.append(node.getText()).append("\n");
        } else {
            sb.append(depth == 1 ? "" : "- ").append(node.getText()).append("\n");
        }
        
        List<Node> children = currentMap.getNodes().stream()
                .filter(n -> node.getId().equals(n.getParentId()))
                .collect(Collectors.toList());
                
        for (Node child : children) {
            buildTextRepresentation(sb, child, depth + 1);
        }
    }

    @FXML
    public void initialize() {
        // Auto-scroll to bottom
        chatContainer.heightProperty().addListener((observable, oldValue, newValue) -> 
            chatScrollPane.setVvalue(1.0)
        );
        addAiMessage("Hallo! Ich bin dein AI Mindmap Assistant. Worüber möchtest du eine Mindmap erstellen?");
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
                        keyDialog.setTitle("API Key benötigt");
                        keyDialog.setHeaderText("Google Gemini API Key");
                        keyDialog.setContentText("Bitte gib deinen Gemini API Key ein:");
                        keyDialog.getDialogPane().setPrefWidth(400);
                        
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
            callGeminiApi(prompt, apiKey);
        } else {
            System.out.println("Kein API Key gefunden. Nutze Fallback.");
            generateMockAiMindMap(prompt);
        }
        
        Platform.runLater(() -> {
            promptField.setDisable(false);
            if (onMapUpdated != null) {
                // We are editing an open map, just update the canvas live!
                onMapUpdated.run();
                addAiMessage("Fertig! Deine geöffnete Mindmap wurde im Hintergrund aktualisiert.");
            } else {
                // We are creating a new map from the Overview, show the button to open it
                openMapButton.setVisible(true);
                openMapButton.setManaged(true);
                addAiMessage("Fertig! Die Mindmap wurde aktualisiert. Du kannst sie dir nun ansehen oder mir weitere Anpassungswünsche schreiben.");
            }
        });
    }

    private void addUserMessage(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-background-color: #007bff; -fx-text-fill: white; -fx-padding: 10; -fx-background-radius: 10; -fx-font-size: 14px;");
        label.setWrapText(true);
        label.setMaxWidth(400);
        
        HBox container = new HBox(label);
        container.setAlignment(Pos.CENTER_RIGHT);
        
        Platform.runLater(() -> chatContainer.getChildren().add(container));
        conversationHistory.append("User: ").append(text).append("\n");
    }

    private void addAiMessage(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-background-color: #e9ecef; -fx-text-fill: #333; -fx-padding: 10; -fx-background-radius: 10; -fx-font-size: 14px;");
        label.setWrapText(true);
        label.setMaxWidth(400);
        
        HBox container = new HBox(label);
        container.setAlignment(Pos.CENTER_LEFT);
        
        Platform.runLater(() -> chatContainer.getChildren().add(container));
        conversationHistory.append("AI: ").append(text).append("\n");
    }

    private void clearMindMapChildren() {
        List<Node> children = currentMap.getNodes().stream()
                .filter(n -> currentRoot.getId().equals(n.getParentId()))
                .collect(Collectors.toList());
        for (Node child : children) {
            service.deleteNode(currentMap, child);
        }
    }

    private void callGeminiApi(String prompt, String apiKey) {
        try {
            String stateContext = currentMapText.isEmpty() ? 
                    "Es existiert noch keine Mindmap." : 
                    "Das ist die AKTUELLE Mindmap (in Textform):\n" + currentMapText + "\n\nÄndere diese Mindmap basierend auf dem neuen Nutzer-Wunsch.";
            
            String aiPrompt = "Du bist ein Mindmap-Experte. " + stateContext + "\n" +
                    "Neuer Nutzer-Wunsch: '" + prompt + "'\n" +
                    "WICHTIG: Antworte AUSSCHLIESSLICH mit der KOMPLETTEN neuen/aktualisierten Mindmap im folgenden Format, OHNE Markdown, OHNE Text davor oder danach. " +
                    "Zeile 1 MUSS das Hauptthema sein.\n" +
                    "Zeile 2 und weiter für Kategorien und Unterkategorien:\n" +
                    "Kategorie 1\n- Unterkategorie 1.1\n- Unterkategorie 1.2\nKategorie 2\n- Unterkategorie 2.1";

            String safePrompt = aiPrompt.replace("\"", "'").replace("\n", "\\n").replace("\r", "");
            String jsonPayload = "{\"contents\": [{\"parts\": [{\"text\": \"" + safePrompt + "\"}]}]}";

            HttpClient client = HttpClient.newHttpClient();
            
            // Das Modell "gemini-2.5-flash" hat vorher mit Status 200 erfolgreich geantwortet.
            // Wir nutzen wieder v1beta, da 2.5-flash dort verf\u00fcgbar ist.
            String[] models = {"gemini-2.5-flash", "gemini-2.0-flash"};
            HttpResponse<String> response = null;
            String responseBody = "";
            int statusCode = 500;
            
            for (String model : models) {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey.trim()))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                        .build();

                response = client.send(request, HttpResponse.BodyHandlers.ofString());
                responseBody = response.body();
                statusCode = response.statusCode();
                
                System.out.println("Tried model " + model + " (v1) -> Status: " + statusCode);
                
                if (statusCode == 200) {
                    break; // Erfolgreich, Schleife abbrechen!
                } else if (statusCode == 429) {
                    System.out.println("Rate limit (429) erreicht. Warte kurz...");
                    try { Thread.sleep(2000); } catch (InterruptedException e) {}
                }
            }

            if (statusCode != 200) {
                final String finalError = responseBody;
                final int finalCode = statusCode;
                Platform.runLater(() -> addAiMessage("API Fehler (" + finalCode + "): " + finalError));
                generateMockAiMindMap(prompt);
                return;
            }

            int textIndex = responseBody.indexOf("\"text\": \"");
            if (textIndex > -1) {
                int startIndex = textIndex + 9;
                // Finde das Ende des Strings, ignoriere maskierte Anführungszeichen
                int endIndex = responseBody.indexOf("\"", startIndex);
                while (endIndex > 0 && responseBody.charAt(endIndex - 1) == '\\') {
                    endIndex = responseBody.indexOf("\"", endIndex + 1);
                    if (endIndex == -1) break;
                }
                if (endIndex == -1) endIndex = responseBody.length() - 1;
                
                String content = responseBody.substring(startIndex, endIndex);
                content = content.replace("\\n", "\n").replace("\\\"", "\"").replace("\\*", "");

                // Update state text
                currentMapText = content;
                
                // Clear existing children
                clearMindMapChildren();

                Node currentMain = null;
                boolean isFirstLine = true;
                
                for (String line : content.split("\n")) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("```") || line.toLowerCase().contains("hier ist die mindmap")) continue;
                    
                    if (isFirstLine) {
                        service.updateNodeText(currentMap, currentRoot, line);
                        isFirstLine = false;
                        continue;
                    }
                    
                    if (line.startsWith("- ")) {
                        if (currentMain != null) {
                            service.addNode(currentMap, currentMain.getId(), line.substring(2).trim());
                        }
                    } else {
                        currentMain = service.addNode(currentMap, currentRoot.getId(), line);
                    }
                }
            } else {
                generateMockAiMindMap(prompt);
            }
        } catch (Exception e) {
            e.printStackTrace();
            generateMockAiMindMap(prompt);
        }
    }

    private void generateMockAiMindMap(String prompt) {
        clearMindMapChildren();
        String p = prompt.toLowerCase();
        
        if (p.contains("mehr") || p.contains("hinzu")) {
            Node basics = service.addNode(currentMap, currentRoot.getId(), "Grundlagen");
            service.addNode(currentMap, basics.getId(), "Erweiterung 1");
            service.addNode(currentMap, basics.getId(), "Erweiterung 2");
        } else {
            Node info = service.addNode(currentMap, currentRoot.getId(), "Info");
            service.addNode(currentMap, info.getId(), "Beispiel");
        }
        currentMapText = "Mock Mindmap\nInfo\n- Beispiel";
    }

    @FXML
    private void onOpenMindmap() {
        if (currentMap == null) return;
        
        if (onMapUpdated != null) {
            // Wenn wir die Mindmap ohnehin schon live geupdatet haben, schließen wir nur das Fenster
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
            Platform.runLater(() -> stage.setMaximized(true));

            if (parentStage != null) {
                ((Stage) chatContainer.getScene().getWindow()).close();
            }
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}