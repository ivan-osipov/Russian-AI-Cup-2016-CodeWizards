import java.util.Arrays;
import java.util.List;

public class Zones {

    public static final Zone HOME = new Zone(new Point2D(900, 3995), new Point2D(790, 3660), new Point2D(760, 3550), new Point2D(560, 3200), new Point2D(400, 3100), new Point2D(5, 3100), new Point2D(5, 3995));

    public static final Zone TOP_FOREFRONT_1 = new Zone(new Point2D(5, 3100), new Point2D(400, 3100), new Point2D(400, 2620), new Point2D(5, 2620));

    public static final Zone TOP_FOREFRONT_2 = new Zone(new Point2D(5, 2620), new Point2D(400, 2620), new Point2D(400, 1550), new Point2D(5, 1550));

    public static final Zone TOP_PRE_ARENA_PLACE = new Zone(new Point2D(5, 1550), new Point2D(400, 1550), new Point2D(400, 970), new Point2D(5, 970));

    public static final Zone TOP_ARENA_PLACE = new Zone(new Point2D(5, 970), new Point2D(400, 970), new Point2D(870, 480), new Point2D(870, 5), new Point2D(5, 5));

    public static final Zone TOP_MIDDLE_TRANSITION = new Zone(new Point2D(870, 480), new Point2D(400, 970), new Point2D(810, 1100), new Point2D(1420, 1670), new Point2D(1575, 1260));

    public static final Zone MIDDLE_FOREFRONT_1 = new Zone(new Point2D(1300, 3090), new Point2D(900, 2650), new Point2D(560, 3200), new Point2D(760, 3550));

    public static final Zone MIDDLE_FOREFRONT_2 = new Zone(new Point2D(2000, 2400), new Point2D(1600, 2050), new Point2D(900, 2650), new Point2D(1300, 3090));

    public static final Zone MIDDLE_BOTTOM_TRANSITION = new Zone(new Point2D(2000, 2400), new Point2D(3200, 3500), new Point2D(3500, 3200), new Point2D(2400, 2100));

    public static final Zone CENTER = new Zone(new Point2D(1600, 2050), new Point2D(1420, 1670), new Point2D(1575, 1260), new Point2D(2400, 2100), new Point2D(2000, 2400));

    public static final Zone BOTTOM_FOREFRONT_1 = new Zone(new Point2D(1400, 3620), new Point2D(1400, 3995), new Point2D(900, 3995), new Point2D(790, 3660));

    public static final Zone BOTTOM_FOREFRONT_2 = new Zone(new Point2D(1400, 3620), new Point2D(1400, 3995), new Point2D(2350, 3995), new Point2D(2350, 3560));

    public static final Zone BOTTOM_PRE_ARENA_PLACE = new Zone(new Point2D(2350, 3995), new Point2D(2350, 3560), new Point2D(3200, 3500), new Point2D(3250, 3995));

    public static final Zone BOTTOM_ARENA_PLACE = new Zone(new Point2D(3250, 3995), new Point2D(3200, 3500), new Point2D(3500, 3200), new Point2D(3995, 3250), new Point2D(3995, 3995));

    public static final Zone LAST_HOME = new Zone(new Point2D(5, 3995), new Point2D(105, 3995), new Point2D(105, 3895), new Point2D(5, 3895));

    public static final List<Zone> ALL_STATIC = Arrays.asList(HOME, TOP_FOREFRONT_1, BOTTOM_FOREFRONT_1, MIDDLE_FOREFRONT_1,
            TOP_FOREFRONT_2, MIDDLE_FOREFRONT_2, BOTTOM_FOREFRONT_2, CENTER,
            TOP_PRE_ARENA_PLACE, BOTTOM_PRE_ARENA_PLACE,TOP_ARENA_PLACE, BOTTOM_ARENA_PLACE,
             TOP_MIDDLE_TRANSITION, MIDDLE_BOTTOM_TRANSITION);

    public static final Zone[] ALL_STATIC_ARRAY =  Zones.ALL_STATIC.toArray(new Zone[Zones.ALL_STATIC.size()]);


    public static final List<Zone> BATTLE_ZONES = Arrays.asList(HOME, TOP_FOREFRONT_1, BOTTOM_FOREFRONT_1, MIDDLE_FOREFRONT_1,
            TOP_FOREFRONT_2, MIDDLE_FOREFRONT_2, BOTTOM_FOREFRONT_2, CENTER,
            TOP_PRE_ARENA_PLACE, BOTTOM_PRE_ARENA_PLACE,TOP_ARENA_PLACE, BOTTOM_ARENA_PLACE);

}
