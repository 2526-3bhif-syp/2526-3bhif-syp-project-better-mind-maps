package htl.leonding.at.controller;
import htl.leonding.at.model.*;
import htl.leonding.at.controller.*;
import htl.leonding.at.service.*;
import htl.leonding.at.repository.*;
import htl.leonding.at.util.*;
import htl.leonding.at.App;


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

    // ── Step definition ───────────────────────────────────────────────────────

    public enum TutorialAction { MAP_CREATED, NODE_ADDED, NODE_STYLED }

    private record Step(
        String emoji, String title, String desc, String[] steps,
        String c1, String c2,
        TutorialAction awaitAction,  // null = info step with Next button
        boolean editorStep           // false = overview, true = editor scene
    ) {}

    private static final Step[] STEPS = {
        new Step("🧠", "Willkommen!",
            "Folge diesem interaktiven Tutorial\num Better Mind Maps kennen zu lernen.",
            null, "#6366f1", "#8b5cf6", null, false),

        new Step("✨", "Erstelle eine Mind Map!",
            "Deine erste Aufgabe:\nErstelle eine Mind Map mit dem Thema 'Arda'.",
            new String[]{"① Klicke auf den  +  Button", "② Name: 'Arda' eingeben", "③ Enter drücken"},
            "#10b981", "#059669", TutorialAction.MAP_CREATED, false),

        new Step("🌿", "Füge einen Child Node hinzu!",
            "Klicke auf den Root Node und drücke Enter\num einen Kind-Node hinzuzufügen.",
            new String[]{"① Root Node anklicken", "② Enter drücken", "③ Text eingeben & Enter"},
            "#14b8a6", "#0891b2", TutorialAction.NODE_ADDED, true),

        new Step("🎨", "Style einen Node!",
            "Rechtsklick auf einen Node und öffne\n'Stil bearbeiten' um ihn zu gestalten.",
            new String[]{"① Node rechtsklicken", "② '🎨 Stil bearbeiten' wählen", "③ Farbe oder Form ändern"},
            "#8b5cf6", "#7c3aed", TutorialAction.NODE_STYLED, true),

        new Step("🚀", "Tutorial abgeschlossen!",
            "Hervorragend! Du kennst jetzt die\nwichtigsten Funktionen. Viel Spaß!",
            null, "#22c55e", "#16a34a", null, true),
    };

    // ── State ─────────────────────────────────────────────────────────────────

    private static int currentStep = -1;
    private static boolean active   = false;
    private static String activeUserId = null;

    // Overview overlay
    private static StackPane ovWrapper;
    private static Parent    ovOriginalRoot;
    private static Scene     ovScene;
    private static Pane      ovOverlay;
    private static Canvas    spotlightCanvas;
    private static javafx.scene.Node[] ovTargets;

    // Editor overlay
    private static StackPane edWrapper;
    private static Parent    edOriginalRoot;
    private static Scene     edScene;
    private static Pane      edOverlay;

    // Current card
    private static VBox      tutorialCard;
    private static HBox      dotsContainer;

    // ── Public API ────────────────────────────────────────────────────────────

    public static boolean isActive() { return active; }

    /** Called from OverviewController.initialize() – shows tutorial for first-time users. */
    public static void showIfNeeded(Pane rootPane, String userId,
                                    javafx.scene.Node... targets) {
        if (isShown(userId)) return;
        active        = true;
        activeUserId  = userId;
        ovTargets     = targets;
        currentStep   = 0;
        showOnOverview(rootPane);
    }

    /** Called from other controllers when the user completes an interactive step. */
    public static void onAction(TutorialAction action) {
        if (!active || currentStep < 0 || currentStep >= STEPS.length) return;
        if (STEPS[currentStep].awaitAction() != action) return;
        advance();
    }

    /** Called from MainController after it loads a map – attaches tutorial to editor. */
    public static void attachToEditorScene(Pane editorRoot) {
        if (!active || currentStep < 0 || currentStep >= STEPS.length) return;
        if (!STEPS[currentStep].editorStep() || edOverlay != null) return;
        showOnEditor(editorRoot);
    }

    // ── Advance logic ─────────────────────────────────────────────────────────

    private static void advance() {
        int next = currentStep + 1;
        if (next >= STEPS.length) { finishTutorial(); return; }
        currentStep = next;

        Step step = STEPS[currentStep];

        if (step.editorStep() && ovOverlay != null) {
            // Crossing from overview → editor; wait for attachToEditorScene()
            closeOverviewOverlay();
        } else if (!step.editorStep() && ovOverlay != null) {
            swapCard(ovOverlay, false);   // staying on overview
        } else if (step.editorStep() && edOverlay != null) {
            swapCard(edOverlay, true);    // staying in editor
        }
    }

    private static void swapCard(Pane overlay, boolean editorStyle) {
        VBox old = tutorialCard;
        buildCard(editorStyle);
        VBox neo = tutorialCard;
        neo.setOpacity(0);
        overlay.getChildren().add(neo);

        FadeTransition out = new FadeTransition(Duration.millis(150), old);
        out.setToValue(0);
        out.setOnFinished(e -> {
            overlay.getChildren().remove(old);
            FadeTransition inT = new FadeTransition(Duration.millis(200), neo);
            inT.setToValue(1); inT.play();
        });
        out.play();

        Platform.runLater(() -> {
            drawSpotlight(getOvTarget());
            positionCard(overlay, editorStyle);
        });
    }

    private static void finishTutorial() {
        markShown(activeUserId);
        active      = false;
        currentStep = -1;
        if (ovOverlay != null) closeOverviewOverlay();
        if (edOverlay != null) closeEditorOverlay();
    }

    // ── Overview overlay ──────────────────────────────────────────────────────

    private static void showOnOverview(Pane rootPane) {
        ovScene       = rootPane.getScene();
        ovOriginalRoot = ovScene.getRoot();
        ovWrapper     = new StackPane();
        ovScene.setRoot(ovWrapper);
        ovWrapper.getChildren().add(ovOriginalRoot);

        ovOverlay = new Pane();
        // Allow clicks to reach the underlying UI (especially through the spotlight hole)
        ovOverlay.setPickOnBounds(false);

        spotlightCanvas = new Canvas();
        // Canvas must NOT block mouse events so the user can click the highlighted element
        spotlightCanvas.setMouseTransparent(true);
        spotlightCanvas.widthProperty().bind(ovOverlay.widthProperty());
        spotlightCanvas.heightProperty().bind(ovOverlay.heightProperty());
        spotlightCanvas.widthProperty().addListener((o, a, b) ->
            Platform.runLater(() -> drawSpotlight(getOvTarget())));
        spotlightCanvas.heightProperty().addListener((o, a, b) ->
            Platform.runLater(() -> drawSpotlight(getOvTarget())));
        ovOverlay.getChildren().add(spotlightCanvas);

        buildCard(false);
        ovOverlay.getChildren().add(tutorialCard);
        ovWrapper.getChildren().add(ovOverlay);

        Platform.runLater(() -> {
            drawSpotlight(getOvTarget());
            positionCard(ovOverlay, false);
        });

        ovOverlay.setOpacity(0);
        FadeTransition ovFi = new FadeTransition(Duration.millis(400), ovOverlay);
        ovFi.setToValue(1); ovFi.play();
    }

    private static void closeOverviewOverlay() {
        if (ovOverlay == null) return;
        Pane ol = ovOverlay; StackPane wr = ovWrapper;
        Scene sc = ovScene;  Parent or = ovOriginalRoot;
        ovOverlay = null; ovWrapper = null; ovScene = null; ovOriginalRoot = null;
        spotlightCanvas = null;

        FadeTransition ft = new FadeTransition(Duration.millis(250), ol);
        ft.setToValue(0);
        ft.setOnFinished(e -> { wr.getChildren().remove(or); if (sc.getRoot() == wr) sc.setRoot(or); });
        ft.play();
    }

    // ── Editor overlay ────────────────────────────────────────────────────────

    private static void showOnEditor(Pane editorRoot) {
        edScene       = editorRoot.getScene();
        edOriginalRoot = edScene.getRoot();
        edWrapper     = new StackPane();
        edScene.setRoot(edWrapper);
        edWrapper.getChildren().add(edOriginalRoot);

        edOverlay = new Pane();
        edOverlay.setPickOnBounds(false); // let clicks pass through to the map

        buildCard(true);
        edOverlay.getChildren().add(tutorialCard);
        edWrapper.getChildren().add(edOverlay);

        Platform.runLater(() -> positionCard(edOverlay, true));

        edOverlay.setOpacity(0);
        FadeTransition edFi = new FadeTransition(Duration.millis(350), edOverlay);
        edFi.setToValue(1); edFi.play();
    }

    private static void closeEditorOverlay() {
        if (edOverlay == null) return;
        Pane ol = edOverlay; StackPane wr = edWrapper;
        Scene sc = edScene;  Parent or = edOriginalRoot;
        edOverlay = null; edWrapper = null; edScene = null; edOriginalRoot = null;

        FadeTransition ft = new FadeTransition(Duration.millis(250), ol);
        ft.setToValue(0);
        ft.setOnFinished(e -> { wr.getChildren().remove(or); if (sc.getRoot() == wr) sc.setRoot(or); });
        ft.play();
    }

    // ── Spotlight ─────────────────────────────────────────────────────────────

    private static void drawSpotlight(javafx.scene.Node target) {
        if (spotlightCanvas == null) return;
        double w = spotlightCanvas.getWidth(), h = spotlightCanvas.getHeight();
        if (w <= 0 || h <= 0) return;

        GraphicsContext gc = spotlightCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);
        gc.setFill(Color.color(0, 0, 0, 0.77));
        gc.fillRect(0, 0, w, h);

        if (target == null || !target.isVisible()) return;
        try {
            Bounds tb = target.localToScene(target.getBoundsInLocal());
            if (tb.getWidth() <= 0) return;
            double pad = 16;
            double rx = tb.getMinX() - pad, ry = tb.getMinY() - pad;
            double rw = tb.getWidth() + pad * 2, rh = tb.getHeight() + pad * 2;
            gc.clearRect(rx, ry, rw, rh);
            gc.setStroke(Color.web("#6366f1")); gc.setLineWidth(2.5);
            gc.strokeRoundRect(rx, ry, rw, rh, 14, 14);
            gc.setStroke(Color.web("#6366f140")); gc.setLineWidth(6);
            gc.strokeRoundRect(rx - 5, ry - 5, rw + 10, rh + 10, 18, 18);
        } catch (Exception ignored) {}
    }

    private static javafx.scene.Node getOvTarget() {
        if (ovTargets == null || currentStep >= ovTargets.length) return null;
        return ovTargets[currentStep];
    }

    // ── Card positioning ──────────────────────────────────────────────────────

    private static void positionCard(Pane overlay, boolean editorStyle) {
        if (tutorialCard == null || overlay.getWidth() <= 0) return;
        double ow = overlay.getWidth(), oh = overlay.getHeight();
        double cw = editorStyle ? 370 : 540;
        double ch = tutorialCard.getHeight() > 50 ? tutorialCard.getHeight()
                                                   : (editorStyle ? 280 : 440);
        if (editorStyle) {
            tutorialCard.setLayoutX(ow - cw - 24);
            tutorialCard.setLayoutY(oh - ch - 24);
            return;
        }

        javafx.scene.Node target = getOvTarget();
        if (target == null || !target.isVisible()) {
            tutorialCard.setLayoutX((ow - cw) / 2);
            tutorialCard.setLayoutY((oh - ch) / 2);
            return;
        }
        try {
            Bounds tb = target.localToScene(target.getBoundsInLocal());
            double x = Math.max(10, Math.min(
                tb.getMinX() + tb.getWidth() / 2 - cw / 2, ow - cw - 10));
            double gap = 30, y;
            if      (tb.getMaxY() + gap + ch <= oh - 10) y = tb.getMaxY() + gap;
            else if (tb.getMinY() - gap - ch >= 10)      y = tb.getMinY() - gap - ch;
            else { y = (oh - ch) / 2;
                   x = Math.max(10, Math.min(tb.getMaxX() + gap, ow - cw - 10)); }
            tutorialCard.setLayoutX(x);
            tutorialCard.setLayoutY(y);
        } catch (Exception ignored) {
            tutorialCard.setLayoutX((ow - cw) / 2);
            tutorialCard.setLayoutY((oh - ch) / 2);
        }
    }

    // ── Card building ─────────────────────────────────────────────────────────

    private static void buildCard(boolean ed) {
        Step step = STEPS[currentStep];
        int r = ed ? 20 : 24;

        // Header
        StackPane header = new StackPane();
        header.setPrefHeight(ed ? 54 : 76);
        header.setStyle("-fx-background-color: linear-gradient(to right,"
            + step.c1() + "," + step.c2() + "); -fx-background-radius: " + r + " " + r + " 0 0;");

        Label stepLbl = new Label("Aufgabe " + (currentStep + 1) + " / " + STEPS.length);
        stepLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: rgba(255,255,255,0.6);"
                       + "-fx-font-weight: 600;");
        StackPane.setAlignment(stepLbl, Pos.CENTER_LEFT);
        StackPane.setMargin(stepLbl, new Insets(0, 0, 0, 20));

        dotsContainer = new HBox(6);
        dotsContainer.setAlignment(Pos.CENTER_RIGHT);
        rebuildDots();
        StackPane.setAlignment(dotsContainer, Pos.CENTER_RIGHT);
        StackPane.setMargin(dotsContainer, new Insets(0, 20, 0, 0));
        header.getChildren().addAll(stepLbl, dotsContainer);

        // Content
        StackPane content = new StackPane();
        content.setMinHeight(ed ? 140 : 240);
        content.getChildren().add(buildContent(ed));

        // Divider
        Region div = new Region();
        div.setPrefHeight(1);
        div.setStyle("-fx-background-color: #1e293b;");

        // Footer
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(12, 20, 16, 20));
        footer.setStyle("-fx-background-color: #0d1117; -fx-background-radius: 0 0 " + r + " " + r + ";");

        Hyperlink skip = new Hyperlink("Überspringen");
        skip.setStyle("-fx-text-fill: #334155; -fx-font-size: 11px;"
                    + "-fx-border-color: transparent; -fx-underline: false;");
        skip.setOnAction(e -> finishTutorial());
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        if (step.awaitAction() != null) {
            // Interactive step: pulsing "waiting" label
            Label waitLbl = new Label("⏳  Warte auf deine Aktion...");
            waitLbl.setStyle("-fx-text-fill: " + step.c1()
                           + "; -fx-font-size: 12px; -fx-font-weight: 600;");
            FadeTransition pulse = new FadeTransition(Duration.millis(900), waitLbl);
            pulse.setFromValue(1.0); pulse.setToValue(0.3);
            pulse.setCycleCount(-1); pulse.setAutoReverse(true);
            pulse.play();
            footer.getChildren().addAll(skip, spacer, waitLbl);
        } else {
            String label = currentStep == 0              ? "Los geht's  →"
                         : currentStep == STEPS.length-1 ? "🎉  Fertig!"
                         : "Weiter  →";
            Button btn = new Button(label);
            btn.setStyle("-fx-background-color: linear-gradient(to right,"
                + step.c1() + "," + step.c2() + "); -fx-background-radius: 12;"
                + "-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;"
                + "-fx-padding: 9 24; -fx-cursor: hand;");
            btn.setOnAction(e -> advance());
            footer.getChildren().addAll(skip, spacer, btn);
        }

        // Assemble
        tutorialCard = new VBox(0);
        tutorialCard.setMinWidth(ed ? 320 : 520);
        tutorialCard.setMaxWidth(ed ? 380 : 560);
        tutorialCard.setStyle("-fx-background-color: #0d1117; -fx-background-radius: " + r + ";"
            + "-fx-border-color: #1e293b; -fx-border-width: 1; -fx-border-radius: " + r + ";"
            + "-fx-effect: dropshadow(gaussian, #000000cc, 40, 0, 0, 10);");
        tutorialCard.getChildren().addAll(header, content, div, footer);
    }

    private static VBox buildContent(boolean ed) {
        Step step = STEPS[currentStep];
        VBox box = new VBox(0);
        box.setAlignment(Pos.TOP_CENTER);
        box.setPadding(new Insets(ed ? 18 : 28, 28, ed ? 14 : 20, 28));

        // Emoji in gradient circle
        StackPane iconWrap = new StackPane();
        double cr = ed ? 26 : 36;
        Circle bg = new Circle(cr);
        bg.setFill(Color.web(step.c1() + "22"));
        bg.setStroke(Color.web(step.c1() + "66"));
        bg.setStrokeWidth(1.5);
        Label emo = new Label(step.emoji());
        emo.setStyle("-fx-font-size: " + (ed ? 22 : 28) + "px;");
        iconWrap.getChildren().addAll(bg, emo);
        VBox.setMargin(iconWrap, new Insets(0, 0, ed ? 12 : 16, 0));

        Label title = new Label(step.title());
        title.setStyle("-fx-font-size: " + (ed ? 15 : 21) + "px; -fx-font-weight: bold;"
                     + "-fx-text-fill: #f1f5f9; -fx-text-alignment: center;");
        title.setWrapText(true); title.setAlignment(Pos.CENTER);
        title.setMaxWidth(ed ? 320 : 460);
        VBox.setMargin(title, new Insets(0, 0, 8, 0));

        Label desc = new Label(step.desc());
        desc.setStyle("-fx-font-size: " + (ed ? 11 : 13) + "px;"
                    + "-fx-text-fill: #64748b; -fx-text-alignment: center;");
        desc.setWrapText(true); desc.setAlignment(Pos.CENTER);
        desc.setMaxWidth(ed ? 320 : 460);
        VBox.setMargin(desc, new Insets(0, 0, step.steps() != null ? (ed ? 14 : 18) : 0, 0));

        box.getChildren().addAll(iconWrap, title, desc);

        if (step.steps() != null) {
            VBox list = new VBox(ed ? 6 : 8);
            list.setAlignment(Pos.CENTER_LEFT);
            for (String s : step.steps()) {
                Label chip = new Label(s);
                chip.setStyle("-fx-background-color: #161d2e; -fx-background-radius: 8;"
                    + "-fx-border-color: #1e293b; -fx-border-radius: 8; -fx-border-width: 1;"
                    + "-fx-text-fill: #94a3b8; -fx-font-size: " + (ed ? 11 : 12) + "px;"
                    + "-fx-padding: " + (ed ? "5 12" : "6 16") + ";");
                list.getChildren().add(chip);
            }
            box.getChildren().add(list);
        }
        return box;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

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
}
