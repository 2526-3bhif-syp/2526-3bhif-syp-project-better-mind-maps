package htl.leonding.at;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.Optional;

public class MainController {

    @FXML
    private TreeView<String> hierarchyTree;
    @FXML
    private TabPane tabPane;

    @FXML
    public void initialize() {
        // Any startup setup goes here
    }

    @FXML
    public void onCreateNewMap() {
        TextInputDialog dialog = new TextInputDialog("New Map");
        dialog.setTitle("New Mind Map");
        dialog.setHeaderText("Create a new Mind Map");
        dialog.setContentText("Please enter the name:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            MindMap map = MindMap.createNew(name);
            renderMindMap(map);
        });
    }

    private void renderMindMap(MindMap map) {
        Tab tab = new Tab(map.getName());
        Pane canvas = new Pane();

        for (Node node : map.getNodes()) {
            StackPane nodeView = new StackPane();
            javafx.scene.shape.Ellipse ellipse = new javafx.scene.shape.Ellipse(40, 25);
            ellipse.setFill(javafx.scene.paint.Color.WHITE);
            ellipse.setStroke(javafx.scene.paint.Color.BLACK);
            Label label = new Label(node.getText());
            nodeView.getChildren().addAll(ellipse, label);

            nodeView.setLayoutX(node.getXCoordinate() - 40);
            nodeView.setLayoutY(node.getYCoordinate() - 25);

            canvas.getChildren().add(nodeView);
        }

        tab.setContent(canvas);
        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);

        TreeItem<String> rootItem = new TreeItem<>(map.getName());
        for (Node node : map.getNodes()) {
            rootItem.getChildren().add(new TreeItem<>(node.getText()));
        }
        rootItem.setExpanded(true);
        hierarchyTree.setRoot(rootItem);

        tab.setUserData(map);
    }
}
