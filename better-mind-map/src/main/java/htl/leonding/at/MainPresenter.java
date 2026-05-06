package htl.leonding.at;

public class MainPresenter {
    private final MainView view;

    public MainPresenter(MainView view) {
        this.view = view;
    }

    public void createNewMap(String name) {
        MindMap map = MindMap.createNew(name);
        view.displayMindMap(map);
    }
}
