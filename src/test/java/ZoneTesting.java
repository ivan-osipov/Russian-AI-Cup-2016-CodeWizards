import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ZoneTesting {

    @Test
    public void pointInsideZone() {
        Zone square = new Zone(new Point2D(0,0), new Point2D(10,0), new Point2D(10,10), new Point2D(0,10));
        Point2D center = new Point2D(5,5);
        assertTrue(square.contains(center));
    }

    @Test
    public void pointOutsideZone() {
        Zone square = new Zone(new Point2D(0,0), new Point2D(10,0), new Point2D(10,10), new Point2D(0,10));
        Point2D center = new Point2D(11,11);
        assertFalse(square.contains(center));
    }

    @Test
    public void pointOnTopLineOfZone() {
        Zone square = new Zone(new Point2D(0,0), new Point2D(10,0), new Point2D(10,10), new Point2D(0,10));
        Point2D center = new Point2D(5,10);
        assertTrue(square.contains(center));
    }

    @Test
    public void pointHigherThanTopLineOfZone() {
        Zone square = new Zone(new Point2D(0,0), new Point2D(10,0), new Point2D(10,10), new Point2D(0,10));
        Point2D center = new Point2D(5,10.1);
        assertFalse(square.contains(center));
    }

    @Test
    public void pointOnBottomLineOfZone() {
        Zone square = new Zone(new Point2D(0,0), new Point2D(10,0), new Point2D(10,10), new Point2D(0,10));
        Point2D center = new Point2D(5,0);
        assertTrue(square.contains(center));
    }

    @Test
    public void pointLowerThanBottomLineOfZone() {
        Zone square = new Zone(new Point2D(0,1), new Point2D(10,1), new Point2D(10,11), new Point2D(0,11));
        Point2D center = new Point2D(5, 0.99);
        assertFalse(square.contains(center));
    }

    @Test
    public void pointAtLeftLineOfZone() {
        Zone square = new Zone(new Point2D(0,0), new Point2D(10,0), new Point2D(10,10), new Point2D(0,10));
        Point2D center = new Point2D(0,5);
        assertTrue(square.contains(center));
    }

    @Test
    public void pointLeftThanLeftLineOfZone() {
        Zone square = new Zone(new Point2D(1,1), new Point2D(11,1), new Point2D(11,11), new Point2D(1,11));
        Point2D center = new Point2D(0.99, 5);
        assertFalse(square.contains(center));
    }

    @Test
    public void pointAtRightLineOfZone() {
        Zone square = new Zone(new Point2D(0,0), new Point2D(10,0), new Point2D(10,10), new Point2D(0,10));
        Point2D center = new Point2D(10,5);
        assertTrue(square.contains(center));
    }

    @Test
    public void pointRightThanRightLineOfZone() {
        Zone square = new Zone(new Point2D(0,0), new Point2D(10,0), new Point2D(10,10), new Point2D(0,10));
        Point2D center = new Point2D(10.0001, 5);
        assertFalse(square.contains(center));
    }

}
