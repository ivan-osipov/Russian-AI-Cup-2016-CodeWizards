import java.util.ArrayList;
import java.util.List;

public class Zone {

    private Point2D[] corners;
    private Point2D centroid;

    public Zone(Point2D... corners) {
        this.corners = corners;
        this.centroid = calculateCentroid(corners);
    }

    public Point2D[] getCorners() {
        return corners;
    }

    private Point2D calculateCentroid(Point2D[] corners) {
        double x = 0;
        double y = 0;
        for (Point2D corner : corners) {
            x += corner.getX();
            y += corner.getY();
        }
        x /= corners.length;
        y /= corners.length;
        return new Point2D(x, y);
    }

    public boolean contains(Point2D point) {
        double x = point.getX();
        double y = point.getY();
        int polySides = corners.length;
        boolean oddTransitions = false;
        for (int i = 0, j = polySides - 1; i < polySides; j = i++) {
            if ((corners[i].getY() < y && corners[j].getY() >= y) || (corners[j].getY() < y && corners[i].getY() >= y)) {
                if (corners[i].getX() + (y - corners[i].getY()) / (corners[j].getY() - corners[i].getY()) * (corners[j].getX() - corners[i].getX()) < x) {
                    oddTransitions = !oddTransitions;
                }
            }
        }
        return oddTransitions;
    }

    public List<Line2D> getLines() {
        List<Line2D> lines = new ArrayList<>();
        for (int i = 0; i < corners.length - 1; i++) {
            lines.add(new Line2D(corners[i], corners[i+1]));
        }
        lines.add(new Line2D(corners[corners.length - 1], corners[0]));
        return lines;
    }

    public Point2D getCentroid() {
        return centroid;
    }

}
