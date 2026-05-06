package htl.leonding.at;

public class Node {
    private String _id;
    private String _text;
    private String _parentId;
    private double _xCoordinate;
    private double _yCoordinate;

    public Node(String id, String text, String parentId, double xCoordinate, double yCoordinate) {
        _id = id;
        _text = text;
        _parentId = parentId;
        _xCoordinate = xCoordinate;
        _yCoordinate = yCoordinate;
    }

    public String getId() { return _id; }
    public String getText() { return _text; }
    public void setText(String text) { _text = text; }
    public String getParentId() { return _parentId; }
    public double getXCoordinate() { return _xCoordinate; }
    public double getYCoordinate() { return _yCoordinate; }
    public void setXCoordinate(double x) { _xCoordinate = x; }
    public void setYCoordinate(double y) { _yCoordinate = y; }
}
