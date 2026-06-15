package htl.leonding.at;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.*;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

/**
 * Floating dark-themed node style editor — color gradient picker, shape grid,
 * emoji icon picker, status badge selector, text-size slider.
 */
public class NodeStyleEditor {

    // ── Palette ───────────────────────────────────────────────────────────────

    static final String[] PRESETS = {
        "#6366f1","#8b5cf6","#a855f7","#ec4899",
        "#f43f5e","#f97316","#eab308","#84cc16",
        "#22c55e","#10b981","#14b8a6","#06b6d4",
        "#3b82f6","#0284c7","#1d4ed8","#4f46e5",
        "#0f172a","#1e293b","#334155","#475569",
        "#ff6b6b","#ffd93d","#6bcb77","#4d96ff"
    };

    static final String[][] BADGES = {
        {"🔥", "HOT",   "#ef4444"},
        {"💡", "IDEA",  "#f59e0b"},
        {"✅", "DONE",  "#10b981"},
        {"⚠️", "WARN",  "#f97316"},
        {"🔗", "LINK",  "#3b82f6"},
        {"⭐", "STAR",  "#8b5cf6"},
        {"❌", "BLOCK", "#6b7280"},
        {"–",  "",      "none"}
    };

    static final String[] ICONS = {
        "💡","🔥","⚡","🎯","📌","🔗","💎","🚀",
        "⭐","❤️","✅","📝","🔑","💰","🌟","🧠",
        "📊","🎨","🔬","🌍","💻","🎵","🏆","–"
    };

    static final String[][] SHAPES = {
        {"ROUNDED_RECT",  "▭", "Abgerundet"},
        {"PILL",          "⬬", "Pille"},
        {"ELLIPSE",       "⬭", "Ellipse"},
        {"DIAMOND",       "◇", "Raute"},
        {"HEXAGON",       "⬡", "Hexagon"},
        {"STAR",          "★", "Stern"},
        {"PARALLELOGRAM", "▱", "Parallelogramm"},
        {"OCTAGON",       "⎔", "Oktagon"},
    };

    // ── State ─────────────────────────────────────────────────────────────────

    private final Node node;
    private final MindMapRepository repository;
    private final Runnable onChanged;

    private final double[] hsv = new double[3]; // [hue 0-360, sat 0-1, val 0-1]

    private Canvas svCanvas;
    private Canvas hueStrip;
    private Label colorPreviewBox;
    private TextField hexField;

    private Stage popup;
    private final double[] dragStart = {0, 0};

    // ── Entry point ───────────────────────────────────────────────────────────

    public static void show(Node node, MindMapRepository repository,
                            Runnable onChanged, Window owner) {
        new NodeStyleEditor(node, repository, onChanged).open(owner);
    }

    private NodeStyleEditor(Node node, MindMapRepository repository, Runnable onChanged) {
        this.node = node;
        this.repository = repository;
        this.onChanged = onChanged;
        parseColor(node.getColor());
    }

    private void parseColor(String hex) {
        try {
            Color c = Color.web(hex == null || hex.isEmpty() ? "#6366f1" : hex);
            hsv[0] = c.getHue();
            hsv[1] = c.getSaturation();
            hsv[2] = c.getBrightness();
        } catch (Exception e) {
            hsv[0] = 240; hsv[1] = 0.7; hsv[2] = 0.9;
        }
    }

    // ── Build UI ──────────────────────────────────────────────────────────────

    private void open(Window owner) {
        popup = new Stage();
        popup.initStyle(StageStyle.UNDECORATED);
        popup.initModality(Modality.NONE);
        popup.initOwner(owner);

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #111827; -fx-border-color: #1e293b; -fx-border-width: 1;");
        root.setPrefWidth(310);

        root.getChildren().addAll(
            buildHeader(),
            new ScrollPane(buildBody()) {{
                setFitToWidth(true);
                setMaxHeight(560);
                setStyle("-fx-background: #111827; -fx-background-color: #111827; -fx-border-width: 0;");
            }}
        );

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());
        popup.setScene(scene);

        // Position near centre of owning window
        popup.show();
        popup.setX(owner.getX() + owner.getWidth() / 2 - 155);
        popup.setY(owner.getY() + owner.getHeight() / 2 - 320);
    }

    // ── Header (drag handle + title + close) ─────────────────────────────────

    private HBox buildHeader() {
        Label title = new Label("✨  Node bearbeiten");
        title.setStyle("-fx-text-fill: #f1f5f9; -fx-font-size: 14px; -fx-font-weight: bold;");

        Button close = new Button("✕");
        close.setStyle("-fx-background-color: transparent; -fx-text-fill: #64748b; -fx-font-size: 14px; "
                + "-fx-cursor: hand; -fx-padding: 2 8; -fx-border-width: 0;");
        close.setOnMouseEntered(e -> close.setStyle(
                "-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-size: 14px; "
                + "-fx-cursor: hand; -fx-padding: 2 8; -fx-border-width: 0;"));
        close.setOnMouseExited(e -> close.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #64748b; -fx-font-size: 14px; "
                + "-fx-cursor: hand; -fx-padding: 2 8; -fx-border-width: 0;"));
        close.setOnAction(e -> popup.close());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(spacer, close);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10, 12, 10, 16));
        header.setStyle("-fx-background-color: #0f172a; -fx-border-color: #1e293b; -fx-border-width: 0 0 1 0;");
        header.getChildren().add(0, title);

        // Drag support
        header.setOnMousePressed(e -> { dragStart[0] = e.getScreenX() - popup.getX(); dragStart[1] = e.getScreenY() - popup.getY(); });
        header.setOnMouseDragged(e -> { popup.setX(e.getScreenX() - dragStart[0]); popup.setY(e.getScreenY() - dragStart[1]); });

        return header;
    }

    // ── Main body ─────────────────────────────────────────────────────────────

    private VBox buildBody() {
        VBox body = new VBox(0);
        body.setStyle("-fx-background-color: #111827;");
        body.getChildren().addAll(
            section("🎨  Farbe",       buildColorPicker()),
            divider(),
            section("◆  Form",         buildShapeGrid()),
            divider(),
            section("📏  Textgröße",   buildSizeSlider()),
            divider(),
            section("😀  Icon",        buildIconPicker()),
            divider(),
            section("🏷️  Status",      buildBadgePicker())
        );
        return body;
    }

    private VBox section(String title, javafx.scene.Node content) {
        Label lbl = new Label(title);
        lbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-weight: bold;");
        lbl.setPadding(new Insets(12, 16, 6, 16));
        VBox box = new VBox(0, lbl, content);
        box.setStyle("-fx-background-color: #111827;");
        return box;
    }

    private Separator divider() {
        Separator s = new Separator();
        s.setStyle("-fx-background-color: #1e293b;");
        s.setPadding(new Insets(2, 0, 2, 0));
        return s;
    }

    // ── Color picker ──────────────────────────────────────────────────────────

    private VBox buildColorPicker() {
        // SV canvas
        svCanvas = new Canvas(278, 150);
        drawSV();
        svCanvas.setOnMouseClicked(e  -> onSVPick(e.getX(), e.getY()));
        svCanvas.setOnMouseDragged(e -> onSVPick(e.getX(), e.getY()));

        // Hue strip
        hueStrip = new Canvas(278, 18);
        drawHue();
        hueStrip.setOnMouseClicked(e  -> onHuePick(e.getX()));
        hueStrip.setOnMouseDragged(e -> onHuePick(e.getX()));

        // Preview + hex
        colorPreviewBox = new Label();
        colorPreviewBox.setMinSize(36, 36);
        colorPreviewBox.setMaxSize(36, 36);
        colorPreviewBox.setStyle("-fx-background-radius: 8; -fx-background-color: " + currentHex() + ";");

        hexField = new TextField(currentHex());
        hexField.setStyle("-fx-background-color: #1e2433; -fx-text-fill: #f1f5f9; "
                + "-fx-font-size: 13px; -fx-padding: 6 10; -fx-background-radius: 8; -fx-border-width: 0;");
        hexField.setPrefWidth(100);
        hexField.setOnAction(e -> applyHex(hexField.getText().trim()));
        hexField.focusedProperty().addListener((obs, o, focused) -> {
            if (!focused) applyHex(hexField.getText().trim());
        });

        HBox hexRow = new HBox(10, colorPreviewBox, hexField);
        hexRow.setAlignment(Pos.CENTER_LEFT);
        hexRow.setPadding(new Insets(8, 16, 6, 16));

        // Preset grid (6×4 = 24 colors)
        FlowPane presets = new FlowPane(6, 6);
        presets.setPadding(new Insets(0, 16, 10, 16));
        for (String hex : PRESETS) {
            Label swatch = new Label();
            swatch.setMinSize(26, 26);
            swatch.setMaxSize(26, 26);
            boolean selected = hex.equalsIgnoreCase(currentHex());
            swatch.setStyle("-fx-background-color: " + hex + "; -fx-background-radius: 6;"
                    + (selected ? " -fx-border-color: white; -fx-border-width: 2; -fx-border-radius: 6;" : ""));
            swatch.setCursor(javafx.scene.Cursor.HAND);
            swatch.setOnMouseClicked(e -> {
                parseColor(hex);
                applyColorToNode();
                redrawAll();
            });
            swatch.setTooltip(new Tooltip(hex));
            presets.getChildren().add(swatch);
        }

        VBox box = new VBox(6,
            wrap(svCanvas),
            wrap(hueStrip),
            hexRow,
            presets
        );
        box.setStyle("-fx-background-color: #111827; -fx-padding: 0 16 0 16;");
        return box;
    }

    private StackPane wrap(Canvas c) {
        StackPane sp = new StackPane(c);
        sp.setStyle("-fx-background-color: #1e2433; -fx-background-radius: 8;");
        return sp;
    }

    private void drawSV() {
        GraphicsContext gc = svCanvas.getGraphicsContext2D();
        double w = svCanvas.getWidth(), h = svCanvas.getHeight();
        for (int col = 0; col < (int) w; col++) {
            double sat = col / w;
            gc.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.hsb(hsv[0], sat, 1.0)),
                new Stop(1, Color.hsb(hsv[0], sat, 0.0))));
            gc.fillRect(col, 0, 1, h);
        }
        // Cursor
        double cx = hsv[1] * w, cy = (1 - hsv[2]) * h;
        gc.setStroke(Color.WHITE); gc.setLineWidth(2);
        gc.strokeOval(cx - 6, cy - 6, 12, 12);
        gc.setStroke(Color.BLACK); gc.setLineWidth(1);
        gc.strokeOval(cx - 7, cy - 7, 14, 14);
    }

    private void drawHue() {
        GraphicsContext gc = hueStrip.getGraphicsContext2D();
        double w = hueStrip.getWidth(), h = hueStrip.getHeight();
        for (int x = 0; x < (int) w; x++) {
            gc.setFill(Color.hsb(x / w * 360.0, 1.0, 1.0));
            gc.fillRect(x, 0, 1, h);
        }
        // Cursor
        double pos = hsv[0] / 360.0 * w;
        gc.setStroke(Color.WHITE); gc.setLineWidth(2);
        gc.strokeRoundRect(pos - 5, 1, 10, h - 2, 4, 4);
        gc.setStroke(Color.BLACK); gc.setLineWidth(1);
        gc.strokeRoundRect(pos - 6, 0, 12, h, 5, 5);
    }

    private void onSVPick(double mx, double my) {
        hsv[1] = Math.max(0, Math.min(1, mx / svCanvas.getWidth()));
        hsv[2] = Math.max(0, Math.min(1, 1 - my / svCanvas.getHeight()));
        applyColorToNode();
        redrawAll();
    }

    private void onHuePick(double mx) {
        hsv[0] = Math.max(0, Math.min(360, mx / hueStrip.getWidth() * 360.0));
        applyColorToNode();
        redrawAll();
    }

    private void applyHex(String raw) {
        String hex = raw.startsWith("#") ? raw : "#" + raw;
        try { Color.web(hex); } catch (Exception e) { return; }
        parseColor(hex);
        applyColorToNode();
        redrawAll();
    }

    private void redrawAll() {
        drawSV();
        drawHue();
        String hex = currentHex();
        colorPreviewBox.setStyle("-fx-background-radius: 8; -fx-background-color: " + hex + ";");
        hexField.setText(hex);
    }

    private String currentHex() {
        Color c = Color.hsb(hsv[0], hsv[1], hsv[2]);
        return String.format("#%02x%02x%02x",
            (int)(c.getRed() * 255), (int)(c.getGreen() * 255), (int)(c.getBlue() * 255));
    }

    private void applyColorToNode() {
        node.setColor(currentHex());
        saveAndRefresh();
    }

    // ── Shape grid ────────────────────────────────────────────────────────────

    private FlowPane buildShapeGrid() {
        FlowPane grid = new FlowPane(8, 8);
        grid.setPadding(new Insets(0, 16, 12, 16));
        for (String[] s : SHAPES) {
            String key = s[0], symbol = s[1], tip = s[2];
            boolean active = key.equals(node.getShape());
            Button btn = new Button(symbol);
            btn.setTooltip(new Tooltip(tip));
            btn.setFont(Font.font(18));
            String baseStyle = "-fx-background-color: " + (active ? "#6366f1" : "#1e2433")
                    + "; -fx-text-fill: " + (active ? "white" : "#94a3b8")
                    + "; -fx-padding: 8 14; -fx-background-radius: 10; -fx-cursor: hand; -fx-border-width: 0;";
            btn.setStyle(baseStyle);
            btn.setOnMouseEntered(e -> btn.setStyle(baseStyle.replace(active ? "#6366f1" : "#1e2433", "#2d3a52").replace(active ? "white" : "#94a3b8", "#e2e8f0")));
            btn.setOnMouseExited(e -> btn.setStyle(baseStyle));
            btn.setOnAction(e -> {
                node.setShape(key);
                saveAndRefresh();
                popup.close();
                // Reopen to refresh active state
                NodeStyleEditor.show(node, repository, onChanged,
                        popup.getOwner() != null ? popup.getOwner() : popup);
            });
            grid.getChildren().add(btn);
        }
        return grid;
    }

    // ── Text size slider ──────────────────────────────────────────────────────

    private VBox buildSizeSlider() {
        Slider slider = new Slider(9, 26, node.getTextSize());
        slider.setShowTickMarks(true);
        slider.setMajorTickUnit(4);
        slider.setStyle("-fx-control-inner-background: #1e2433;");

        Label preview = new Label("Aa");
        preview.setStyle("-fx-text-fill: #f1f5f9; -fx-font-size: " + (int) node.getTextSize() + "px;");
        preview.setMinWidth(35);

        Label val = new Label((int) node.getTextSize() + "px");
        val.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");
        val.setMinWidth(30);

        slider.valueProperty().addListener((obs, o, nv) -> {
            double size = Math.round(nv.doubleValue());
            preview.setStyle("-fx-text-fill: #f1f5f9; -fx-font-size: " + (int) size + "px;");
            val.setText((int) size + "px");
            node.setTextSize(size);
            saveAndRefresh();
        });

        HBox row = new HBox(10, preview, slider, val);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(slider, Priority.ALWAYS);
        row.setPadding(new Insets(0, 16, 12, 16));
        return new VBox(row);
    }

    // ── Icon picker ───────────────────────────────────────────────────────────

    private FlowPane buildIconPicker() {
        FlowPane grid = new FlowPane(6, 6);
        grid.setPadding(new Insets(0, 16, 12, 16));
        for (String ico : ICONS) {
            boolean clear = "–".equals(ico);
            boolean active = ico.equals(node.getIcon()) || (clear && node.getIcon().isEmpty());
            Button btn = new Button(clear ? "✕" : ico);
            btn.setFont(Font.font(16));
            btn.setTooltip(new Tooltip(clear ? "Icon entfernen" : ico));
            String bg = active ? (clear ? "#ef4444" : "#6366f1") : "#1e2433";
            btn.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: white; "
                    + "-fx-padding: 6 10; -fx-background-radius: 8; -fx-cursor: hand; -fx-border-width: 0;");
            btn.setOnAction(e -> {
                node.setIcon(clear ? "" : ico);
                saveAndRefresh();
                popup.close();
                NodeStyleEditor.show(node, repository, onChanged,
                        popup.getOwner() != null ? popup.getOwner() : popup);
            });
            grid.getChildren().add(btn);
        }
        return grid;
    }

    // ── Badge picker ──────────────────────────────────────────────────────────

    private FlowPane buildBadgePicker() {
        FlowPane grid = new FlowPane(8, 8);
        grid.setPadding(new Insets(0, 16, 14, 16));
        for (String[] b : BADGES) {
            String emoji = b[0], key = b[1], color = b[2];
            boolean active = key.equals(node.getBadge()) || ("".equals(key) && node.getBadge().isEmpty());
            boolean clear = "".equals(key);
            Button btn = new Button(emoji + (clear ? " Kein" : " " + key));
            btn.setTooltip(new Tooltip(clear ? "Badge entfernen" : key));
            String bg = active ? (clear ? "#374151" : color) : "#1e2433";
            btn.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: white; -fx-font-size: 12px;"
                    + "-fx-padding: 6 12; -fx-background-radius: 20; -fx-cursor: hand; -fx-border-width: 0;");
            btn.setOnAction(e -> {
                node.setBadge(key);
                saveAndRefresh();
                popup.close();
                NodeStyleEditor.show(node, repository, onChanged,
                        popup.getOwner() != null ? popup.getOwner() : popup);
            });
            grid.getChildren().add(btn);
        }
        return grid;
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    private void saveAndRefresh() {
        repository.updateNode(node);
        if (onChanged != null) onChanged.run();
    }
}
