# Feature-Übersicht — Branch `feature/modern-mindmap-styling`

Alle Änderungen wurden in **3 Commits** auf diesem Branch eingecheckt.

---

## 1 · Modernes Dark-UI, Node-Formen & Auto-Resize (`ab4e533`)

### Design-System (`styles.css`)
- Komplett neues dunkles Farbschema: Hintergrund `#0d1117`, Akzent-Farbe Indigo `#6366f1`
- Alle Buttons neu gestylt mit Glow-Effekten (Indigo, Lila, Cyan, Rot)
- Toolbar, Sidebar, TabPane, Statusbar, Kontextmenü, Scrollbars — einheitlich dunkel
- TabPane: aktiver Tab mit Indigo-Unterstrich

### Layout (`main-view.fxml`)
- Separator zwischen Zurück-Button und Aktions-Buttons
- Bessere Button-Labels: `+ New Map`, `✨ AI Generate`, `▶ Present`
- Sync-Label mit `●` Status-Indikator

### Node-Rendering (`MainController.java`)
- **Root-Node**: Indigo-Verlauf `#6366f1 → #4338ca` statt altem Blau
- **Selektion**: Indigo-Outline mit DropShadow-Glow statt Rot
- **Canvas-Hintergründe**: Dunklere, modernere Farben für alle 4 Themes (LIGHT/DARK/SEPIA/OCEAN)
- Verbindungslinien: dünner, subtiler, mit Opacity

### Node-Formen (`Node.java`, `DatabaseManager.java`, `MindMapRepository.java`, `MainController.java`)
- 4 wählbare Formen im HUD (rechts oben):
  - `▭` **Rounded Rect** — Standard
  - `⬬` **Pill** — Kapsel (vollständig gerundet)
  - `⬭` **Ellipse** — Oval
  - `◇` **Diamond** — Raute
- Aktive Form im HUD mit Indigo-Border hervorgehoben
- Form wird pro Node in SQLite gespeichert (Spalte `shape`)

### Auto-Resize
- Node-**Breite** wächst proportional zur Textlänge (max. 210px)
- Node-**Höhe** wächst mit Anzahl der Zeilenumbrüche
- Ellipse/Diamond bekommen automatisch mehr Platz (kleinerer nutzbarer Innenbereich)

---

## 2 · Gestylte Dialoge, Node-Beschreibung & professionelles Popup (`8ca611e`)

### Dialoge (`MainController.java`, `styles.css`)
- Alle `TextInputDialog`-Fenster (Node hinzufügen, bearbeiten, AI-Prompt, API-Key) erhalten automatisch das dunkle CSS-Theme
- TextField mit Indigo-Focus-Glow, dunkler Hintergrund, passende Buttons
- Helper-Methode `applyTheme(dialog)` — einmalig aufrufen, alles wird gestylt

### Node-Beschreibung (`Node.java`, `DatabaseManager.java`, `MindMapRepository.java`)
- Neues Feld `description` (String) pro Node
- Automatische SQLite-Migration: Spalte `description` wird beim ersten Start hinzugefügt
- Wird beim Speichern und Laden mitübertragen

### Rechtsklick-Kontextmenü (`MainController.java`)
- Neuer Eintrag: **📝 Beschreibung bearbeiten**
- Menü-Struktur: `Add Child | Edit Text | --- | Edit Description | --- | Delete`

### Doppelklick-Popup (`MainController.java`, `styles.css`)
- Doppelklick auf jeden Node öffnet ein **floating Popup-Fenster** (transparente Stage, kein OS-Rahmen)
- **Header**: Node-Icon + Titel + ✕-Button; per Drag verschiebbar
- **Body**: Scrollbarer Beschreibungstext; leer → Kursiver Hinweis
- **Footer**: `✏ Bearbeiten`-Button + `Schließen`
- Zentriert sich automatisch auf dem Hauptfenster beim Öffnen
- Bearbeiten-Dialog: große Textarea (10 Zeilen) mit dunklem Styling und Indigo-Focus-Glow

---

## 3 · Neue Startseite mit Grid, Suche & Thumbnails (`297fce1`)

### Hero-Banner (`overview-view.fxml`, `styles.css`)
- Gradient-Header `#0d1117 → #161b22`
- Titel `🧠 Better Mind Maps` + Live-Stats (Anzahl Maps & Nodes)
- Username mit Indigo-Badge rechts
- Logout + `+ New Mind Map`-Button

### Live-Suche
- Suchfeld filtert Karten **in Echtzeit** während der Eingabe
- Dark-styled TextField mit Indigo-Focus-Glow

### Karten-Grid (`OverviewController.java`, `styles.css`)
- `FlowPane` statt einfacher Liste — passt sich der Fensterbreite an
- Jede Karte (280px breit) enthält:
  - **Canvas-Thumbnail**: echtes gerendertes Mini-Bild der Mindmap (Nodes + Linien, Theme-Farben)
  - **Hover-Overlay**: `Open →` Hint + Indigo-Glow-Border
  - **Theme-Badge**: `☀ Light`, `🌙 Dark`, `📜 Sepia`, `🌊 Ocean`
  - **Node-Zähler** + **Sync-Status** (grün/orange/rot je nach Status)
  - **🗑 Delete-Button** — isoliert vom Karten-Klick (kein versehentliches Öffnen)
- `+ New Mind Map`-Karte mit gestrichelter Indigo-Border am Ende des Grids

---

## Geänderte Dateien (gesamt)

| Datei | Was geändert |
|---|---|
| `styles.css` | Komplett neu (Design-System, Dialoge, Popup, Overview) |
| `main-view.fxml` | Toolbar, Sidebar, Statusbar-Layout |
| `overview-view.fxml` | Komplett neu (Hero, Suche, FlowPane) |
| `MainController.java` | Node-Formen, Auto-Resize, Dialoge, Doppelklick-Popup |
| `OverviewController.java` | Komplett neu (Grid, Thumbnail-Renderer, Suche) |
| `Node.java` | Felder: `shape`, `description` |
| `DatabaseManager.java` | Migrationen: Spalten `shape`, `description` |
| `MindMapRepository.java` | save/update/load für `shape` und `description` |
