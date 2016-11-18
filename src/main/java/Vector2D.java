public class Vector2D {

    private Point2D start;

    private Point2D end;

    public Vector2D(Point2D start, Point2D end) {
        this.start = start;
        this.end = end;
    }

    public Vector2D getReverse() {
        return new Vector2D(end, start);
    }

    public double distanceBetween() {
        return start.getDistanceTo(end);
    }

    public Point2D getStart() {
        return start;
    }

    public void setStart(Point2D start) {
        this.start = start;
    }

    public Point2D getEnd() {
        return end;
    }

    public void setEnd(Point2D end) {
        this.end = end;
    }
}
