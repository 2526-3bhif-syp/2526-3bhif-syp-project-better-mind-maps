package htl.leonding.at;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class OverviewController {

    @FXML private VBox cardsContainer;

    private final MindMapRepository repository = new MindMapRepository();
    private final MindMapService service = new MindMapService(repository);

    @FXML
    public void initialize() {
        loadMaps();
    }

    private void loadMaps() {
        cardsContainer.getChildren().clear();
        List<MindMap> maps = repository.loadAll();

        if (maps.isEmpty()) {
            Label empty = new Label("No mind maps yet. Click \"+ New Mind Map\" to get started.");
            empty.getStyleClass().add("empty-label");
            cardsContainer.getChildren().add(empty);
            return;
        }

        for (MindMap map : maps) {
            cardsContainer.getChildren().add(createCard(map));
        }
    }

    private HBox createCard(MindMap map) {
        // Avatar showing first letter
        Label avatar = new Label(map.getName().substring(0, 1).toUpperCase());
        avatar.getStyleClass().add("card-avatar");

        Label nameLabel = new Label(map.getName());
        nameLabel.getStyleClass().add("card-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button openBtn = new Button("Open");
        openBtn.getStyleClass().add("btn-blue");
        openBtn.setOnAction(e -> openEditor(map));

        Button deleteBtn = new Button("Delete");
        deleteBtn.getStyleClass().add("btn-danger");
        deleteBtn.setOnAction(e -> confirmDelete(map));

        HBox card = new HBox(14, avatar, nameLabel, spacer, openBtn, deleteBtn);
        card.getStyleClass().add("map-card");
        card.setAlignment(Pos.CENTER_LEFT);

        // Double-click opens the map
        card.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) openEditor(map);
        });

        return card;
    }

    private void confirmDelete(MindMap map) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Mind Map");
        confirm.setHeaderText("Delete \"" + map.getName() + "\"?");
        confirm.setContentText("This action cannot be undone.");
        confirm.showAndWait()
                .filter(btn -> btn == ButtonType.OK)
                .ifPresent(btn -> {
                    service.deleteMindMap(map.getId());
                    loadMaps();
                });
    }

    @FXML
    private void onNewMap() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("New Mind Map");
        dialog.setHeaderText("Create a new Mind Map");
        dialog.setContentText("Name:");
        Optional<String> result = dialog.showAndWait();
        result.map(String::trim).filter(s -> !s.isEmpty()).ifPresent(name -> {
            MindMap map = service.createMindMap(name);
            openEditor(map);
        });
    }

    private void openEditor(MindMap map) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("main-view.fxml"));
            Scene scene = new Scene(loader.load(), 1024, 768);
            MainController controller = loader.getController();
            controller.loadMindMap(map);
            Stage stage = (Stage) cardsContainer.getScene().getWindow();
            stage.setScene(scene);
        } catch (IOException e) {
            throw new RuntimeException("Failed to open editor", e);
        }
    }
}
