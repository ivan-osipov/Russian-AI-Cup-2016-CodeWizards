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
    private Point2D startPoint;
    private Point2D currentPosition;
    private List<Zone> capturedZones;
    private int currentZoneNumber;
    private EnumMap<WizardState, Behaviour> behaviours;

    private List<Point2D> wayToBonus;

    private StatisticCollector statisticCollector;

    private GameMapGraph graph = new GameMapGraph();
    private GraphMapper graphMapper = new GraphMapper();

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

    private Integer mySkillBranch = FROST_BOLT_BRANCH;

    private Integer mySecondSkillBranch = STAFF_AND_FIREBALL_BRANCH;

    private LivingUnitsObjective livingUnitsObjective = new LivingUnitsObjective();


    private void initializeStrategy() {
        if (wizardState == null) {
            VISUALIZER = new DebugVisualizer();

            wizardState = WizardState.WALKING;
            startPoint = new Point2D(self.getX(), self.getY());
            capturedZones = Arrays.asList(Zones.HOME, Zones.MIDDLE_FOREFRONT_1, Zones.MIDDLE_FOREFRONT_2, Zones.CENTER,
                    Zones.ENEMY_MIDDLE_FOREFRONT_2, Zones.ENEMY_MIDDLE_FOREFRONT_1/*, Zones.ENEMY_HOME*/);
            mySkillBranch = FROST_BOLT_BRANCH;
            mySecondSkillBranch = STAFF_AND_FIREBALL_BRANCH;

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
            behaviours = new EnumMap<>(WizardState.class);
            behaviours.put(WizardState.WALKING, new WalkingBehaviour(self, world, game, move, this));
            behaviours.put(WizardState.PROTECTION, new ProtectionBehaviour(self, world, game, move, this));
            behaviours.put(WizardState.PUSHING, new PushingBehaviour(self, world, game, move, this));
            behaviours.put(WizardState.BONUS_MINING, new BonusMiningBehaviour(self, world, game, move, this));
        }
        behaviours.values().forEach(b -> b.update(self, world, move));
        updateBonusStatus();
        currentPosition = new Point2D(self.getX(), self.getY());

        updateCurrentZoneNumber();
        statisticCollector = new StatisticCollector(self, world);
        drawStatistic(statisticCollector.collectZoneStatistic());
    }

    private void updateBonusStatus() {
        BonusMiningBehaviour bonusMiningBehaviour = (BonusMiningBehaviour) behaviours.get(WizardState.BONUS_MINING);
        List<Integer> bonusRespTimes = bonusMiningBehaviour.getBonusRespTimes();
        System.out.println("Next bonus time " + bonusRespTimes.get(0));
        if(!bonusRespTimes.isEmpty() && bonusRespTimes.get(0).equals(world.getTickIndex())) {
            bonusMiningBehaviour.nowTimeOfOccurrence();
            bonusRespTimes.remove(0);
        }
    }

    private void updateCurrentZoneNumber() {
        for (int i = 0; i < capturedZones.size(); i++) {
            if (capturedZones.get(i).contains(new Point2D(self.getX(), self.getY()))) {
                currentZoneNumber = i;
                return;
            }
        }
    }

    public int getCurrentZoneNumber() {
        return currentZoneNumber;
    }

    public Point2D getCurrentPosition() {
        return currentPosition;
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
        if (behaviour == null) {
            move.setAction(ActionType.NONE);
            System.err.println("Behaviour not found: " + wizardState);
        } else {
            behaviour.doIt();
        }
    }

    public EnumMap<WizardState, Behaviour> getBehaviours() {
        return behaviours;
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
            VISUALIZER.text(wizard.getX() + wizard.getRadius(),
                    wizard.getY() + wizard.getRadius(),
                    wizard.getX() + " - " + wizard.getY(),
                    Color.BLACK);
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
// важность бонуса защиты при малом количестве здоровья - больше
//todo если couldown 0 и мана есть и расстояние между двумя соперниками 80% от радиуса фаербола, обязательно один волшебник
//    public void fireballShot() {
//        move.setMaxCastDistance(0);//todo расстояние до центра между группой противников
//        move.setMinCastDistance(move.getMaxCastDistance() * 0.8);
//        move.setAction(ActionType.FIREBALL);
//    }
// todo ускорить союзника, если маны останется на 100% (ледяной болт) - скорость регенерации*общий кулдаун для ледяного болта, если его здоровье 15% и он среди 5 ближайших союзников к ближайшему противнику, если у самого более 80% здоровья
//todo если ускоренный союзник с учетом скорости и времени до конца регенерации маны успевает покинуть поле боя, то брать 100% для огненного болта, если ближайшее время его запуска не превышает время на покидание зоны бд союзника
//
//todo EMPOWER усиление атаки, если количество союзников более 150% противников, то нужно

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

    public void setWayToBonus(List<Point2D> wayToBonus) {
        this.wayToBonus = wayToBonus;
    }

    public List<Point2D> getWayToBonus() {
        return wayToBonus;
    }

    public Point2D getStartPoint() {
        return startPoint;
    }
}
