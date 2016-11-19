import model.*;

import java.awt.*;
import java.util.*;
import java.util.List;

import static model.SkillType.*;


public final class MyStrategy implements Strategy {

    private DebugVisualizer VISUALIZER;
    private Random random;
    private Wizard self;
    private World world;
    private Game game;
    private Move move;

    private WizardState wizardState;
    private List<Zone> capturedZones;
    private int currentZoneNumber;
    private EnumMap<WizardState, Behaviour> behaviours;

    private StatisticCollector statisticCollector;

    private GameMapGraph graph = new GameMapGraph();
    private GraphMapper graphMapper = new GraphMapper();

    private static final int TARGET_POSITION_IN_RATING = 0;

//    private final Map<LaneType, Point2D[]> waypointsByLane = new EnumMap<>(LaneType.class);

    private final Map<LaneType, Point2D[]> waypointsToBonus = new EnumMap<>(LaneType.class);

    private final Point2D center = new Point2D(2000, 2000);
    private final List<SkillType> skillTypesToLearn = new ArrayList<>();

    private static final int MISSILE_BRANCH = 0;
    private static final int FROST_BOLT_BRANCH = 1;
    private static final int STAFF_AND_FIREBALL_BRANCH = 2;
    private static final int HASTE_BRANCH = 3;
    private static final int SHIELD_BRANCH = 4;

    private final SkillType[][] skillBranches = {
            {RANGE_BONUS_PASSIVE_1, RANGE_BONUS_AURA_1, RANGE_BONUS_PASSIVE_2, RANGE_BONUS_AURA_2, ADVANCED_MAGIC_MISSILE},
            {MAGICAL_DAMAGE_BONUS_PASSIVE_1, MAGICAL_DAMAGE_BONUS_AURA_1, MAGICAL_DAMAGE_BONUS_PASSIVE_2, MAGICAL_DAMAGE_BONUS_AURA_2, FROST_BOLT},
            {STAFF_DAMAGE_BONUS_PASSIVE_1, STAFF_DAMAGE_BONUS_AURA_1, STAFF_DAMAGE_BONUS_PASSIVE_2, STAFF_DAMAGE_BONUS_AURA_2, FIREBALL},
            {MOVEMENT_BONUS_FACTOR_PASSIVE_1, MOVEMENT_BONUS_FACTOR_AURA_1, MOVEMENT_BONUS_FACTOR_PASSIVE_2, MOVEMENT_BONUS_FACTOR_AURA_2, HASTE},
            {MAGICAL_DAMAGE_ABSORPTION_PASSIVE_1, MAGICAL_DAMAGE_ABSORPTION_AURA_1, MAGICAL_DAMAGE_ABSORPTION_PASSIVE_2, MAGICAL_DAMAGE_ABSORPTION_AURA_2, SHIELD}
    };

    private ActionType favoriteActionType = ActionType.FROST_BOLT;

    private Integer mySkillBranch = FROST_BOLT_BRANCH;

    private Integer mySecondSkillBranch = null;

    private Boolean damager = true;

    private Integer skillLevel = 0;

    private int lastTickCheck = 0;

    private int weightOfAggressiveness;

    private Point2D[] waypoints;

    private enum MovingState {STABLE_PATH, TO_BONUS}

    private Point2D previousPosition = new Point2D(0, 0);

    private int stayingTime = 0;

    private LivingUnitsObjective livingUnitsObjective = new LivingUnitsObjective();


    private void initializeStrategy() {
        if (wizardState == null) {
            VISUALIZER = new DebugVisualizer();

            wizardState = WizardState.WALKING;
            capturedZones = Arrays.asList(Zones.HOME, Zones.MIDDLE_FOREFRONT_1, Zones.MIDDLE_FOREFRONT_2, Zones.CENTER,
                    Zones.ENEMY_MIDDLE_FOREFRONT_2, Zones.ENEMY_MIDDLE_FOREFRONT_1, Zones.ENEMY_HOME);
            mySkillBranch = FROST_BOLT_BRANCH;
            mySecondSkillBranch = STAFF_AND_FIREBALL_BRANCH;
            favoriteActionType = ActionType.FROST_BOLT;

            random = new Random(game.getRandomSeed());
            Points.CHECK_POINTS.forEach(point -> {
                GameMapGraph.Node node = graphMapper.map(point);
                graph.addNode(node);
            });
            Points.CHECK_POINT_EDGES.forEach(checkPointEdge -> {
                GameMapGraph.Edge edge = graphMapper.map(checkPointEdge);
                graph.addEdge(edge);
                graph.addEdge(graphMapper.map(checkPointEdge.getReverse()));
            });
//            double mapSize = game.getMapSize();
//            waypointsByLane.put(LaneType.MIDDLE, new Point2D[]{
//                    new Point2D(100.0D, mapSize - 100.0D),
//                    random.nextBoolean()
//                            ? new Point2D(600.0D, mapSize - 200.0D)
//                            : new Point2D(200.0D, mapSize - 600.0D),
//                    new Point2D(800.0D, mapSize - 800.0D),
//                    new Point2D(mapSize - 600.0D, 600.0D)
//            });
//
//            waypointsByLane.put(LaneType.TOP, new Point2D[]{
//                    new Point2D(100.0D, mapSize - 100.0D),
//                    new Point2D(100.0D, mapSize - 400.0D),
//                    new Point2D(200.0D, mapSize - 800.0D),
//                    new Point2D(200.0D, mapSize * 0.75D),
//                    new Point2D(200.0D, mapSize * 0.5D),
//                    new Point2D(200.0D, mapSize * 0.25D),
//                    new Point2D(200.0D, 200.0D),
//                    new Point2D(mapSize * 0.25D, 200.0D),
//                    new Point2D(mapSize * 0.5D, 200.0D),
//                    new Point2D(mapSize * 0.75D, 200.0D),
//                    new Point2D(mapSize - 200.0D, 200.0D)
//            });
//
//            waypointsByLane.put(LaneType.BOTTOM, new Point2D[]{
//                    new Point2D(100.0D, mapSize - 100.0D),
//                    new Point2D(400.0D, mapSize - 100.0D),
//                    new Point2D(800.0D, mapSize - 200.0D),
//                    new Point2D(mapSize * 0.25D, mapSize - 200.0D),
//                    new Point2D(mapSize * 0.5D, mapSize - 200.0D),
//                    new Point2D(mapSize * 0.75D, mapSize - 200.0D),
//                    new Point2D(mapSize - 200.0D, mapSize - 200.0D),
//                    new Point2D(mapSize - 200.0D, mapSize * 0.75D),
//                    new Point2D(mapSize - 200.0D, mapSize * 0.5D),
//                    new Point2D(mapSize - 200.0D, mapSize * 0.25D),
//                    new Point2D(mapSize - 200.0D, 200.0D)
//            });
//
//            waypointsToBonus.put(LaneType.TOP, new Point2D[]{
//                    new Point2D(200.0D, 200.0D),
//                    new Point2D(400.0D, 400.0D),
//                    new Point2D(600.0D, 600.0D),
//                    new Point2D(800.0D, 800.0D),
//                    new Point2D(1000.0D, 1000.0D),
//                    new Point2D(1200.0D, 1200.0D)
//            });
//
//            waypointsToBonus.put(LaneType.BOTTOM, new Point2D[]{
//                    new Point2D(mapSize - 200.0D, mapSize - 200.0D),
//                    new Point2D(mapSize - 400.0D, mapSize - 400.0D),
//                    new Point2D(mapSize - 600.0D, mapSize - 600.0D),
//                    new Point2D(mapSize - 800.0D, mapSize - 800.0D),
//                    new Point2D(mapSize - 1000.0D, mapSize - 1000.0D),
//                    new Point2D(mapSize - 1200.0D, mapSize - 1200.0D)
//            });
//
//            switch ((int) self.getId()) {
//                case 1:
//                case 2:
//                case 6:
//                case 7:
//                    myLane = LaneType.TOP;
//                    break;
//                case 3:
//                case 8:
//                    myLane = LaneType.MIDDLE;
//                    break;
//                case 4:
//                case 5:
//                case 9:
//                case 10:
//                    myLane = LaneType.BOTTOM;
//                    break;
//                default:
//            }
//            waypoints = waypointsByLane.get(myLane);
//            bonusesByLane.put(LaneType.TOP, new Point2D(1200, 1200));
//            bonusesByLane.put(LaneType.MIDDLE, new Point2D(1200, 1200));
//            bonusesByLane.put(LaneType.BOTTOM, new Point2D(2800, 2800));
        }
        behaviours = new EnumMap<>(WizardState.class);
        behaviours.put(WizardState.WALKING, new WalkingBehaviour(self, world, game, move, this));
        behaviours.put(WizardState.PROTECTION, new ProtectionBehaviour(self, world, game, move, this));
        behaviours.put(WizardState.PUSHING, new PushingBehaviour(self, world, game, move, this));

        updateCurrentZoneNumber();
        statisticCollector = new StatisticCollector(self, world);
//        List<Player> playersSortedByScore = Arrays.asList(world.getPlayers());
//        Collections.sort(playersSortedByScore, (p1, p2) -> Integer.compare(p2.getScore(), p1.getScore()));
//        int ownPositionInRating = IntStream.range(0, playersSortedByScore.size())
//                .filter(num -> playersSortedByScore.get(num).isMe())
//                .findFirst()
//                .getAsInt();
//        weightOfAggressiveness = TARGET_POSITION_IN_RATING - ownPositionInRating;
        drawStatistic(statisticCollector.collectZoneStatistic());
    }

    private void updateCurrentZoneNumber() {
        for (int i = 0; i < capturedZones.size(); i++) {
            if(capturedZones.get(i).contains(new Point2D(self.getX(), self.getY()))) {
                currentZoneNumber = i;
                return;
            }
        }
        wizardState = WizardState.UNDEFINED;
    }

    public int getCurrentZoneNumber() {
        return currentZoneNumber;
    }

    public StatisticCollector getStatisticCollector() {
        return statisticCollector;
    }

    public void setWizardState(WizardState wizardState) {
        this.wizardState = wizardState;
    }

    private void initializeTick(Wizard self, World world, Game game, Move move) {
        this.self = self;
        this.world = world;
        this.game = game;
        this.move = move;
    }

    @Override
    public void move(Wizard self, World world, Game game, Move move) {
        initializeTick(self, world, game, move);
        initializeStrategy();
        learnSkills();
//        processMessages();

        Behaviour behaviour = behaviours.get(wizardState);
        if(behaviour == null) {
            move.setAction(ActionType.NONE);
            System.err.println("Behaviour not found: " + wizardState);
        } else {
            behaviour.doIt();
        }
//        if (movingState == MovingState.STABLE_PATH) {
//            //todo ожидание миньёнов у башни
//            //todo не пытаться отступать внутри квадрата 150х150
//
//            if (!alliesOwnTerritory) {
//                goTo(getPreviousWaypoint());
//            } else {
//                goTo(getNextWaypoint());
//            }
//        } else if (movingState == MovingState.TO_BONUS) {
//            Point2D bonus = bonusesByLane.get(myLane);
//            if (self.getDistanceTo(bonus.getX(), bonus.getY()) <= game.getWizardVisionRange()) {
//                boolean bonusExists = false;
//                for (Bonus currentBonus : world.getBonuses()) {
//                    if (currentBonus.getX() == bonus.getX() && currentBonus.getY() == bonus.getY()) {
//                        bonusExists = true;
//                        break;
//                    }
//                }
//                if (bonusExists) {
//                    goTo(getNextWaypointToBonus());
//                } else {
//                    //todo need to attack
//                    goTo(getPreviousWaypointToBonus());
//                }
//                bonusIsChecked = true;
//            } else {
//                if (bonusIsChecked) {
//                    movingState = MovingState.STABLE_PATH;
//                } else {
//                    goTo(getNextWaypointToBonus());
//                }
//
//            }
//        }
//        Point2D currentPosition = new Point2D(self.getX(), self.getY());
//        if (previousPosition.equals(currentPosition)) {
//            if (stayingTime > 10 && stayingTime < 20) {
//                move.setStrafeSpeed(game.getWizardStrafeSpeed());
//            } else if (stayingTime >= 20 && stayingTime < 30) {
//                move.setStrafeSpeed(-game.getWizardStrafeSpeed());
//            } else if (stayingTime >= 30 && stayingTime < 40) {
//                move.setSpeed(game.getWizardBackwardSpeed());
//                move.setStrafeSpeed(game.getWizardStrafeSpeed());
//            } else {
//                move.setSpeed(game.getWizardBackwardSpeed());
//                move.setStrafeSpeed(-game.getWizardStrafeSpeed());
//            }
//            stayingTime++;
//        } else {
//            stayingTime = 0;
//        }
//        previousPosition = currentPosition;
    }

    public void learnSkills() {
        switch (self.getLevel()) {
            case 2:
                move.setSkillToLearn(skillBranches[mySkillBranch][0]);
                break;
            case 3:
                move.setSkillToLearn(skillBranches[mySkillBranch][1]);
                break;
            case 4:
                move.setSkillToLearn(skillBranches[mySkillBranch][2]);
                break;
            case 5:
                move.setSkillToLearn(skillBranches[mySkillBranch][3]);
                break;
            case 6:
                move.setSkillToLearn(skillBranches[mySkillBranch][4]);
                break;
            case 7:
                move.setSkillToLearn(skillBranches[mySecondSkillBranch][0]);
                break;
            case 8:
                move.setSkillToLearn(skillBranches[mySecondSkillBranch][1]);
                break;
            case 9:
                move.setSkillToLearn(skillBranches[mySecondSkillBranch][2]);
                break;
            case 10:
                move.setSkillToLearn(skillBranches[mySecondSkillBranch][3]);
                break;
            case 11:
                move.setSkillToLearn(skillBranches[mySecondSkillBranch][4]);
                break;
            case 12:
                move.setSkillToLearn(skillBranches[SHIELD_BRANCH][0]);
                break;
            case 13:
                move.setSkillToLearn(skillBranches[SHIELD_BRANCH][1]);
                break;
            case 14:
                move.setSkillToLearn(skillBranches[SHIELD_BRANCH][2]);
                break;
            case 15:
                move.setSkillToLearn(skillBranches[SHIELD_BRANCH][3]);
                break;
            case 16:
                move.setSkillToLearn(skillBranches[SHIELD_BRANCH][4]);
                break;
        }

    }

    public List<Zone> getCapturedZones() {
        return capturedZones;
    }

    public GameMapGraph getGraph() {
        return graph;
    }

    public GraphMapper getGraphMapper() {
        return graphMapper;
    }

    private void printDebugInformation() {
        if (!VISUALIZER.isInitialized()) return;
        for (Wizard wizard : world.getWizards()) {
//            if (wizard.isMaster()) {
                VISUALIZER.text(wizard.getX() + wizard.getRadius(),
                        wizard.getY() + wizard.getRadius(),
                        wizard.getX() + " - " + wizard.getY(),
                        Color.BLACK);
//                break;
//                if (Zones.HOME.contains(new Point2D(wizard.getX(), wizard.getY()))) {
//                    VISUALIZER.fillCircle(wizard.getX() - wizard.getRadius(), wizard.getY() + wizard.getRadius(), 2, Color.GREEN);
//                    VISUALIZER.drawZone(Zones.HOME, Color.GREEN);
//                } else if (Zones.TOP_FOREFRONT_1.contains(new Point2D(wizard.getX(), wizard.getY()))) {
//                    VISUALIZER.fillCircle(wizard.getX() - wizard.getRadius(), wizard.getY() + wizard.getRadius(), 2, new Color(255, 255, 0));
//                    VISUALIZER.drawZone(Zones.TOP_FOREFRONT_1, new Color(255, 255, 0));
//                } else if(Zones.TOP_FOREFRONT_2.contains(new Point2D(wizard.getX(), wizard.getY()))) {
//                    VISUALIZER.fillCircle(wizard.getX() - wizard.getRadius(), wizard.getY() + wizard.getRadius(), 2, Color.ORANGE);
//                    VISUALIZER.drawZone(Zones.TOP_FOREFRONT_2, Color.ORANGE);
//                } else {
//                    VISUALIZER.fillCircle(wizard.getX() - wizard.getRadius(), wizard.getY() + wizard.getRadius(), 2, Color.RED);
//                }
//            }
        }
    }

//    private void processMessages() {
//        if (self.isMaster()) {
//            List<LaneType> laneTypes = new ArrayList<>(Arrays.asList(LaneType.TOP, LaneType.MIDDLE, LaneType.BOTTOM));
//            laneTypes.remove(myLane);
//            for (int i = 0; i < 4; i++) {
//                laneTypes.addAll(Arrays.asList(LaneType.TOP, LaneType.MIDDLE, LaneType.BOTTOM));
//            }
//            Map<LaneType, List<Integer>> skillTypesByLanes = createSkillTypesByLane();
//
//            skillTypesByLanes.get(myLane).add(mySkillBranch);
//
//            Message[] messages = Arrays.stream(world.getPlayers())
//                    .filter(p -> !p.isMe())
//                    .map(p -> {
//                        LaneType currentLaneType;
//                        if (laneTypes.size() > 0) {
//                            currentLaneType = laneTypes.get(0);
//                            laneTypes.remove(0);
//                        } else {
//                            currentLaneType = LaneType.values()[random.nextInt(3)];
//                        }
//                        List<Integer> skillTypesOnCurrentLane = skillTypesByLanes.get(myLane);
//                        Integer notUsedSkillType = null;
//                        for (int i = 0; i < 5; i++) {
//                            if (!skillTypesOnCurrentLane.contains(i)) {
//                                notUsedSkillType = i;
//                                break;
//                            }
//                        }
//                        if (notUsedSkillType == null) {
//                            notUsedSkillType = random.nextInt(5);
//                        }
//
//                        return new Message(currentLaneType, skillBranches[notUsedSkillType][4], new byte[0]);
//                    }).toArray(Message[]::new);
//            move.setMessages(messages);
//
//
//        } else {
//            myLane = LaneType.TOP;
//            mySkillBranch = FROST_BOLT_BRANCH;
//            favoriteActionType = ActionType.FROST_BOLT;
//            damager = true;
//            Message[] messages = self.getMessages();
//            int myMessageIndex = ((Long) self.getId()).intValue();
//            if(myMessageIndex >= messages.length) {
//                System.err.println("Incorrect message index");
//                return;
//            }
//            Message messageForMe = messages[myMessageIndex];
//            if(messageForMe.getLane() != null) {
//                myLane = messageForMe.getLane();
//                System.out.println("Accepted lane from message");
//            }
//            switch (messageForMe.getSkillToLearn()) {
//                case ADVANCED_MAGIC_MISSILE:
//                    mySkillBranch = MISSILE_BRANCH;
//                    favoriteActionType = ActionType.MAGIC_MISSILE;
//                    damager = true;
//                    break;
//                case FROST_BOLT:
//                    mySkillBranch = FROST_BOLT_BRANCH;
//                    favoriteActionType = ActionType.FROST_BOLT;
//                    damager = true;
//                    break;
//                case FIREBALL:
//                    mySkillBranch = STAFF_AND_FIREBALL_BRANCH;
//                    favoriteActionType = ActionType.FIREBALL;
//                    damager = true;
//                    break;
//                case HASTE:
//                    mySkillBranch = HASTE_BRANCH;
//                    favoriteActionType = ActionType.HASTE;
//                    damager = false;
//                    break;
//                case SHIELD:
//                    mySkillBranch = SHIELD_BRANCH;
//                    favoriteActionType = ActionType.SHIELD;
//                    damager = false;
//                    break;
//                default:
//                    mySkillBranch = FROST_BOLT_BRANCH;
//                    favoriteActionType = ActionType.FROST_BOLT;
//                    damager = true;
//                    break;
//            }
//        }
//    }
//    private Map<LaneType, List<Integer>> createSkillTypesByLane() {
//        Map<LaneType, List<Integer>> skillTypesByLane = new HashMap<>();
//        skillTypesByLane.put(LaneType.TOP, new ArrayList<>());
//        skillTypesByLane.put(LaneType.BOTTOM, new ArrayList<>());
//        skillTypesByLane.put(LaneType.MIDDLE, new ArrayList<>());
//        return skillTypesByLane;
//    }
// TODO: 12.11.2016 вызывать во время wizard.remainingActionCooldownTicks > 0
// и если перспективное действие - перемещение
//    public Point2D getPerspectivePosition() {
// TODO: 12.11.2016 учесть позиции противников
// возможность спрятаться за деревом с учетом скорости перемещения и будет ли это иметь смысл
// посмотреть на окружающие бонусы
// важность бонуса защиты при малом количестве здоровья - больше
// если расстояние от противника до бонуса меньше и сокращается в течении n тиков в зависимости от типа бонуса отступать и применять действия
//        return null;
//    }
// TODO: 12.11.2016 если достаточно здоровья, количество атакующих противников * их способность атаковать в худшем случае
// TODO: 12.11.2016 в сумме дает возможность добить и убежать, кинуть зов о помощи при принятии решения продолжать
//    public void staffHit() {
//        move.setAction(ActionType.STAFF);
//    }
//todo приоритет зависит от минимума разницы урона и оставшегося здоровья противника
//    public void missilShot() {
//        move.setAction(ActionType.MAGIC_MISSILE);
//    }
//todo бросаем, если couldown 0 и мана есть если ключевой уничтожаемый персонж имеет большее количество здоровья или менее 30%
//todo основная задача - заморозить более сильного соперника или не дать убежать противнику без хп
//    public void frostBoltShot() {
//        move.setAction(ActionType.FROST_BOLT);
//    }
//todo если couldown 0 и мана есть и расстояние между двумя соперниками 80% от радиуса фаербола, обязательно один волшебник
//    public void fireballShot() {
//        move.setMaxCastDistance(0);//todo расстояние до центра между группой противников
//        move.setMinCastDistance(move.getMaxCastDistance() * 0.8);
//        move.setAction(ActionType.FIREBALL);
//    }
// todo ускорить союзника, если маны останется на 100% (ледяной болт) - скорость регенерации*общий кулдаун для ледяного болта, если его здоровье 15% и он среди 5 ближайших союзников к ближайшему противнику, если у самого более 80% здоровья
//todo если ускоренный союзник с учетом скорости и времени до конца регенерации маны успевает покинуть поле боя, то брать 100% для огненного болта, если ближайшее время его запуска не превышает время на покидание зоны бд союзника
//    public void doHaste(int targetId) {
//
//    }
//    public void doShield(int targetId) {
//
//    }
//    public int distanceToBonus() {
//
//    }
//todo EMPOWER усиление атаки, если количество союзников более 150% противников, то нужно
//todo HASTE если противников больше на 150% и рядом нет волшебников союзников, то используем для отсупления
//todo SHIELD аналогично HASTE
//    public boolean bonusIsNecessary() {
//
//    }
//    public boolean waitMinios() {
//        //todo если стоим вблизи башши, но миньоны отстали - ждем
//    }

    private void drawStatistic(Map<Zone, ZoneStatistic> statisticsByZones) {
        if (VISUALIZER == null || !VISUALIZER.isInitialized()) return;
        VISUALIZER.beginPre();
        for (Map.Entry<Zone, ZoneStatistic> statisticEntry : statisticsByZones.entrySet()) {
            if (VISUALIZER != null && VISUALIZER.isInitialized()) {
                double alliesEstimation = livingUnitsObjective.estimate(statisticEntry.getValue().getAllies());
                double enemiesEstimation = livingUnitsObjective.estimate(statisticEntry.getValue().getEnemies());
                Color zoneColor;
                if (alliesEstimation == 0 && enemiesEstimation == 0) {
                    zoneColor = Color.BLACK;
                } else if (enemiesEstimation > 0) {
                    double zoneEstimation = alliesEstimation / enemiesEstimation;
                    if (zoneEstimation > 1) {
                        zoneColor = Color.GREEN;
                    } else if (zoneEstimation == 1) {
                        zoneColor = Color.ORANGE;
                    } else {
                        zoneColor = Color.RED;
                    }
                } else if (alliesEstimation > 0) {
                    zoneColor = Color.GREEN;
                } else {
                    zoneColor = Color.BLACK;
                }
                VISUALIZER.drawZone(statisticEntry.getKey(), zoneColor);

//                printDebugInformation();
            }
        }

        Points.CHECK_POINT_EDGES.forEach(edge -> {
            VISUALIZER.drawVector(edge, Color.BLUE);
        });
        VISUALIZER.endPre();
    }

//    private Point2D getNextWaypoint() {
//        int lastWaypointIndex = waypoints.length - 1;
//        Point2D lastWaypoint = waypoints[lastWaypointIndex];
//
//        for (int waypointIndex = 0; waypointIndex < lastWaypointIndex; ++waypointIndex) {
//            Point2D waypoint = waypoints[waypointIndex];
//
//            if (waypoint.getDistanceTo(self) <= WAYPOINT_RADIUS) {
//                return waypoints[waypointIndex + 1];
//            }
//
//            if (lastWaypoint.getDistanceTo(waypoint) < lastWaypoint.getDistanceTo(self)) {
//                return waypoint;
//            }
//        }
//
//        return lastWaypoint;
//    }
//
//    private Point2D getNextWaypointToBonus() {
//        Point2D[] waypoints = waypointsToBonus.get(myLane);
//        int lastWaypointIndex = waypoints.length - 1;
//        Point2D lastWaypoint = waypoints[lastWaypointIndex];
//
//        for (int waypointIndex = 0; waypointIndex < lastWaypointIndex; ++waypointIndex) {
//            Point2D waypoint = waypoints[waypointIndex];
//
//            if (waypoint.getDistanceTo(self) <= WAYPOINT_RADIUS) {
//                return waypoints[waypointIndex + 1];
//            }
//
//            if (lastWaypoint.getDistanceTo(waypoint) < lastWaypoint.getDistanceTo(self)) {
//                return waypoint;
//            }
//        }
//
//        return lastWaypoint;
//    }
//
//    private Point2D getPreviousWaypoint() {
//        Point2D firstWaypoint = waypoints[0];
//
//        for (int waypointIndex = waypoints.length - 1; waypointIndex > 0; --waypointIndex) {
//            Point2D waypoint = waypoints[waypointIndex];
//
//            if (waypoint.getDistanceTo(self) <= WAYPOINT_RADIUS) {
//                return waypoints[waypointIndex - 1];
//            }
//
//            if (firstWaypoint.getDistanceTo(waypoint) < firstWaypoint.getDistanceTo(self)) {
//                return waypoint;
//            }
//        }
//
//        return firstWaypoint;
//    }
//
//    private Point2D getPreviousWaypointToBonus() {
//        Point2D[] waypoints = waypointsToBonus.get(myLane);
//        Point2D firstWaypoint = waypoints[0];
//
//        for (int waypointIndex = waypoints.length - 1; waypointIndex > 0; --waypointIndex) {
//            Point2D waypoint = waypoints[waypointIndex];
//
//            if (waypoint.getDistanceTo(self) <= WAYPOINT_RADIUS) {
//                return waypoints[waypointIndex - 1];
//            }
//
//            if (firstWaypoint.getDistanceTo(waypoint) < firstWaypoint.getDistanceTo(self)) {
//                return waypoint;
//            }
//        }
//
//        return firstWaypoint;
//    }


    private LivingUnit getNearestTarget() {
        List<LivingUnit> targets = statisticCollector.getLivingUnits();

        LivingUnit nearestTarget = null;
        double nearestTargetDistance = Double.MAX_VALUE;

        for (LivingUnit target : targets) {
            if (target.getFaction() == Faction.NEUTRAL || target.getFaction() == self.getFaction()) {
                continue;
            }

            double distance = self.getDistanceTo(target);

            if (distance < nearestTargetDistance) {
                nearestTarget = target;
                nearestTargetDistance = distance;
            }
        }
        return nearestTarget;
    }
}
