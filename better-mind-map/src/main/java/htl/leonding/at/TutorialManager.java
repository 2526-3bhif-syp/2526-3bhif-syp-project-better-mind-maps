package htl.leonding.at;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.sql.*;

public class TutorialManager {

    // {emoji, title, desc, String[] features, color1, color2}
    private static final Object[][] STEPS = {
        {"🧠", "Willkommen bei\nBetter Mind Maps!",
         "Die smarte App für deine Gedanken, Ideen und Projekte.\nLass uns kurz die wichtigsten Funktionen zeigen.",
         null, "#6366f1", "#8b5cf6"},

        {"🗂️", "Dein Dashboard",
         "Alle deine Mind Maps auf einen Blick.\nErstelle, öffne, benenne um oder lösche sie.",
         new String[]{"➕  Neue Map erstellen", "✏️  Umbenennen", "🗑️  Löschen", "🔍  Suchen"},
         "#3b82f6", "#6366f1"},

        {"✨", "Mind Map erstellen",
         "Klicke auf + und gib einen Namen ein.\nDeine Map ist in Sekunden bereit.",
         new String[]{"1×  Klick auf +", "Name eingeben", "Sofort geöffnet"},
         "#10b981", "#059669"},

        {"🌿", "Nodes bearbeiten",
         "Baue deine Mindmap mit intuitiven Shortcuts auf.",
         new String[]{"Enter → Child Node", "F2 → Text bearbeiten", "Del → Löschen",
                      "Ziehen → Verschieben", "Pfeiltasten → Navigation"},
         "#14b8a6", "#0891b2"},

        {"🎨", "Node Styling",
         "Gestalte jeden Node einzigartig.\nRechtsklick auf einen Node → 'Stil bearbeiten'",
         new String[]{"Voller Farbverlauf", "8 Formen (Hexagon, Stern...)",
                      "Emojis & Icons", "HOT · DONE · IDEA · WARN Badges"},
         "#8b5cf6", "#7c3aed"},

        {"🤖", "KI-Assistent",
         "Lass die KI deine Mindmap automatisch aufbauen\nund mit Farben & Formen gestalten.",
         new String[]{"Claude API Key (sk-ant-...)", "Gemini API Key",
                      "Automatische Struktur", "KI wählt Farben & Formen"},
         "#f59e0b", "#d97706"},

        {"⌨️", "Tastenkürzel",
         "Arbeite schneller mit diesen Shortcuts.",
         new String[]{"Enter → Child Node", "F2 → Bearbeiten", "Del → Löschen",
                      "Pfeiltasten → Navigation", "F → Vollbild", "Strg+Z → Rückgängig"},
         "#ec4899", "#db2777"},

        {"🚀", "Du bist bereit!",
         "Erstelle jetzt deine erste Mind Map\nund entfalte deine Kreativität!",
         null, "#22c55e", "#16a34a"}
    };

    private static int currentStep;
    private static javafx.scene.Node[] stepTargets;
    private static Canvas spotlightCanvas;
    private static StackPane contentArea;
    private static HBox dotsContainer;
    private static VBox tutorialCard;

    // ── Public API ────────────────────────────────────────────────────────────

    public static void showIfNeeded(Pane rootPane, String userId,
                                    javafx.scene.Node... targets) {
        if (isShown(userId)) return;
        show(rootPane, userId, targets);
    }

    // ── DB ────────────────────────────────────────────────────────────────────

    private static boolean isShown(String userId) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT tutorial_shown FROM users WHERE id = ?")) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("tutorial_shown") == 1;
            }
        } catch (Exception ignored) {}
        return false;
    }

    private static void markShown(String userId) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE users SET tutorial_shown = 1 WHERE id = ?")) {
            ps.setString(1, userId);
            ps.executeUpdate();
        } catch (Exception ignored) {}
    }

    // ── Overlay ───────────────────────────────────────────────────────────────

    private static void show(Pane rootPane, String userId, javafx.scene.Node[] targets) {
        currentStep = 0;
        stepTargets = targets;

        Scene scene = rootPane.getScene();
        Parent originalRoot = scene.getRoot();

        // Swap scene root: put original inside a StackPane wrapper so the
        // overlay Pane (sibling) gets properly sized by StackPane layout.
        StackPane wrapper = new StackPane();
        scene.setRoot(wrapper);
        wrapper.getChildren().add(originalRoot);

        // Overlay Pane – children absolutely positioned via layoutX/Y
        Pane overlay = new Pane();

        // Canvas fills overlay for spotlight effect
        Canvas canvas = new Canvas();
        spotlightCanvas = canvas;
        canvas.widthProperty().bind(overlay.widthProperty());
        canvas.heightProperty().bind(overlay.heightProperty());
        canvas.widthProperty().addListener((o, a, b) ->
            Platform.runLater(() -> drawSpotlight(getTarget(currentStep))));
        canvas.heightProperty().addListener((o, a, b) ->
            Platform.runLater(() -> drawSpotlight(getTarget(currentStep))));
        overlay.getChildren().add(canvas);

        // ── Card ──
        StackPane header = new StackPane();
        header.setPrefHeight(76);
        header.setStyle(gradientStyle(0));

        Label stepLbl = new Label("Schritt 1 von " + STEPS.length);
        stepLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: rgba(255,255,255,0.55);"
                       + "-fx-font-weight: 600;");
        StackPane.setAlignment(stepLbl, Pos.CENTER_LEFT);
        StackPane.setMargin(stepLbl, new Insets(0, 0, 0, 24));

        dotsContainer = new HBox(6);
        dotsContainer.setAlignment(Pos.CENTER_RIGHT);
        rebuildDots();
        StackPane.setAlignment(dotsContainer, Pos.CENTER_RIGHT);
        StackPane.setMargin(dotsContainer, new Insets(0, 24, 0, 0));
        header.getChildren().addAll(stepLbl, dotsContainer);

        contentArea = new StackPane();
        contentArea.setMinHeight(240);
        contentArea.getChildren().add(buildContent(0));

        Region div = new Region();
        div.setPrefHeight(1);
        div.setStyle("-fx-background-color: #1e293b;");

        // Footer
        Button backBtn = new Button("← Zurück");
        backBtn.setVisible(false);
        backBtn.setManaged(false);
        backBtn.setStyle(
                "-fx-background-color: transparent; -fx-border-color: #1e293b;"
              + "-fx-border-radius: 12; -fx-text-fill: #64748b; -fx-font-size: 13px;"
              + "-fx-padding: 8 20; -fx-cursor: hand;");

        Button nextBtn = new Button("Los geht's  →");
        nextBtn.setStyle(nextBtnStyle(0));

        Hyperlink skip = new Hyperlink("Überspringen");
        skip.setStyle("-fx-text-fill: #334155; -fx-font-size: 12px;"
                    + "-fx-border-color: transparent; -fx-underline: false;");
        skip.setOnAction(e -> close(overlay, wrapper, scene, originalRoot, userId));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(14, 24, 20, 24));
        footer.setStyle("-fx-background-color: #0d1117; -fx-background-radius: 0 0 24 24;");
        footer.getChildren().addAll(skip, spacer, backBtn, nextBtn);

        tutorialCard = new VBox(0);
        tutorialCard.setMinWidth(520);
        tutorialCard.setMaxWidth(560);
        tutorialCard.setStyle(
                "-fx-background-color: #0d1117; -fx-background-radius: 24;"
              + "-fx-border-color: #1e293b; -fx-border-width: 1; -fx-border-radius: 24;"
              + "-fx-effect: dropshadow(gaussian, #000000cc, 60, 0, 0, 20);");
        tutorialCard.getChildren().addAll(header, contentArea, div, footer);

        // Button actions
        nextBtn.setOnAction(e -> {
            if (currentStep >= STEPS.length - 1) {
                close(overlay, wrapper, scene, originalRoot, userId);
                return;
            }
            currentStep++;
            navigate(currentStep, header, stepLbl, backBtn, nextBtn, overlay);
        });
        backBtn.setOnAction(e -> {
            if (currentStep > 0) {
                currentStep--;
                navigate(currentStep, header, stepLbl, backBtn, nextBtn, overlay);
            }
        });

        overlay.getChildren().add(tutorialCard);
        wrapper.getChildren().add(overlay);

        // Position after first layout pulse
        Platform.runLater(() -> {
            javafx.scene.Node t = getTarget(0);
            drawSpotlight(t);
            positionCard(t, overlay);
        });

        // Fade in
        overlay.setOpacity(0);
        FadeTransition fi = new FadeTransition(Duration.millis(400), overlay);
        fi.setToValue(1);
        fi.play();
    }

    // ── Step navigation ───────────────────────────────────────────────────────

    private static void navigate(int step, StackPane header, Label stepLbl,
                                  Button backBtn, Button nextBtn, Pane overlay) {
        header.setStyle(gradientStyle(step));
        stepLbl.setText("Schritt " + (step + 1) + " von " + STEPS.length);
        rebuildDots();

        backBtn.setVisible(step > 0);
        backBtn.setManaged(step > 0);
        nextBtn.setText(step == STEPS.length - 1 ? "🚀  Jetzt starten!" : "Weiter  →");
        nextBtn.setStyle(nextBtnStyle(step));

        // Animated content swap
        VBox newContent = buildContent(step);
        newContent.setOpacity(0);
        contentArea.getChildren().add(newContent);

        VBox oldContent = (VBox) contentArea.getChildren().get(0);
        FadeTransition out = new FadeTransition(Duration.millis(160), oldContent);
        out.setToValue(0);
        out.setOnFinished(ev -> {
            contentArea.getChildren().remove(oldContent);
            FadeTransition in = new FadeTransition(Duration.millis(200), newContent);
            in.setToValue(1);
            in.play();
        });
        out.play();

        // Spotlight + card reposition after layout
        Platform.runLater(() -> {
            javafx.scene.Node t = getTarget(step);
            drawSpotlight(t);
            positionCard(t, overlay);
        });
    }

    // ── Spotlight ─────────────────────────────────────────────────────────────

    private static void drawSpotlight(javafx.scene.Node target) {
        if (spotlightCanvas == null) return;
        double w = spotlightCanvas.getWidth();
        double h = spotlightCanvas.getHeight();
        if (w <= 0 || h <= 0) return;

        GraphicsContext gc = spotlightCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);
        gc.setFill(Color.color(0, 0, 0, 0.77));
        gc.fillRect(0, 0, w, h);

        if (target == null || !target.isVisible()) return;

        try {
            Bounds tb = target.localToScene(target.getBoundsInLocal());
            if (tb.getWidth() <= 0 || tb.getHeight() <= 0) return;

            double pad = 16;
            double rx = tb.getMinX() - pad;
            double ry = tb.getMinY() - pad;
            double rw = tb.getWidth()  + pad * 2;
            double rh = tb.getHeight() + pad * 2;

            // Punch transparent hole
            gc.clearRect(rx, ry, rw, rh);

            // Highlight ring
            gc.setStroke(Color.web("#6366f1"));
            gc.setLineWidth(2.5);
            gc.strokeRoundRect(rx, ry, rw, rh, 14, 14);

            // Pulsing outer glow (extra ring)
            gc.setStroke(Color.web("#6366f140"));
            gc.setLineWidth(5);
            gc.strokeRoundRect(rx - 5, ry - 5, rw + 10, rh + 10, 18, 18);
        } catch (Exception ignored) {}
    }

    // ── Card positioning ──────────────────────────────────────────────────────

    private static void positionCard(javafx.scene.Node target, Pane overlay) {
        if (tutorialCard == null || overlay.getWidth() <= 0) return;

        double ow = overlay.getWidth();
        double oh = overlay.getHeight();
        double cw = 540;
        double ch = tutorialCard.getHeight() > 50 ? tutorialCard.getHeight() : 440;

        if (target == null || !target.isVisible()) {
            // Center in overlay
            tutorialCard.setLayoutX((ow - cw) / 2);
            tutorialCard.setLayoutY((oh - ch) / 2);
            return;
        }

        try {
            Bounds tb = target.localToScene(target.getBoundsInLocal());
            if (tb.getWidth() <= 0) {
                tutorialCard.setLayoutX((ow - cw) / 2);
                tutorialCard.setLayoutY((oh - ch) / 2);
                return;
            }

            // Horizontal: center card over the target, clamped
            double x = tb.getMinX() + tb.getWidth() / 2.0 - cw / 2.0;
            x = Math.max(10, Math.min(x, ow - cw - 10));

            double gap = 30;
            double y;
            if (tb.getMaxY() + gap + ch <= oh - 10) {
                y = tb.getMaxY() + gap;             // below target
            } else if (tb.getMinY() - gap - ch >= 10) {
                y = tb.getMinY() - gap - ch;        // above target
            } else {
                // No room above or below → center vertically, shift card right
                y = (oh - ch) / 2;
                x = Math.max(10, Math.min(tb.getMaxX() + gap, ow - cw - 10));
            }

            tutorialCard.setLayoutX(x);
            tutorialCard.setLayoutY(y);
        } catch (Exception ignored) {
            tutorialCard.setLayoutX((ow - cw) / 2);
            tutorialCard.setLayoutY((oh - ch) / 2);
        }
    }

    // ── Close ─────────────────────────────────────────────────────────────────

    private static void close(Pane overlay, StackPane wrapper, Scene scene,
                               Parent originalRoot, String userId) {
        markShown(userId);
        FadeTransition out = new FadeTransition(Duration.millis(300), overlay);
        out.setToValue(0);
        out.setOnFinished(e -> {
            // Detach originalRoot from wrapper first (JavaFX requires parent == null
            // before a node can become a scene root)
            wrapper.getChildren().remove(originalRoot);
            scene.setRoot(originalRoot);
        });
        out.play();
    }

    // ── Step content ──────────────────────────────────────────────────────────

    private static VBox buildContent(int step) {
        Object[] s = STEPS[step];
        String emoji   = (String)   s[0];
        String title   = (String)   s[1];
        String desc    = (String)   s[2];
        String[] feats = (String[]) s[3];
        String c1      = (String)   s[4];

        VBox content = new VBox(0);
        content.setAlignment(Pos.TOP_CENTER);
        content.setPadding(new Insets(28, 32, 20, 32));

        // Emoji in colored circle
        StackPane iconWrap = new StackPane();
        Circle bg = new Circle(36);
        bg.setFill(Color.web(c1 + "22"));
        bg.setStroke(Color.web(c1 + "66"));
        bg.setStrokeWidth(1.5);
        Label emojiLbl = new Label(emoji);
        emojiLbl.setStyle("-fx-font-size: 28px;");
        iconWrap.getChildren().addAll(bg, emojiLbl);
        VBox.setMargin(iconWrap, new Insets(0, 0, 16, 0));

        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size: 21px; -fx-font-weight: bold;"
                        + "-fx-text-fill: #f1f5f9; -fx-text-alignment: center;");
        titleLbl.setWrapText(true);
        titleLbl.setAlignment(Pos.CENTER);
        titleLbl.setMaxWidth(460);
        VBox.setMargin(titleLbl, new Insets(0, 0, 10, 0));

        Label descLbl = new Label(desc);
        descLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;"
                       + "-fx-text-alignment: center;");
        descLbl.setWrapText(true);
        descLbl.setAlignment(Pos.CENTER);
        descLbl.setMaxWidth(460);
        VBox.setMargin(descLbl, new Insets(0, 0, feats != null ? 18 : 0, 0));

        content.getChildren().addAll(iconWrap, titleLbl, descLbl);

        if (feats != null) {
            FlowPane chips = new FlowPane(8, 8);
            chips.setAlignment(Pos.CENTER);
            for (String feat : feats) {
                Label chip = new Label(feat);
                chip.setStyle(
                        "-fx-background-color: #161d2e; -fx-background-radius: 20;"
                      + "-fx-border-color: #1e293b; -fx-border-radius: 20;"
                      + "-fx-border-width: 1; -fx-text-fill: #94a3b8;"
                      + "-fx-font-size: 12px; -fx-padding: 6 14;");
                chips.getChildren().add(chip);
            }
            content.getChildren().add(chips);
        }
        return content;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static javafx.scene.Node getTarget(int step) {
        if (stepTargets == null || step >= stepTargets.length) return null;
        return stepTargets[step];
    }

    private static void rebuildDots() {
        dotsContainer.getChildren().clear();
        for (int i = 0; i < STEPS.length; i++) {
            Circle dot = new Circle(i == currentStep ? 5 : 3.5);
            if      (i == currentStep) dot.setFill(Color.WHITE);
            else if (i < currentStep)  dot.setFill(Color.web("#a5b4fc"));
            else                       dot.setFill(Color.web("#334155"));
            dotsContainer.getChildren().add(dot);
        }
    }

    private static String gradientStyle(int step) {
        return "-fx-background-color: linear-gradient(to right, "
             + STEPS[step][4] + ", " + STEPS[step][5] + ");"
             + "-fx-background-radius: 24 24 0 0;";
    }

    private static String nextBtnStyle(int step) {
        return "-fx-background-color: linear-gradient(to right, "
             + STEPS[step][4] + ", " + STEPS[step][5] + ");"
             + "-fx-background-radius: 12; -fx-text-fill: white;"
             + "-fx-font-size: 13px; -fx-font-weight: bold;"
             + "-fx-padding: 9 24; -fx-cursor: hand;";
    }
}
