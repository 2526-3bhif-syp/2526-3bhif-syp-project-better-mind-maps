package htl.leonding.at;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class OverviewController {

    @FXML private FlowPane cardsFlow;
    @FXML private Label userLabel;
    @FXML private Label heroStatsLabel;
    @FXML private Label sectionLabel;
    @FXML private TextField searchField;

    private MainController mainController;
    private final MindMapRepository repository = new MindMapRepository();
    private final MindMapService service = new MindMapService(repository);
    private List<MindMap> allMaps = new ArrayList<>();

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    public void initialize() {
        SessionManager.User user = SessionManager.getCurrentUser();
        if (user != null) {
            userLabel.setText(user.getUsername());
        }
        loadMaps();

        // Show first-time tutorial after scene and layout are ready
        Platform.runLater(() -> {
            SessionManager.User u = SessionManager.getCurrentUser();
            if (u == null || cardsFlow.getScene() == null) return;

            // Target nodes: index matches STEPS[] in TutorialManager
            // 0=Welcome  1=Dashboard  2=CreateMap  3-7=editor (no live target)
            javafx.scene.Node newMapCard = cardsFlow.getChildren().isEmpty() ? null
                : cardsFlow.getChildren().get(cardsFlow.getChildren().size() - 1);

            TutorialManager.showIfNeeded(
                (javafx.scene.layout.Pane) cardsFlow.getScene().getRoot(), u.getId(),
                null,           // 0 Welcome
                cardsFlow,      // 1 Dashboard
                newMapCard,     // 2 Create Map  (the + card)
                null,           // 3 Edit Nodes
                null,           // 4 Styling
                null,           // 5 AI
                null,           // 6 Shortcuts
                null            // 7 Ready
            );
        });

        searchField.textProperty().addListener((obs, old, q) -> {
            String query = q.trim().toLowerCase();
            if (query.isEmpty()) {
                buildCards(allMaps);
            } else {
                List<MindMap> filtered = allMaps.stream()
                    .filter(m -> m.getName().toLowerCase().contains(query))
                    .collect(Collectors.toList());
                buildCards(filtered);
            }
        });
    }

    private void loadMaps() {
        String userId = SessionManager.getCurrentUser() != null ? SessionManager.getCurrentUser().getId() : null;
        if (userId == null) return;

        allMaps = repository.loadAll(userId);
        int totalNodes = allMaps.stream().mapToInt(m -> m.getNodes().size()).sum();
        heroStatsLabel.setText(allMaps.size() + " Mind Maps  ·  " + totalNodes + " Nodes");
        buildCards(allMaps);
    }

    private void buildCards(List<MindMap> maps) {
        cardsFlow.getChildren().clear();

        if (maps.isEmpty() && allMaps.isEmpty()) {
            cardsFlow.getChildren().add(createNewCard());
            return;
        }

        if (maps.isEmpty()) {
            Label noResult = new Label("No mind maps match your search.");
            noResult.getStyleClass().add("card-meta");
            noResult.setStyle("-fx-font-size: 13px; -fx-padding: 8 0;");
            cardsFlow.getChildren().add(noResult);
            return;
        }

        for (MindMap map : maps) {
            cardsFlow.getChildren().add(createCard(map));
        }

        if (searchField.getText().trim().isEmpty()) {
            cardsFlow.getChildren().add(createNewCard());
        }
    }

    // ── Card ─────────────────────────────────────────────────────────────────

    private VBox createCard(MindMap map) {
        VBox card = new VBox(0);
        card.getStyleClass().add("overview-card");
        card.setPrefWidth(280);
        card.setMaxWidth(280);
        card.setCursor(Cursor.HAND);

        // ── Thumbnail ──
        ImageView thumb = renderThumbnail(map);
        StackPane thumbPane = new StackPane();
        thumbPane.setPrefSize(280, 152);
        thumbPane.setMaxSize(280, 152);
        thumbPane.getChildren().add(thumb);

        // Clip thumbnail to top rounded corners
        Rectangle clip = new Rectangle(280, 152);
        clip.setArcWidth(14);
        clip.setArcHeight(14);
        thumbPane.setClip(clip);

        // Hover overlay
        StackPane overlay = new StackPane();
        overlay.getStyleClass().add("card-thumb-overlay");
        overlay.setPrefSize(280, 152);
        overlay.setVisible(false);
        Label openHint = new Label("Open  →");
        openHint.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: white;");
        overlay.getChildren().add(openHint);
        thumbPane.getChildren().add(overlay);

        // ── Info section ──
        VBox info = new VBox(10);
        info.getStyleClass().add("card-info");

        // Title row
        HBox titleRow = new HBox(6);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label nameLabel = new Label(map.getName());
        nameLabel.getStyleClass().add("card-name");
        nameLabel.setMaxWidth(160);
        HBox.setHgrow(nameLabel, Priority.ALWAYS);

        Button renameBtn = new Button("✏");
        renameBtn.getStyleClass().add("card-delete-btn");
        renameBtn.setStyle("-fx-text-fill: #6366f1;");
        renameBtn.setOnAction(e -> { onRenameMap(map, nameLabel); e.consume(); });
        renameBtn.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_CLICKED,
            javafx.event.Event::consume);

        String theme = map.getTheme() == null ? "LIGHT" : map.getTheme();
        Label themeBadge = new Label(themeIcon(theme) + " " + capitalize(theme));
        themeBadge.getStyleClass().add("card-badge");
        titleRow.getChildren().addAll(nameLabel, renameBtn, themeBadge);

        // Meta row
        HBox metaRow = new HBox(10);
        metaRow.setAlignment(Pos.CENTER_LEFT);
        int nodeCount = map.getNodes().size();
        Label nodeLabel = new Label("⬡  " + nodeCount + " node" + (nodeCount != 1 ? "s" : ""));
        nodeLabel.getStyleClass().add("card-meta");

        String sync = map.getSyncStatus() != null ? map.getSyncStatus() : "PENDING";
        Label syncLabel = new Label("● " + sync);
        syncLabel.getStyleClass().add("card-meta");
        syncLabel.setStyle("-fx-text-fill: " + syncColor(sync) + ";");

        Region metaSpacer = new Region();
        HBox.setHgrow(metaSpacer, Priority.ALWAYS);

        Button deleteBtn = new Button("🗑");
        deleteBtn.getStyleClass().add("card-delete-btn");
        deleteBtn.setOnAction(e -> {
            confirmDelete(map);
            e.consume();
        });
        deleteBtn.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_CLICKED,
            javafx.event.Event::consume);

        metaRow.getChildren().addAll(nodeLabel, syncLabel, metaSpacer, deleteBtn);
        info.getChildren().addAll(titleRow, metaRow);
        card.getChildren().addAll(thumbPane, info);

        // ── Interactions ──
        card.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) openEditor(map);
        });
        card.setOnMouseEntered(e -> {
            overlay.setVisible(true);
            card.getStyleClass().add("overview-card-hover");
        });
        card.setOnMouseExited(e -> {
            overlay.setVisible(false);
            card.getStyleClass().remove("overview-card-hover");
        });

        return card;
    }

    private VBox createNewCard() {
        VBox card = new VBox(12);
        card.getStyleClass().addAll("overview-card", "overview-card-create");
        card.setPrefWidth(280);
        card.setMaxWidth(280);
        card.setMinHeight(240);
        card.setAlignment(Pos.CENTER);
        card.setCursor(Cursor.HAND);

        Label icon = new Label("+");
        icon.setStyle("-fx-font-size: 38px; -fx-font-weight: bold; -fx-text-fill: #6366f1;");

        Label label = new Label("New Mind Map");
        label.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #8b94a8;");

        Label sub = new Label("Click to create");
        sub.setStyle("-fx-font-size: 11px; -fx-text-fill: #4a5568;");

        card.getChildren().addAll(icon, label, sub);
        card.setOnMouseClicked(e -> onNewMap());
        card.setOnMouseEntered(e -> card.getStyleClass().add("overview-card-hover"));
        card.setOnMouseExited(e -> card.getStyleClass().remove("overview-card-hover"));
        return card;
    }

    // ── Thumbnail rendering ───────────────────────────────────────────────────

    private ImageView renderThumbnail(MindMap map) {
        final int W = 280, H = 152;
        List<Node> nodes = map.getNodes();

        Canvas canvas = new Canvas(W, H);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        String theme = map.getTheme() == null ? "LIGHT" : map.getTheme();
        gc.setFill(Color.web(themeBg(theme)));
        gc.fillRect(0, 0, W, H);

        if (nodes.isEmpty()) {
            gc.setFill(Color.web("#2a3245"));
            gc.setFont(javafx.scene.text.Font.font(26));
            gc.fillText("🧠", W / 2.0 - 13, H / 2.0 + 10);
            return snapshot(canvas, W, H);
        }

        // Bounding box
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (Node n : nodes) {
            minX = Math.min(minX, n.getXCoordinate());
            minY = Math.min(minY, n.getYCoordinate());
            maxX = Math.max(maxX, n.getXCoordinate());
            maxY = Math.max(maxY, n.getYCoordinate());
        }

        double pad = 28;
        double rangeX = Math.max(1, maxX - minX);
        double rangeY = Math.max(1, maxY - minY);
        double scale = Math.min((W - pad * 2) / rangeX, (H - pad * 2) / rangeY);
        scale = Math.min(scale, 0.55);

        double ox = (W - rangeX * scale) / 2.0 - minX * scale;
        double oy = (H - rangeY * scale) / 2.0 - minY * scale;

        // Lines
        gc.setStroke(Color.web("DARK".equals(theme) ? "#2a3245" : "#cbd5e1"));
        gc.setLineWidth(1.2);
        for (Node n : nodes) {
            if (n.getParentId() == null) continue;
            Node parent = nodes.stream().filter(p -> p.getId().equals(n.getParentId())).findFirst().orElse(null);
            if (parent != null) {
                gc.strokeLine(
                    parent.getXCoordinate() * scale + ox, parent.getYCoordinate() * scale + oy,
                    n.getXCoordinate() * scale + ox,      n.getYCoordinate() * scale + oy
                );
            }
        }

        // Nodes
        for (Node n : nodes) {
            boolean isRoot = n.getParentId() == null;
            double nx = n.getXCoordinate() * scale + ox;
            double ny = n.getYCoordinate() * scale + oy;
            double nw = Math.max(20, 54 * scale);
            double nh = Math.max(10, 24 * scale);

            if (isRoot) {
                gc.setFill(Color.web("#6366f1"));
            } else {
                String col = n.getColor();
                if (col == null || col.isEmpty() || "#ffffff".equals(col)) {
                    col = "DARK".equals(theme) ? "#1e2433" : "#e2e8f0";
                }
                try { gc.setFill(Color.web(col)); } catch (Exception ex) { gc.setFill(Color.web("#e2e8f0")); }
            }
            gc.fillRoundRect(nx - nw / 2, ny - nh / 2, nw, nh, 5, 5);
        }

        return snapshot(canvas, W, H);
    }

    private ImageView snapshot(Canvas canvas, int w, int h) {
        WritableImage img = canvas.snapshot(null, null);
        ImageView iv = new ImageView(img);
        iv.setFitWidth(w);
        iv.setFitHeight(h);
        iv.setPreserveRatio(false);
        iv.setSmooth(true);
        return iv;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String themeBg(String theme) {
        switch (theme) {
            case "DARK":  return "#0d1117";
            case "SEPIA": return "#f5f0e8";
            case "OCEAN": return "#e0f7ff";
            default:      return "#f1f5f9";
        }
    }

    private String themeIcon(String theme) {
        switch (theme) {
            case "DARK":  return "🌙";
            case "SEPIA": return "📜";
            case "OCEAN": return "🌊";
            default:      return "☀";
        }
    }

    private String syncColor(String sync) {
        if ("SYNCED".equals(sync))  return "#10b981";
        if ("FAILED".equals(sync))  return "#f43f5e";
        return "#f59e0b";
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    private void applyTheme(Dialog<?> dialog) {
        dialog.getDialogPane().getStylesheets().add(
            getClass().getResource("styles.css").toExternalForm()
        );
    }

    // ── Actions ──────────────────────────────────────────────────────────────

    private void confirmDelete(MindMap map) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Mind Map löschen");
        confirm.setHeaderText("\"" + map.getName() + "\" löschen?");
        confirm.setContentText("Diese Aktion kann nicht rückgängig gemacht werden.");
        applyTheme(confirm);
        confirm.showAndWait()
            .filter(btn -> btn == ButtonType.OK)
            .ifPresent(btn -> {
                service.deleteMindMap(map.getId());
                loadMaps();
            });
    }

    @FXML
    private void onNewMap() {
        TextInputDialog nameDialog = new TextInputDialog();
        nameDialog.setTitle("New Mind Map");
        nameDialog.setHeaderText("Neue Mind Map erstellen");
        nameDialog.setContentText("Name der Mind Map:");
        applyTheme(nameDialog);
        Optional<String> nameResult = nameDialog.showAndWait();
        if (nameResult.isEmpty() || nameResult.get().trim().isEmpty()) return;
        MindMap map = service.createMindMap(nameResult.get().trim());
        openEditor(map);
    }

    private void onRenameMap(MindMap map, Label nameLabel) {
        TextInputDialog dialog = new TextInputDialog(map.getName());
        dialog.setTitle("Mind Map umbenennen");
        dialog.setHeaderText("Neuer Name für \"" + map.getName() + "\"");
        dialog.setContentText("Name:");
        applyTheme(dialog);
        dialog.showAndWait()
            .map(String::trim).filter(s -> !s.isEmpty())
            .ifPresent(newName -> {
                map.setName(newName);
                repository.updateMapName(map.getId(), newName);
                nameLabel.setText(newName);
            });
    }

    private void openEditor(MindMap map) {
        Stage stage = (Stage) cardsFlow.getScene().getWindow();
        if (mainController != null) {
            stage.setScene(mainController.getRootScene());
            Platform.runLater(() -> { stage.setMaximized(true); WindowsDarkMode.applyToAllWindows(); });
            mainController.openMapAsTab(map);
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("main-view.fxml"));
            Scene scene = new Scene(loader.load(), 1280, 800);
            MainController controller = loader.getController();
            controller.loadMindMap(map);
            stage.setScene(scene);
            Platform.runLater(() -> { stage.setMaximized(true); WindowsDarkMode.applyToAllWindows(); });
        } catch (IOException e) {
            throw new RuntimeException("Failed to open editor", e);
        }
    }

    @FXML
    private void onLogout() {
        SessionManager.logout();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("login-view.fxml"));
            Scene scene = new Scene(loader.load(), 1024, 768);
            Stage stage = (Stage) cardsFlow.getScene().getWindow();
            stage.setScene(scene);
            Platform.runLater(() -> { stage.setMaximized(true); WindowsDarkMode.applyToAllWindows(); });
        } catch (IOException e) {
            throw new RuntimeException("Failed to load login screen", e);
        }
    }
}
