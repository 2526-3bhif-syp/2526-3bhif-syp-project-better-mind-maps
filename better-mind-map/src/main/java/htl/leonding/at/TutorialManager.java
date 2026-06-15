package htl.leonding.at;

import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.sql.*;

public class TutorialManager {

    // {emoji, title, description, String[] features, color1, color2}
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
         new String[]{"Enter → Child Node", "F2 → Text bearbeiten", "Del → Löschen", "Ziehen → Verschieben", "Pfeiltasten → Navigation"},
         "#14b8a6", "#0891b2"},

        {"🎨", "Node Styling",
         "Gestalte jeden Node einzigartig.\nRechtsklick auf einen Node → 'Stil bearbeiten'",
         new String[]{"Voller Farbverlauf", "8 Formen (Hexagon, Stern...)", "Emojis & Icons", "HOT · DONE · IDEA · WARN Badges"},
         "#8b5cf6", "#7c3aed"},

        {"🤖", "KI-Assistent",
         "Lass die KI deine Mindmap automatisch aufbauen\nund mit Farben & Formen gestalten.",
         new String[]{"Claude API Key (sk-ant-...)", "Gemini API Key", "Automatische Struktur", "KI wählt Farben & Formen"},
         "#f59e0b", "#d97706"},

        {"⌨️", "Tastenkürzel",
         "Arbeite schneller mit diesen Shortcuts.",
         new String[]{"Enter → Child Node", "F2 → Bearbeiten", "Del → Löschen", "Pfeiltasten → Navigation", "F → Vollbild", "Strg+Z → Rückgängig"},
         "#ec4899", "#db2777"},

        {"🚀", "Du bist bereit!",
         "Erstelle jetzt deine erste Mind Map\nund entfalte deine Kreativität!",
         null, "#22c55e", "#16a34a"}
    };

    private static int currentStep;
    private static StackPane contentArea;
    private static HBox dotsContainer;

    public static void showIfNeeded(Pane rootPane, String userId) {
        if (isShown(userId)) return;
        show(rootPane, userId);
    }

    // ── DB helpers ────────────────────────────────────────────────────────────

    private static boolean isShown(String userId) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT tutorial_shown FROM users WHERE id = ?")) {
            stmt.setString(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt("tutorial_shown") == 1;
            }
        } catch (Exception ignored) {}
        return false;
    }

    private static void markShown(String userId) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE users SET tutorial_shown = 1 WHERE id = ?")) {
            stmt.setString(1, userId);
            stmt.executeUpdate();
        } catch (Exception ignored) {}
    }

    // ── Build overlay ─────────────────────────────────────────────────────────

    private static void show(Pane rootPane, String userId) {
        currentStep = 0;

        // Semi-transparent backdrop
        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: #000000c4;");
        overlay.setManaged(false);
        overlay.setLayoutX(0);
        overlay.setLayoutY(0);
        overlay.prefWidthProperty().bind(rootPane.widthProperty());
        overlay.prefHeightProperty().bind(rootPane.heightProperty());

        // Card
        VBox card = new VBox(0);
        card.setMaxWidth(560);
        card.setMinWidth(520);
        card.setStyle(
                "-fx-background-color: #0d1117;" +
                "-fx-background-radius: 24;" +
                "-fx-border-color: #1e293b;" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 24;" +
                "-fx-effect: dropshadow(gaussian, #000000cc, 60, 0, 0, 20);");

        // ── Header ──
        StackPane header = new StackPane();
        header.setPrefHeight(76);
        header.setStyle(gradientStyle(0));

        Label stepLbl = new Label("Schritt 1 von " + STEPS.length);
        stepLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: rgba(255,255,255,0.55); -fx-font-weight: 600;");
        StackPane.setAlignment(stepLbl, Pos.CENTER_LEFT);
        StackPane.setMargin(stepLbl, new Insets(0, 0, 0, 24));

        dotsContainer = new HBox(6);
        dotsContainer.setAlignment(Pos.CENTER_RIGHT);
        rebuildDots();
        StackPane.setAlignment(dotsContainer, Pos.CENTER_RIGHT);
        StackPane.setMargin(dotsContainer, new Insets(0, 24, 0, 0));

        header.getChildren().addAll(stepLbl, dotsContainer);

        // ── Content area ──
        contentArea = new StackPane();
        contentArea.setMinHeight(240);
        contentArea.getChildren().add(buildContent(0));

        // ── Divider ──
        Region div = new Region();
        div.setPrefHeight(1);
        div.setStyle("-fx-background-color: #1e293b;");

        // ── Footer ──
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(14, 24, 20, 24));
        footer.setStyle("-fx-background-color: #0d1117; -fx-background-radius: 0 0 24 24;");

        Hyperlink skip = new Hyperlink("Überspringen");
        skip.setStyle("-fx-text-fill: #334155; -fx-font-size: 12px; -fx-border-color: transparent; -fx-underline: false;");
        skip.setOnAction(e -> close(overlay, rootPane, userId));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button backBtn = new Button("← Zurück");
        backBtn.setVisible(false);
        backBtn.setManaged(false);
        backBtn.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-border-color: #1e293b;" +
                "-fx-border-radius: 12;" +
                "-fx-text-fill: #64748b;" +
                "-fx-font-size: 13px;" +
                "-fx-padding: 8 20;" +
                "-fx-cursor: hand;");

        Button nextBtn = new Button("Los geht's  →");
        nextBtn.setStyle(nextBtnStyle(0));

        nextBtn.setOnAction(e -> {
            if (currentStep >= STEPS.length - 1) {
                close(overlay, rootPane, userId);
                return;
            }
            currentStep++;
            navigate(currentStep, header, stepLbl, backBtn, nextBtn);
        });

        backBtn.setOnAction(e -> {
            if (currentStep > 0) {
                currentStep--;
                navigate(currentStep, header, stepLbl, backBtn, nextBtn);
            }
        });

        footer.getChildren().addAll(skip, spacer, backBtn, nextBtn);

        card.getChildren().addAll(header, contentArea, div, footer);
        overlay.getChildren().add(card);
        rootPane.getChildren().add(overlay);

        // Fade in
        overlay.setOpacity(0);
        FadeTransition fi = new FadeTransition(Duration.millis(400), overlay);
        fi.setToValue(1);
        fi.play();
    }

    // ── Step content ──────────────────────────────────────────────────────────

    private static VBox buildContent(int step) {
        Object[] s = STEPS[step];
        String emoji    = (String)   s[0];
        String title    = (String)   s[1];
        String desc     = (String)   s[2];
        String[] feats  = (String[]) s[3];
        String c1       = (String)   s[4];

        VBox content = new VBox(0);
        content.setAlignment(Pos.TOP_CENTER);
        content.setPadding(new Insets(28, 32, 20, 32));

        // Emoji in gradient circle
        StackPane iconWrap = new StackPane();
        Circle bg = new Circle(36);
        bg.setFill(Color.web(c1 + "22"));
        bg.setStroke(Color.web(c1 + "66"));
        bg.setStrokeWidth(1.5);
        Label emojiLbl = new Label(emoji);
        emojiLbl.setStyle("-fx-font-size: 28px;");
        iconWrap.getChildren().addAll(bg, emojiLbl);
        VBox.setMargin(iconWrap, new Insets(0, 0, 16, 0));

        // Title
        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size: 21px; -fx-font-weight: bold; -fx-text-fill: #f1f5f9; -fx-text-alignment: center;");
        titleLbl.setWrapText(true);
        titleLbl.setAlignment(Pos.CENTER);
        titleLbl.setMaxWidth(460);
        VBox.setMargin(titleLbl, new Insets(0, 0, 10, 0));

        // Description
        Label descLbl = new Label(desc);
        descLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b; -fx-text-alignment: center;");
        descLbl.setWrapText(true);
        descLbl.setAlignment(Pos.CENTER);
        descLbl.setMaxWidth(460);
        VBox.setMargin(descLbl, new Insets(0, 0, feats != null ? 18 : 0, 0));

        content.getChildren().addAll(iconWrap, titleLbl, descLbl);

        // Feature chips
        if (feats != null) {
            FlowPane chips = new FlowPane(8, 8);
            chips.setAlignment(Pos.CENTER);
            for (String feat : feats) {
                Label chip = new Label(feat);
                chip.setStyle(
                        "-fx-background-color: #161d2e;" +
                        "-fx-background-radius: 20;" +
                        "-fx-border-color: #1e293b;" +
                        "-fx-border-radius: 20;" +
                        "-fx-border-width: 1;" +
                        "-fx-text-fill: #94a3b8;" +
                        "-fx-font-size: 12px;" +
                        "-fx-padding: 6 14;");
                chips.getChildren().add(chip);
            }
            content.getChildren().add(chips);
        }

        return content;
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    private static void navigate(int step, StackPane header, Label stepLbl,
                                 Button backBtn, Button nextBtn) {
        header.setStyle(gradientStyle(step));
        stepLbl.setText("Schritt " + (step + 1) + " von " + STEPS.length);
        rebuildDots();

        backBtn.setVisible(step > 0);
        backBtn.setManaged(step > 0);

        nextBtn.setText(step == STEPS.length - 1 ? "🚀  Jetzt starten!" : "Weiter  →");
        nextBtn.setStyle(nextBtnStyle(step));

        // Animate: fade old out → fade new in
        VBox newContent = buildContent(step);
        newContent.setOpacity(0);
        contentArea.getChildren().add(newContent);

        VBox oldContent = (VBox) contentArea.getChildren().get(0);
        FadeTransition out = new FadeTransition(Duration.millis(160), oldContent);
        out.setToValue(0);
        out.setOnFinished(e -> {
            contentArea.getChildren().remove(oldContent);
            FadeTransition in = new FadeTransition(Duration.millis(200), newContent);
            in.setToValue(1);
            in.play();
        });
        out.play();
    }

    private static void close(StackPane overlay, Pane rootPane, String userId) {
        markShown(userId);
        FadeTransition out = new FadeTransition(Duration.millis(300), overlay);
        out.setToValue(0);
        out.setOnFinished(e -> rootPane.getChildren().remove(overlay));
        out.play();
    }

    // ── Style helpers ─────────────────────────────────────────────────────────

    private static String gradientStyle(int step) {
        String c1 = (String) STEPS[step][4];
        String c2 = (String) STEPS[step][5];
        return "-fx-background-color: linear-gradient(to right, " + c1 + ", " + c2 + ");" +
               "-fx-background-radius: 24 24 0 0;";
    }

    private static String nextBtnStyle(int step) {
        String c1 = (String) STEPS[step][4];
        String c2 = (String) STEPS[step][5];
        return "-fx-background-color: linear-gradient(to right, " + c1 + ", " + c2 + ");" +
               "-fx-background-radius: 12;" +
               "-fx-text-fill: white;" +
               "-fx-font-size: 13px;" +
               "-fx-font-weight: bold;" +
               "-fx-padding: 9 24;" +
               "-fx-cursor: hand;";
    }

    private static void rebuildDots() {
        dotsContainer.getChildren().clear();
        for (int i = 0; i < STEPS.length; i++) {
            Circle dot = new Circle(i == currentStep ? 5 : 3.5);
            if (i == currentStep)     dot.setFill(Color.WHITE);
            else if (i < currentStep) dot.setFill(Color.web("#a5b4fc"));
            else                      dot.setFill(Color.web("#334155"));
            dotsContainer.getChildren().add(dot);
        }
    }
}
