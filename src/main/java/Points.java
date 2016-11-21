import java.util.ArrayList;
import java.util.List;

public class Points {

    public static List<Point2D> HOME_ZONE_POINTS = new ArrayList<>();

    public static List<Point2D> TOP_START_POINTS = new ArrayList<>();

    public static List<Point2D> MIDDLE_START_POINTS = new ArrayList<>();

    public static List<Point2D> BOTTOM_START_POINTS = new ArrayList<>();

    public static List<Point2D> LOWER_BONUS_ZONE_POINTS = new ArrayList<>();

    public static List<Point2D> UPPER_BONUS_ZONE_POINTS = new ArrayList<>();

    public static Point2D LOWER_BONUS_POINT = new Point2D(2800, 2800);

    public static Point2D UPPER_BONUS_POINT = new Point2D(1200, 1200);

    public static List<Point2D> ENEMY_HOME_ZONE_POINTS = new ArrayList<>();

    public static List<Point2D> ENEMY_TOP_START_POINTS = new ArrayList<>();

    public static List<Point2D> ENEMY_MIDDLE_START_POINTS = new ArrayList<>();

    public static List<Point2D> ENEMY_BOTTOM_START_POINTS = new ArrayList<>();


    public static List<Point2D> CHECK_POINTS = new ArrayList<>();

    public static List<Vector2D> CHECK_POINT_EDGES = new ArrayList<>();

    static {

        fillHomeZonePoints();

        fillTopLineStart();

        fillMiddleLineStart();

        fillBottomLineStart();

        fillLowerBonusZonePoints();

        fillUpperBonusZonePoints();

        fillEnemyHomeZonePoints();

        fillEnemyTopLineStart();

        fillEnemyMiddleLineStart();

        fillEnemyBottomLineStart();

        fillCheckPoints();

        //connection zones
        //home-top
        CHECK_POINT_EDGES.add(new Vector2D(HOME_ZONE_POINTS.get(3), TOP_START_POINTS.get(0)));
        //home-middle
        CHECK_POINT_EDGES.add(new Vector2D(HOME_ZONE_POINTS.get(4), MIDDLE_START_POINTS.get(0)));
        CHECK_POINT_EDGES.add(new Vector2D(HOME_ZONE_POINTS.get(5), MIDDLE_START_POINTS.get(0)));
        //home-bottom
        CHECK_POINT_EDGES.add(new Vector2D(HOME_ZONE_POINTS.get(6), BOTTOM_START_POINTS.get(0)));

        //middle-lower-bonus
        CHECK_POINT_EDGES.add(new Vector2D(MIDDLE_START_POINTS.get(2), LOWER_BONUS_ZONE_POINTS.get(0)));//переход от центра к низу

        //between bonuses
        CHECK_POINT_EDGES.add(new Vector2D(LOWER_BONUS_POINT, UPPER_BONUS_POINT));

        //lower-bonus-bottom
        CHECK_POINT_EDGES.add(new Vector2D(LOWER_BONUS_ZONE_POINTS.get(2), BOTTOM_START_POINTS.get(2)));
        //upper-bonus-middle
        CHECK_POINT_EDGES.add(new Vector2D(UPPER_BONUS_ZONE_POINTS.get(0), MIDDLE_START_POINTS.get(1)));
        //upper-bonus-top
        CHECK_POINT_EDGES.add(new Vector2D(UPPER_BONUS_ZONE_POINTS.get(2), TOP_START_POINTS.get(1)));

        //enemy points
        //home-top
        CHECK_POINT_EDGES.add(new Vector2D(ENEMY_HOME_ZONE_POINTS.get(3), ENEMY_TOP_START_POINTS.get(0)));
        //home-middle
        CHECK_POINT_EDGES.add(new Vector2D(ENEMY_HOME_ZONE_POINTS.get(4), ENEMY_MIDDLE_START_POINTS.get(0)));
        CHECK_POINT_EDGES.add(new Vector2D(ENEMY_HOME_ZONE_POINTS.get(5), ENEMY_MIDDLE_START_POINTS.get(0)));
        //home-bottom
        CHECK_POINT_EDGES.add(new Vector2D(ENEMY_HOME_ZONE_POINTS.get(6), ENEMY_BOTTOM_START_POINTS.get(0)));

        //middle-lower-bonus
        CHECK_POINT_EDGES.add(new Vector2D(ENEMY_MIDDLE_START_POINTS.get(2), LOWER_BONUS_ZONE_POINTS.get(0)));//переход от центра к низу

        //lower-bonus-bottom
        CHECK_POINT_EDGES.add(new Vector2D(LOWER_BONUS_ZONE_POINTS.get(2), ENEMY_TOP_START_POINTS.get(1)));
        //upper-bonus-middle
        CHECK_POINT_EDGES.add(new Vector2D(UPPER_BONUS_ZONE_POINTS.get(0), ENEMY_MIDDLE_START_POINTS.get(1)));
        //upper-bonus-top
        CHECK_POINT_EDGES.add(new Vector2D(UPPER_BONUS_ZONE_POINTS.get(2), ENEMY_BOTTOM_START_POINTS.get(2)));


        CHECK_POINT_EDGES.add(new Vector2D(MIDDLE_START_POINTS.get(1), ENEMY_MIDDLE_START_POINTS.get(1)));
        CHECK_POINT_EDGES.add(new Vector2D(MIDDLE_START_POINTS.get(2), ENEMY_MIDDLE_START_POINTS.get(2)));
        CHECK_POINT_EDGES.add(new Vector2D(MIDDLE_START_POINTS.get(1), ENEMY_MIDDLE_START_POINTS.get(2)));
        CHECK_POINT_EDGES.add(new Vector2D(MIDDLE_START_POINTS.get(2), ENEMY_MIDDLE_START_POINTS.get(1)));
    }

    private static void fillEnemyHomeZonePoints() {
        ENEMY_HOME_ZONE_POINTS.add(convertToEnemyPoint(HOME_ZONE_POINTS.get(0)));//0
        ENEMY_HOME_ZONE_POINTS.add(convertToEnemyPoint(HOME_ZONE_POINTS.get(1)));//1
        ENEMY_HOME_ZONE_POINTS.add(convertToEnemyPoint(HOME_ZONE_POINTS.get(2)));//2
        CHECK_POINT_EDGES.add(new Vector2D(ENEMY_HOME_ZONE_POINTS.get(0), ENEMY_HOME_ZONE_POINTS.get(1)));

        ENEMY_HOME_ZONE_POINTS.add(convertToEnemyPoint(HOME_ZONE_POINTS.get(3)));//3
        CHECK_POINT_EDGES.add(new Vector2D(ENEMY_HOME_ZONE_POINTS.get(1), ENEMY_HOME_ZONE_POINTS.get(3)));
        ENEMY_HOME_ZONE_POINTS.add(convertToEnemyPoint(HOME_ZONE_POINTS.get(4)));//4
        CHECK_POINT_EDGES.add(new Vector2D(ENEMY_HOME_ZONE_POINTS.get(1), ENEMY_HOME_ZONE_POINTS.get(4)));
        ENEMY_HOME_ZONE_POINTS.add(convertToEnemyPoint(HOME_ZONE_POINTS.get(5)));//5
        CHECK_POINT_EDGES.add(new Vector2D(ENEMY_HOME_ZONE_POINTS.get(1), ENEMY_HOME_ZONE_POINTS.get(5)));

        CHECK_POINT_EDGES.add(new Vector2D(ENEMY_HOME_ZONE_POINTS.get(0), ENEMY_HOME_ZONE_POINTS.get(2)));
        CHECK_POINT_EDGES.add(new Vector2D(ENEMY_HOME_ZONE_POINTS.get(2), ENEMY_HOME_ZONE_POINTS.get(5)));
        ENEMY_HOME_ZONE_POINTS.add(convertToEnemyPoint(HOME_ZONE_POINTS.get(6)));//6
        CHECK_POINT_EDGES.add(new Vector2D(ENEMY_HOME_ZONE_POINTS.get(2), ENEMY_HOME_ZONE_POINTS.get(6)));

        CHECK_POINT_EDGES.add(new Vector2D(ENEMY_HOME_ZONE_POINTS.get(3), ENEMY_HOME_ZONE_POINTS.get(4)));
        CHECK_POINT_EDGES.add(new Vector2D(ENEMY_HOME_ZONE_POINTS.get(4), ENEMY_HOME_ZONE_POINTS.get(5)));
        CHECK_POINT_EDGES.add(new Vector2D(ENEMY_HOME_ZONE_POINTS.get(5), ENEMY_HOME_ZONE_POINTS.get(6)));
        CHECK_POINT_EDGES.add(new Vector2D(ENEMY_HOME_ZONE_POINTS.get(3), ENEMY_HOME_ZONE_POINTS.get(5)));
        CHECK_POINT_EDGES.add(new Vector2D(ENEMY_HOME_ZONE_POINTS.get(3), ENEMY_HOME_ZONE_POINTS.get(6)));
        CHECK_POINT_EDGES.add(new Vector2D(ENEMY_HOME_ZONE_POINTS.get(2), ENEMY_HOME_ZONE_POINTS.get(4)));
    }

    private static void fillEnemyTopLineStart() {
        ENEMY_TOP_START_POINTS.add(convertToEnemyPoint(new Point2D(211, 2621)));//0
        ENEMY_TOP_START_POINTS.add(convertToEnemyPoint(new Point2D(190, 1550)));//1
        CHECK_POINT_EDGES.add(new Vector2D(ENEMY_TOP_START_POINTS.get(0), ENEMY_TOP_START_POINTS.get(1)));
    }

    private static void fillEnemyMiddleLineStart() {
        ENEMY_MIDDLE_START_POINTS.add(convertToEnemyPoint(new Point2D(1122, 2890)));//0
        ENEMY_MIDDLE_START_POINTS.add(convertToEnemyPoint(new Point2D(1680, 2120)));//1
        ENEMY_MIDDLE_START_POINTS.add(convertToEnemyPoint(new Point2D(1878, 2285)));//2
        CHECK_POINT_EDGES.add(new Vector2D(ENEMY_MIDDLE_START_POINTS.get(0), ENEMY_MIDDLE_START_POINTS.get(1)));
        CHECK_POINT_EDGES.add(new Vector2D(ENEMY_MIDDLE_START_POINTS.get(0), ENEMY_MIDDLE_START_POINTS.get(2)));
        CHECK_POINT_EDGES.add(new Vector2D(ENEMY_MIDDLE_START_POINTS.get(1), ENEMY_MIDDLE_START_POINTS.get(2)));//точки в начале "центра" рядом
    }

    private static void fillEnemyBottomLineStart() {
        ENEMY_BOTTOM_START_POINTS.add(convertToEnemyPoint(new Point2D(1340, 3800)));//0
        ENEMY_BOTTOM_START_POINTS.add(convertToEnemyPoint(new Point2D(2381, 3851)));//1
        CHECK_POINT_EDGES.add(new Vector2D(ENEMY_BOTTOM_START_POINTS.get(0), ENEMY_BOTTOM_START_POINTS.get(1)));

        ENEMY_BOTTOM_START_POINTS.add(convertToEnemyPoint(new Point2D(3224, 3749)));//2
        CHECK_POINT_EDGES.add(new Vector2D(ENEMY_BOTTOM_START_POINTS.get(1), ENEMY_BOTTOM_START_POINTS.get(2)));
    }

    public static Point2D convertToEnemyPoint(Point2D point) {
        return new Point2D(Zones.MAP_SIZE - point.getX(), Zones.MAP_SIZE - point.getY());
    }

    private static void fillCheckPoints() {
        CHECK_POINTS.addAll(HOME_ZONE_POINTS);
        CHECK_POINTS.addAll(TOP_START_POINTS);
        CHECK_POINTS.addAll(MIDDLE_START_POINTS);
        CHECK_POINTS.addAll(BOTTOM_START_POINTS);
        CHECK_POINTS.addAll(LOWER_BONUS_ZONE_POINTS);
        CHECK_POINTS.addAll(UPPER_BONUS_ZONE_POINTS);
        CHECK_POINTS.add(LOWER_BONUS_POINT);
        CHECK_POINTS.add(UPPER_BONUS_POINT);
        CHECK_POINTS.addAll(ENEMY_HOME_ZONE_POINTS);
        CHECK_POINTS.addAll(ENEMY_TOP_START_POINTS);
        CHECK_POINTS.addAll(ENEMY_MIDDLE_START_POINTS);
        CHECK_POINTS.addAll(ENEMY_BOTTOM_START_POINTS);
    }

    private static void fillUpperBonusZonePoints() {
        UPPER_BONUS_ZONE_POINTS.add(new Point2D(1474, 1589));//0
        CHECK_POINT_EDGES.add(new Vector2D(UPPER_BONUS_ZONE_POINTS.get(0), UPPER_BONUS_POINT));
        UPPER_BONUS_ZONE_POINTS.add(new Point2D(575, 811));//1 upper - top
        CHECK_POINT_EDGES.add(new Vector2D(UPPER_BONUS_ZONE_POINTS.get(1), UPPER_BONUS_POINT));
        UPPER_BONUS_ZONE_POINTS.add(new Point2D(295, 922));//2 top - upper
        CHECK_POINT_EDGES.add(new Vector2D(UPPER_BONUS_ZONE_POINTS.get(1), UPPER_BONUS_ZONE_POINTS.get(2)));

    }

    private static void fillLowerBonusZonePoints() {
        LOWER_BONUS_ZONE_POINTS.add(new Point2D(2104, 2327));//0
        CHECK_POINT_EDGES.add(new Vector2D(LOWER_BONUS_ZONE_POINTS.get(0), LOWER_BONUS_POINT));
        LOWER_BONUS_ZONE_POINTS.add(new Point2D(2649, 2929));//1 "вроль ближней стеночки" переход центр - низ
        CHECK_POINT_EDGES.add(new Vector2D(LOWER_BONUS_ZONE_POINTS.get(0), LOWER_BONUS_ZONE_POINTS.get(1)));
        CHECK_POINT_EDGES.add(new Vector2D(LOWER_BONUS_POINT, LOWER_BONUS_ZONE_POINTS.get(1)));
        LOWER_BONUS_ZONE_POINTS.add(new Point2D(3247, 3457));//2 сторона "с центра" нижней боевой зоны
        CHECK_POINT_EDGES.add(new Vector2D(LOWER_BONUS_ZONE_POINTS.get(1), LOWER_BONUS_ZONE_POINTS.get(2)));
        CHECK_POINT_EDGES.add(new Vector2D(LOWER_BONUS_POINT, LOWER_BONUS_ZONE_POINTS.get(2)));
    }

    private static void fillTopLineStart() {
        TOP_START_POINTS.add(new Point2D(211, 2621));//0
        TOP_START_POINTS.add(new Point2D(190, 1550));//1
        CHECK_POINT_EDGES.add(new Vector2D(TOP_START_POINTS.get(0), TOP_START_POINTS.get(1)));
    }

    private static void fillHomeZonePoints() {
        HOME_ZONE_POINTS.add(new Point2D(185, 3741));//0
        HOME_ZONE_POINTS.add(new Point2D(236, 3387));//1
        HOME_ZONE_POINTS.add(new Point2D(506, 3777));//2
        CHECK_POINT_EDGES.add(new Vector2D(HOME_ZONE_POINTS.get(0), HOME_ZONE_POINTS.get(1)));

        HOME_ZONE_POINTS.add(new Point2D(177, 3094));//3
        CHECK_POINT_EDGES.add(new Vector2D(HOME_ZONE_POINTS.get(1), HOME_ZONE_POINTS.get(3)));
        HOME_ZONE_POINTS.add(new Point2D(583, 3228));//4
        CHECK_POINT_EDGES.add(new Vector2D(HOME_ZONE_POINTS.get(1), HOME_ZONE_POINTS.get(4)));
        HOME_ZONE_POINTS.add(new Point2D(731, 3505));//5
        CHECK_POINT_EDGES.add(new Vector2D(HOME_ZONE_POINTS.get(1), HOME_ZONE_POINTS.get(5)));

        CHECK_POINT_EDGES.add(new Vector2D(HOME_ZONE_POINTS.get(0), HOME_ZONE_POINTS.get(2)));
        CHECK_POINT_EDGES.add(new Vector2D(HOME_ZONE_POINTS.get(2), HOME_ZONE_POINTS.get(5)));
        HOME_ZONE_POINTS.add(new Point2D(816, 3730));//6
        CHECK_POINT_EDGES.add(new Vector2D(HOME_ZONE_POINTS.get(2), HOME_ZONE_POINTS.get(6)));

        CHECK_POINT_EDGES.add(new Vector2D(HOME_ZONE_POINTS.get(3), HOME_ZONE_POINTS.get(4)));
        CHECK_POINT_EDGES.add(new Vector2D(HOME_ZONE_POINTS.get(4), HOME_ZONE_POINTS.get(5)));
        CHECK_POINT_EDGES.add(new Vector2D(HOME_ZONE_POINTS.get(5), HOME_ZONE_POINTS.get(6)));
        CHECK_POINT_EDGES.add(new Vector2D(HOME_ZONE_POINTS.get(3), HOME_ZONE_POINTS.get(5)));
        CHECK_POINT_EDGES.add(new Vector2D(HOME_ZONE_POINTS.get(3), HOME_ZONE_POINTS.get(6)));
        CHECK_POINT_EDGES.add(new Vector2D(HOME_ZONE_POINTS.get(2), HOME_ZONE_POINTS.get(4)));

    }

    private static void fillMiddleLineStart() {
        MIDDLE_START_POINTS.add(new Point2D(1122, 2890));//0
        MIDDLE_START_POINTS.add(new Point2D(1680, 2120));//1
        MIDDLE_START_POINTS.add(new Point2D(1878, 2285));//2
        CHECK_POINT_EDGES.add(new Vector2D(MIDDLE_START_POINTS.get(0), MIDDLE_START_POINTS.get(1)));
        CHECK_POINT_EDGES.add(new Vector2D(MIDDLE_START_POINTS.get(0), MIDDLE_START_POINTS.get(2)));
        CHECK_POINT_EDGES.add(new Vector2D(MIDDLE_START_POINTS.get(1), MIDDLE_START_POINTS.get(2)));//точки в начале "центра" рядом
    }

    private static void fillBottomLineStart() {
        BOTTOM_START_POINTS.add(new Point2D(1340, 3800));//0
        BOTTOM_START_POINTS.add(new Point2D(2381, 3851));//1
        CHECK_POINT_EDGES.add(new Vector2D(BOTTOM_START_POINTS.get(0), BOTTOM_START_POINTS.get(1)));

        BOTTOM_START_POINTS.add(new Point2D(3224, 3749));//2
        CHECK_POINT_EDGES.add(new Vector2D(BOTTOM_START_POINTS.get(1), BOTTOM_START_POINTS.get(2)));
    }

}
