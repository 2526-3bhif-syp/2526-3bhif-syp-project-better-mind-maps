# Chat- History
## Paul Lohninger

create a chat.md in the root folder of my project

Bitte überprüfe ob du Zugriff auf die Issues hast im Github?

Bitte hole die User Stories welche in Form von Issues in Github gespeichert sind und aktualisiere die UserStories von user-story.adoc


## Julian Heissinger

Analyse this project, ask me questions about it, review the code that is already there, once you dont have any questions anymore I will give you the first user story to implement, try to stay close the UI-Sketch, when we get to coding.

No, leave the mindmap as it is. Set up the JavaFX project once I give you the first user, story. You can expand the model as much as you like, try to keep it minimal. Try to stick closely to the UI Sketch, ask for permission if you want to change anything. Any more questions?

Story
As a student, I want to create a new mind map so that I can collect and organize ideas.

Acceptance Criteria
The user can create a new mind map.
The user can enter a name for the mind map.
An empty mind map with a root node is created.
The new mind map is displayed immediately.
The mind map can be saved.
Effort
5

I do not accept your implementation, use an .FXML file for the view.

add the JavaFX dependencies to the pom.xml

Refactor this Project to use the MVP pattern

## Ryan James

## Arda Dönmez

Lies dir die User Story #7 aus dem GitHub Issue "Move / Arrange Nodes" durch.

**Aufgabe:**
Ich möchte die Funktion implementieren, mit der User Nodes in der Mindmap frei bewegen können.

**Anforderungen basierend auf den Akzeptanzkriterien:**
1. Ermögliche es, Nodes per Drag-and-Drop frei zu verschieben.
2. Die Position im Datenmodell muss sofort aktualisiert werden.
3. Die Verbindungslinien zwischen den Nodes müssen sich automatisch mitbewegen.
4. Achte darauf, dass das MVP-Pattern (wie von Julian Heissinger gefordert) beibehalten wird.

**Kontext:**
- Wir nutzen JavaFX und FXML.
- Die Abhängigkeiten sind bereits in der pom.xml vorhanden.

Bitte analysiere den aktuellen Code im `view` und `presenter` Paket und schlage mir die notwendigen Änderungen vor, um das Verschieben der Nodes zu ermöglichen.

Die neuen Nodes die erstellt werden sollen nicht immer weiter unten platziert werden sondern den am nähsten freien platz

die nodes sollen beim erstellen nicht untereinander sein sondern sollen kreislich gehen. Das jetzige problem ist, dass sie nur an einer bestimmten position entweder wie in einem array nur von unten nach oben erstellt wird.

Erstelle ein Button der zu einem KI chat führt. Ich möchte eine funktion haben wo ich der KI sage was für eine art von mindmap ich haben möchte und die KI soll sie mir dann erstellen. Sie soll achten dass sie strukturiert ist und so übersichtlich macht wie es geht. Die KI funktion soll sehr schön gestyled sein.

Das jetzige Problem von der KI function ist, dass sie nicht das macht was ich will bsp.: ich wollte eine Mindmap über aktien und sie macht es nicht richtig. Die KI soll selber recherchieren was die wichtgisten begriffe für die mindmap ist und sie dementsprechend "zeichne".