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
    private Map<Zone, ZoneStatistic> zoneStatistics;

    private WizardState wizardState;
    private Point2D startPoint;
    private Point2D currentPosition;
    private List<Zone> capturedZones;
    private int currentZoneNumber;
    private int battleZoneNumber;
    private EnumMap<WizardState, Behaviour> behaviours;

    private List<Point2D> wayToBonus;

    private StatisticCollector statisticCollector;

    private GameMapGraph graph = new GameMapGraph();
    private GraphMapper graphMapper = new GraphMapper();

    private int bonusIteration;

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
        long time = System.currentTimeMillis();
        if (wizardState == null) {
            VISUALIZER = new DebugVisualizer();

            wizardState = WizardState.WALKING;
            startPoint = new Point2D(self.getX(), self.getY());
            capturedZones = Arrays.asList(Zones.HOME, Zones.MIDDLE_FOREFRONT_1, Zones.MIDDLE_FOREFRONT_2, Zones.CENTER,
                    Zones.ENEMY_MIDDLE_FOREFRONT_2, Zones.ENEMY_MIDDLE_FOREFRONT_1, Zones.ENEMY_HOME, Zones.ENEMY_LAST_HOME);
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
        statisticCollector = new StatisticCollector(self, world);
        behaviours.values().forEach(b -> b.update(self, world, move));
        zoneStatistics = statisticCollector.collectZoneStatistic();
        currentPosition = new Point2D(self.getX(), self.getY());

        updateCurrentZoneNumber();
        updateBattleZoneNumber();
        drawStatistic(statisticCollector.collectZoneStatistic());
        System.out.println("Time for tick" + (System.currentTimeMillis() - time));
    }

    public Map<Zone, ZoneStatistic> getZoneStatistics() {
        return zoneStatistics;
    }

    private void updateCurrentZoneNumber() {
        for (int i = 0; i < capturedZones.size(); i++) {
            if (capturedZones.get(i).contains(new Point2D(self))) {
                currentZoneNumber = i;
                return;
            }
        }
    }

    private void updateBattleZoneNumber() {
        Map<Zone, ZoneStatistic> zoneStatistic = statisticCollector.collectZoneStatistic();
        for (int i = 0; i < capturedZones.size(); i++) {
            if(zoneStatistic.get(capturedZones.get(i)).getEnemies().size() > 0) {
                battleZoneNumber = i;
                return;
            }
        }
    }

    public int getCurrentZoneNumber() {
        return currentZoneNumber;
    }

    public int getBattleZoneNumber() {
        return battleZoneNumber;
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
