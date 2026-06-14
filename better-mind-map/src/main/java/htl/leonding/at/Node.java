package htl.leonding.at;

public class Node {
    private String _id;
    private String _text;
    private String _parentId;
    private double _xCoordinate;
    private double _yCoordinate;
    private double _textSize;
    private String _color;

    public Node(String id, String text, String parentId, double xCoordinate, double yCoordinate) {
        this(id, text, parentId, xCoordinate, yCoordinate, 12.0, "#ffffff");
    }

    public Node(String id, String text, String parentId, double xCoordinate, double yCoordinate, double textSize, String color) {
        _id = id;
        _text = text;
        _parentId = parentId;
        _xCoordinate = xCoordinate;
        _yCoordinate = yCoordinate;
        _textSize = textSize == 0 ? 12.0 : textSize;
        _color = (color == null || color.isEmpty()) ? "#ffffff" : color;
    }

    public String getId() { return _id; }
    public String getText() { return _text; }
    public void setText(String text) { _text = text; }
    public String getParentId() { return _parentId; }
    public double getXCoordinate() { return _xCoordinate; }
    public double getYCoordinate() { return _yCoordinate; }
    public void setXCoordinate(double x) { _xCoordinate = x; }
    public void setYCoordinate(double y) { _yCoordinate = y; }
    public double getTextSize() { return _textSize; }
    public void setTextSize(double textSize) { _textSize = textSize; }
    public String getColor() { return _color; }
    public void setColor(String color) { _color = color; }
}
