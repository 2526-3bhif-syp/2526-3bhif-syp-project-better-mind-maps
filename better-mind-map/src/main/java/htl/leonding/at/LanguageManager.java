package htl.leonding.at;

import java.util.HashMap;
import java.util.Map;

public class LanguageManager {

    public enum Language { ENGLISH, DEUTSCH }

    private static Language current = Language.ENGLISH;

    private static final Map<String, String> EN = new HashMap<>();
    private static final Map<String, String> DE = new HashMap<>();

    static {
        // ── Buttons ──────────────────────────────────────────────────────────
        p("btn.back",            "← Overview",                           "← Übersicht");
        p("btn.newmap",          "+ New Map",                            "+ Neue Map");
        p("btn.aigenerate",      "✨ AI Generate",                       "✨ KI Generieren");
        p("btn.present",         "▶  Present",                           "▶  Präsentieren");
        p("btn.logout",          "Logout",                               "Abmelden");
        p("btn.language",        "🌐  Language",                         "🌐  Sprache");
        p("btn.sync",            "Sync",                                 "Sync");
        p("btn.deletemap",       "🗑  Delete Map",                       "🗑  Map löschen");
        p("btn.exitpresentation","Exit Presentation",                    "Präsentation beenden");

        // ── Overview ─────────────────────────────────────────────────────────
        p("section.label",       "MY MIND MAPS",                        "MEINE MIND MAPS");
        p("search.prompt",       "Search mind maps...",                  "Mind Maps suchen...");
        p("card.open",           "Open  →",                             "Öffnen  →");
        p("card.create.label",   "New Mind Map",                        "Neue Mind Map");
        p("card.create.sub",     "Click to create",                     "Klicken zum Erstellen");

        // ── Sidebar / Statusbar ───────────────────────────────────────────────
        p("sidebar.structure",   "STRUCTURE",                           "STRUKTUR");
        p("sb.addchild",         "Add child",                           "Kind hinzufügen");
        p("sb.edit",             "Edit",                                "Bearbeiten");
        p("sb.delete",           "Delete",                              "Löschen");
        p("sb.navigate",         "Navigate",                            "Navigieren");
        p("sb.cycle",            "Cycle",                               "Wechseln");
        p("sb.zoom",             "Zoom",                                "Zoom");
        p("status.pending",      "● Pending sync",                      "● Sync ausstehend");

        // ── Context menu ──────────────────────────────────────────────────────
        p("menu.addchild",       "Add Child Node  [Enter]",             "Kind-Knoten hinzufügen  [Enter]");
        p("menu.edittext",       "Edit Text  [F2]",                     "Text bearbeiten  [F2]");
        p("menu.deletenode",     "Delete Node  [Del]",                  "Knoten löschen  [Del]");
        p("menu.editdesc",       "📝  Edit Description",                "📝  Beschreibung bearbeiten");
        p("menu.editstyle",      "🎨  Edit Style",                      "🎨  Stil bearbeiten");
        p("menu.duplicate",      "📋  Duplicate",                       "📋  Duplizieren");

        // ── Dialogs ───────────────────────────────────────────────────────────
        p("dlg.newmap.title",    "New Mind Map",                        "Neue Mind Map");
        p("dlg.newmap.header",   "Create a new Mind Map",               "Neue Mind Map erstellen");
        p("dlg.newmap.content",  "Map name:",                           "Name der Mind Map:");

        p("dlg.createfirst",     "Create a Mind Map first",             "Zuerst eine Mind Map erstellen");

        p("dlg.editnode.title",  "Edit Node",                           "Knoten bearbeiten");
        p("dlg.editnode.header", "Edit text",                           "Text ändern");
        p("dlg.editnode.content","New text:",                           "Neuer Text:");

        p("dlg.addchild.title",  "Add Node",                            "Knoten hinzufügen");
        p("dlg.addchild.header", "Child of \"%s\"",                     "Kind von \"%s\"");

        p("dlg.rename.title",    "Rename Mind Map",                     "Mind Map umbenennen");
        p("dlg.rename.header",   "New name for \"%s\"",                 "Neuer Name für \"%s\"");
        p("dlg.rename.content",  "Name:",                               "Name:");

        p("dlg.delete.title",    "Delete Mind Map",                     "Mind Map löschen");
        p("dlg.delete.header",   "Delete \"%s\"?",                      "\"%s\" löschen?");
        p("dlg.delete.content",  "This action cannot be undone.",       "Diese Aktion kann nicht rückgängig gemacht werden.");

        p("dlg.desc.title",      "Description",                         "Beschreibung");
        p("dlg.desc.header",     "Description for: %s",                 "Beschreibung für: %s");
        p("dlg.desc.prompt",     "Notes, details or a description for this node...",
                                 "Notizen, Details oder eine Beschreibung für diesen Knoten...");
        p("dlg.desc.save",       "Save",                                "Speichern");
        p("dlg.desc.cancel",     "Cancel",                              "Abbrechen");

        // ── HUD ───────────────────────────────────────────────────────────────
        p("hud.noselection",     "Select a node to edit its style",     "Knoten auswählen um Stil zu bearbeiten");
        p("hud.fullEditor",      "🎨  Full Editor",                     "🎨  Vollständig bearbeiten");
        p("hud.canvastheme",     "Canvas Theme:",                       "Canvas-Design:");
        p("hud.nodeStyling",     "NODE STYLING",                        "KNOTEN-STIL");
        p("hud.presentationCtrl","PRESENTATION CONTROLS",               "PRÄSENTATIONSSTEUERUNG");

        // ── Description popup ─────────────────────────────────────────────────
        p("desc.empty",
          "No description yet.\n\nRight-click → \"Edit Description\" to add one.",
          "Noch keine Beschreibung vorhanden.\n\nRechtsklick → \"Beschreibung bearbeiten\" um eine hinzuzufügen.");
        p("desc.edit",           "✏  Edit",                            "✏  Bearbeiten");
        p("desc.close",          "Close",                              "Schließen");

        // ── AI Chat ───────────────────────────────────────────────────────────
        p("ai.back",             "← Back",                             "← Zurück");
        p("ai.title",            "AI Mindmap Assistant",               "KI-Mindmap-Assistent");
        p("ai.nomapLoaded",      "No mind map loaded",                 "Keine Mind Map geladen");
        p("ai.ready",            "● Ready",                            "● Bereit");
        p("ai.thinking.status",  "● Thinking...",                      "● Denkt nach...");
        p("ai.ready.status",     "● Ready",                            "● Bereit");
        p("ai.subtitle",         "Ask me a question or choose a suggestion",
                                 "Stelle mir eine Frage oder wähle einen Vorschlag aus");
        p("ai.input.prompt",     "Write a message...",                 "Schreibe eine Nachricht...");
        p("ai.thinking.bubble",  "Thinking...",                        "Denkt nach...");
        p("ai.nokey",
          "No API key provided. Type !apikey:YOUR_KEY to set one.",
          "Kein API Key vorhanden. Tippe !apikey:DEIN_KEY um einen zu setzen.");
        p("ai.keyset",           "API Key set (%s). Let's go!",        "API Key gesetzt (%s). Los geht's!");
        p("ai.apikey.title",     "API Key required",                   "API Key benötigt");
        p("ai.apikey.header",    "Enter your Gemini or Claude API Key","Gemini oder Claude API Key eingeben");
        p("ai.apikey.content",   "API Key (Gemini or sk-ant-... for Claude):","API Key (Gemini oder sk-ant-... für Claude):");
        p("ai.updated",          "Mind map updated! Created %d categories for \"%s\".",
                                 "Mindmap aktualisiert! %d Kategorien zu \"%s\" erstellt.");
        p("ai.error",            "Could not process the response. Please try again.",
                                 "Die Antwort konnte nicht verarbeitet werden. Bitte versuche es erneut.");
        p("ai.connectionError",  "Connection error: %s",               "Verbindungsfehler: %s");
        p("ai.claudeError",      "Error (%d): %s",                     "Fehler (%d): %s");
        p("ai.geminiError",      "Gemini error (%d): %s",              "Gemini-Fehler (%d): %s");

        p("ai.s1", "💡  Explain the current topic in more detail",    "💡  Erkläre das aktuelle Thema genauer");
        p("ai.s2", "✨  Expand the mind map with new ideas",           "✨  Erweitere die Mindmap mit neuen Ideen");
        p("ai.s3", "🔄  Simplify the structure",                       "🔄  Vereinfache die Struktur");
        p("ai.s4", "📚  Add more details",                             "📚  Füge mehr Details hinzu");
        p("ai.s5", "🎯  Create a short summary",                       "🎯  Erstelle eine kurze Zusammenfassung");
        p("ai.s6", "💬  What else could I add?",                       "💬  Was könnte ich noch ergänzen?");

        p("ai.r1", "✨ Expand",        "✨ Erweitern");
        p("ai.r2", "🔄 Restructure",   "🔄 Neu strukturieren");
        p("ai.r3", "📊 Add Examples",  "📊 Beispiele hinzufügen");
        p("ai.r4", "❓ Explain",       "❓ Erklären");

        // ── Sync status ───────────────────────────────────────────────────────
        p("sync.pending",  "Cloud Sync: PENDING",       "Cloud Sync: Ausstehend");
        p("sync.syncing",  "Cloud Sync: SYNCING...",    "Cloud Sync: Synchronisiert...");
        p("sync.synced",   "Cloud Sync: SYNCED",        "Cloud Sync: Synchronisiert");
        p("sync.failed",   "Cloud Sync: FAILED",        "Cloud Sync: Fehlgeschlagen");

        p("sync.label.PENDING", "PENDING",   "Ausstehend");
        p("sync.label.SYNCED",  "SYNCED",    "Synchronisiert");
        p("sync.label.FAILED",  "FAILED",    "Fehlgeschlagen");

        // ── Canvas themes ─────────────────────────────────────────────────────
        p("theme.light",  "Light",   "Hell");
        p("theme.dark",   "Dark",    "Dunkel");
        p("theme.sepia",  "Sepia",   "Sepia");
        p("theme.ocean",  "Ocean",   "Ozean");

        // ── Language dialog ───────────────────────────────────────────────────
        p("lang.title",    "Language / Sprache",  "Sprache / Language");
        p("lang.header",   "Select your language","Sprache auswählen");
        p("lang.content",  "Language:",           "Sprache:");
    }

    private static void p(String key, String en, String de) {
        EN.put(key, en);
        DE.put(key, de);
    }

    public static String get(String key) {
        return (current == Language.ENGLISH ? EN : DE).getOrDefault(key, key);
    }

    /** Like get() but runs String.format(value, args) on the result. */
    public static String getf(String key, Object... args) {
        return String.format(get(key), args);
    }

    public static void setLanguage(Language lang) { current = lang; }
    public static Language getLanguage() { return current; }
    public static boolean isEnglish() { return current == Language.ENGLISH; }
}
