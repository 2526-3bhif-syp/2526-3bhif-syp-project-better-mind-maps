# CLAUDE.md – Projektkontext

## Projektübersicht

- **Projektname:** Better-Mind-Maps
- **Beschreibung:** Erstellen von Mindmaps, welche gespeichert und wieder geöffnet werden können. Es soll Benutzer geben, welche MindMaps speichern und teilen können.  
- **Tech-Stack:** Java, JavaFX, DB mit SQLite
- **Sprache:** Deutsch (Dokumentation), Englisch (Code)

---

## Anforderungen & User Stories

- User Stories sind als **GitHub Issues** erfasst
- Issues verwenden folgende **Labels zur Priorisierung:**
    - `must-have` – Kernfunktionalität, zwingend erforderlich
    - `should-have` – wichtig, aber nicht kritisch für MVP
    - `could-have` – wünschenswert, wenn Zeit vorhanden
- Issues können zusätzlich nach **Milestones** gruppiert sein (z.B. MVP, Phase 2)
- Jedes Issue enthält eine User Story im Format:
  *„Als [Rolle] möchte ich [Funktion], damit [Nutzen]."*

### Zugriff auf Issues

```bash
# Alle offenen Issues auflisten
gh issue list

# Issues nach Label filtern
gh issue list --label "must-have"

# Einzelnes Issue lesen
gh issue view <NUMMER>

# Issue nach Abschluss schließen
gh issue close <NUMMER> --comment "Umgesetzt in Commit <SHA>"
```

---

### Fachspezifikation (`docs/fachspezifikation.md`)

Beschreibt die fachlichen Anforderungen ohne technische Details. Enthält:

1. **Einleitung** – Projektziel, Scope, Zielgruppen
- Schüler sollen Lerninhalte übersichtlich darstellen können.
- Zusammenhänge zwischen Themen und Begriffen sollen leichter erkennbar sein.
- Die Anwendung soll das Wiederholen und Strukturieren des Unterrichtsstoffs erleichtern.
- Die Inhalte sollen schneller strukturiert werden können als mit normalen Notizen.

2. **Akteure & Rollen** – Wer nutzt das System?
- Hauptgruppe sind Schüler, jedoch können auch andere Gruppen unser Projekt nutzen

3. **Akzeptanzkriterien** – Pro User Story: wann gilt sie als erfüllt?

4. **Nicht-funktionale Anforderungen** – Performance, Verfügbarkeit, Datenschutz
- Usability: Ein neuer Knoten soll in maximal 3 Interaktionen erstellt werden können. Die Anwendung soll ohne Anleitung verständlich sein.
- Performance: Das Laden einer Mind Map mit bis zu 100 Knoten dauert weniger als 1 Sekunde. Das Verschieben eines Knotens erfolgt ohne spürbare Verzögerung
- Robustheit: Ungültige Eingaben, wie zum Beispiel leere Texte, werden abgefangen.
- Zuverlässigkeit: Daten werden dauerhaft in einer SQLite-Datenbank gespeichert und korrekt wieder geladen.

---

### Technische Spezifikation (`docs/technische-spezifikation.md`)

Beschreibt die technische Umsetzung. Enthält:

1. **Architekturübersicht** – Systemkomponenten und deren Zusammenspiel
2. **Tech-Stack** – Frameworks, Libraries, Tools mit Versionen
3. **Datenmodell** – Entitäten, Relationen, Datenbankschema
4. **API-Design** – Endpunkte, Request/Response-Formate
5. **Authentifizierung & Autorisierung** – Sicherheitskonzept
6. **Fehlerbehandlung** – Strategien und Error-Codes
7. **Deployment** – Infrastruktur, CI/CD, Environments
8. **Testing-Strategie** – Unit, Integration, E2E

---

## Arbeitsablauf (Workflow)

### Bei jeder neuen Aufgabe diese Reihenfolge einhalten:

```
1. ISSUE LESEN
   → GitHub Issue lesen und verstehen
   → Akzeptanzkriterien identifizieren

2. FACHSPEZIFIKATION PRÜFEN/ERGÄNZEN
   → Gibt es bereits einen Abschnitt dazu?
   → Falls nein: Abschnitt erstellen
   → Falls ja: Auf Vollständigkeit prüfen, ggf. ergänzen

3. TECHNISCHE SPEZIFIKATION PRÜFEN/ERGÄNZEN
   → Architektur-Auswirkungen dokumentieren
   → Datenmodell erweitern falls nötig
   → API-Endpunkte definieren falls nötig

4. IMPLEMENTIEREN
   → Code schreiben gemäß Spezifikationen
   → Tests schreiben

5. ABSCHLIESSEN
   → Tests ausführen und sicherstellen, dass alles grün ist
   → Issue auf GitHub schließen mit Kommentar
   → Commit-Message referenziert Issue-Nummer (z.B. "feat: Login-Seite #5")
```

### Wichtig:

- **Niemals Code schreiben, bevor die Spezifikationen aktuell sind**
- **Änderungen an der Architektur immer zuerst in der technischen Spezifikation dokumentieren**
- **Bei Unklarheiten: nachfragen, nicht raten**

---

## Code-Konventionen

### Allgemein

- Code und Kommentare auf **Englisch**
- Dokumentation auf **Deutsch**
- Keine auskommentierten Code-Blöcke committen
- Jede Datei hat einen klaren, einzelnen Zweck (Single Responsibility)

### Commit-Messages

Format: `<typ>: <beschreibung> #<issue-nummer>`

Typen:
- `feat` – neues Feature
- `fix` – Bugfix
- `docs` – Dokumentation
- `refactor` – Code-Umbau ohne Funktionsänderung
- `test` – Tests hinzufügen/ändern
- `chore` – Build, Dependencies, Konfiguration

Beispiel: `feat: implement user authentication #12`

### Branching

- `main` – stabiler, deploybarer Stand
- `dev` – Entwicklungsbranch
- Feature-Branches: `feature/<issue-nummer>-<kurzbeschreibung>`
- Beispiel: `feature/12-user-authentication`

---

## Testing

- Jedes Feature braucht Tests
- Tests vor dem Committen ausführen
- Testbefehle: [hier eintragen, z.B. `npm test`, `pytest`]

---

## Schnellreferenz – Häufige Befehle

```bash
# Projekt starten
javafx run

# Tests ausführen
# noch keine vorhanden
```