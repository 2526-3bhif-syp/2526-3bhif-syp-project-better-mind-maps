package htl.leonding.at;

public class Node {
    private String _id;
    private String _text;
    private String _parentId;
    private double _xCoordinate;
    private double _yCoordinate;
    private double _textSize;
    private String _color;
    private String _shape;
    private String _description;
    private String _icon;
    private String _badge;

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
        _shape = "ROUNDED_RECT";
        _description = "";
        _icon = "";
        _badge = "";
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
    public String getShape() { return _shape == null ? "ROUNDED_RECT" : _shape; }
    public void setShape(String shape) { _shape = shape; }
    public String getDescription() { return _description == null ? "" : _description; }
    public void setDescription(String description) { _description = description; }
    public String getIcon() { return _icon == null ? "" : _icon; }
    public void setIcon(String icon) { _icon = icon == null ? "" : icon; }
    public String getBadge() { return _badge == null ? "" : _badge; }
    public void setBadge(String badge) { _badge = badge == null ? "" : badge; }
}
