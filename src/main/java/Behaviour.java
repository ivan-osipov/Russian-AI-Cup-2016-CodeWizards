import model.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class Behaviour {
    private static final double LOW_HP_FACTOR = 0.2D;
    protected static final double POINT_RADIUS = 1.0D;
    protected static final double MAX_POINT_RADIUS = 5.0D;

    protected Wizard self;
    protected World world;
    protected Game game;
    protected Move move;
    protected Point2D currentPosition;

    protected MyStrategy strategy;


    private Point2D previousPoint = new Point2D(0, 0);
    private int stayingTime = 0;

    public Behaviour(Wizard self, World world, Game game, Move move, MyStrategy strategy) {
        this.self = self;
        this.world = world;
        this.game = game;
        this.move = move;
        this.strategy = strategy;
    }

    public void update(Wizard self, World world, Move move) {
        this.self = self;
        this.world = world;
        this.move = move;
        this.currentPosition = new Point2D(self.getX(), self.getY());
    }

    abstract void perform();

    public void doIt() {
//        System.out.println(self.getX() + "   " + self.getY());
//        if (!(this instanceof WalkingBehaviour) && strategy.getStartPoint().equals(currentPosition)) {
//            strategy.setWizardState(WizardState.WALKING);
//            return;
//        }
        if (isDangerousAround()) return;
        double wayTimeToBonus = calculateTimeForWayToBonus();
        int remainedTimeToBonusAppearance = game.getBonusAppearanceIntervalTicks() -
                (world.getTickIndex() % game.getBonusAppearanceIntervalTicks());
        if (remainedTimeToBonusAppearance <= wayTimeToBonus) {
            BonusMiningBehaviour behaviour = (BonusMiningBehaviour) strategy.getBehaviours().get(WizardState.BONUS_MINING);
            behaviour.setPreIterationState(true);
            strategy.setWizardState(WizardState.BONUS_MINING);
            strategy.setWayToBonus(calculateWayToBonus());
            return;
        }

        perform();
    }

    protected boolean isDangerousAround() {
        Minion nearestDangerousMinion = minionsAreNear();
        if (enemiesCanKill() || nearestDangerousMinion != null) {
            if (nearestDangerousMinion != null) {
                double angleToNearestMinion = self.getAngleTo(nearestDangerousMinion);
                if (angleToNearestMinion >= -Math.PI / 2 && angleToNearestMinion <= Math.PI / 2) {
                    goBackByLookAngle(angleToNearestMinion);
                    return true;
                } else {
                    goByLookAngle(angleToNearestMinion);
                    return true;
                }
            }
            if (!safe() && !nextSafe()) {
                Zone nearSafeZone = findNearSafeZone();
                goBack(nearSafeZone.getCentroid());
                return true;
            }
        }
        return false;
    }

    protected double calculateTimeForWayToBonus() {
        List<Point2D> wayToBonus = calculateWayToBonus();
        if (wayToBonus.isEmpty()) {
            return POINT_RADIUS;
        }
        return ((distanceByWayPoint(wayToBonus) - game.getBonusRadius() - getForwardSpeed()) / getForwardSpeed()) /*+ (overheadForTurns(wayToBonus) / 2)*/;
    }

    protected double distanceByWayPoint(List<Point2D> bestWayPoints) {
        double distance = 0;
        for (int i = 0; i < bestWayPoints.size() - 1; i++) {
            distance += bestWayPoints.get(i).getDistanceTo(bestWayPoints.get(i + 1));
        }
        return distance;
    }

    protected double overheadForTurns(List<Point2D> bestWayPoints) {
        double turnOverhead = 0;
        double speedOfTurn = game.getWizardMaxTurnAngle();
        Point2D firstPoint = bestWayPoints.get(0);
        double angle = self.getAngle();
        double angleToFirstPoint = self.getAngleTo(firstPoint.getX(), firstPoint.getY());
        angle += angleToFirstPoint;
        double ticksForTurn = Math.abs(angleToFirstPoint) / speedOfTurn;
        turnOverhead += ticksForTurn;
        for (int i = 0; i < bestWayPoints.size() - 1; i++) {
            double turnAngle = bestWayPoints.get(i).getAngleTo(bestWayPoints.get(i + 1), angle);
            turnOverhead += Math.abs(turnAngle) / speedOfTurn;
            angle += turnAngle;
        }
        return turnOverhead;
    }

    protected List<Point2D> calculateWayToBonus() {
        Point2D targetPoint = Points.LOWER_BONUS_POINT.getDistanceTo(self) <= Points.UPPER_BONUS_POINT.getDistanceTo(self)
                ? Points.LOWER_BONUS_POINT
                : Points.UPPER_BONUS_POINT;
        return calculateWayToBonus(targetPoint);
    }

    protected List<Point2D> calculateWayToBonus(Point2D targetPoint) {
        Point2D selfPoint = new Point2D(self.getX(), self.getY());
        Point2D nearToSelfGraphPoint = Points.CHECK_POINTS.get(0);
        for (Point2D checkPoint : Points.CHECK_POINTS) {
            if (checkPoint.getDistanceTo(selfPoint) < nearToSelfGraphPoint.getDistanceTo(selfPoint)) {
                nearToSelfGraphPoint = checkPoint;
            }
        }
        if (nearToSelfGraphPoint.getDistanceTo(targetPoint) > POINT_RADIUS) {
            GraphMapper graphMapper = strategy.getGraphMapper().copy();
            GameMapGraph graph = strategy.getGraph().copy();
            addEdgeBetweenAbsoluteAndGraphPoint(selfPoint, nearToSelfGraphPoint, graphMapper, graph);
            List<GameMapGraph.Node> bestWay = graph
                    .findBestWayDijkstra(
                            graphMapper.map(nearToSelfGraphPoint),
                            graphMapper.map(targetPoint));
            return graphMapper.map(bestWay);
        }
        return Collections.emptyList();
    }

    protected void addEdgeBetweenAbsoluteAndGraphPoint(Point2D absPoint, Point2D graphPoint, GraphMapper graphMapper, GameMapGraph graph) {
        if (absPoint.getDistanceTo(graphPoint) < MAX_POINT_RADIUS) {
            GameMapGraph.Node targetNode = graphMapper.map(absPoint);
            graph.addNode(targetNode);
            Vector2D targetToNearEdge = new Vector2D(absPoint, graphPoint);
            GameMapGraph.Edge targetEdge = graphMapper.map(targetToNearEdge);
            graph.addEdge(targetEdge);
        }
    }

    protected boolean enemiesCanKill() {
        if (self.getLife() > 0.7 * self.getMaxLife()) {
            return false;
        }
        List<LivingUnit> enemies = getNearDangerousEnemies();
        int ticksInReserve = 5;
        double potentialDamage = 0;
        for (LivingUnit enemy : enemies) {
            if (enemy instanceof Building) {
                Building building = (Building) enemy;
                if (building.getType() == BuildingType.GUARDIAN_TOWER) {
                    double enemyAttackRange = game.getGuardianTowerAttackRange();
                    double distanceToMe = building.getDistanceTo(self);
                    double timeForRetreat = getTimeForRetreat(enemyAttackRange, distanceToMe, ticksInReserve);
                    if(timeForRetreat == 0) {
                        continue;
                    }
                    if (timeForRetreat < building.getRemainingActionCooldownTicks()) {
                        potentialDamage += game.getGuardianTowerDamage();
                    }
                }
            } else if (enemy instanceof Wizard) {
                Wizard wizard = (Wizard) enemy;
                List<SkillType> skillTypes = Arrays.asList(wizard.getSkills());
                double wizardDamage = 0;
                double distanceToMe = wizard.getDistanceTo(self);
                double timeForRetreat = getTimeForRetreat(game.getWizardCastRange(), distanceToMe, ticksInReserve);
                if(timeForRetreat == 0) {
                    continue;
                }
                if (timeForRetreat < wizard.getRemainingActionCooldownTicks()) {
                    double manaAdd = calculateManaAddAfterTicks(ticksInReserve, wizard.getLevel());
                    if (skillTypes.contains(SkillType.FROST_BOLT)
                            && wizard.getMana() + manaAdd >= game.getFrostBoltManacost()
                            && wizard.getRemainingCooldownTicksByAction()[ActionType.FROST_BOLT.ordinal()] < timeForRetreat) {
                        wizardDamage = Math.max(game.getFrostBoltDirectDamage(), wizardDamage);
                    }
                    if (skillTypes.contains(SkillType.FIREBALL)
                            && wizard.getMana() + manaAdd >= game.getFireballManacost()
                            && wizard.getRemainingCooldownTicksByAction()[ActionType.FIREBALL.ordinal()] < timeForRetreat) {
                        wizardDamage = Math.max(game.getFireballExplosionMaxDamage(), wizardDamage);
                    }
                    if (wizard.getMana() + manaAdd >= game.getMagicMissileManacost()) {
                        wizardDamage = Math.max(game.getMagicMissileDirectDamage(), wizardDamage);
                    }
                    potentialDamage += wizardDamage;
                }
            }
        }
        return potentialDamage >= minLife();
    }

    private double minLife() {
        return self.getLife() - self.getMaxLife() * LOW_HP_FACTOR;
    }

    private double calculateManaAddAfterTicks(int ticks, int wizardLevel) {
        double wizardManaRegeneration = game.getWizardBaseManaRegeneration();
        wizardManaRegeneration += game.getWizardManaRegenerationGrowthPerLevel() * (wizardLevel - 1);
        return wizardManaRegeneration;
    }

    protected double calculateMaxRange(List<LivingUnit> enemies) {
        double maxRange = 0;
        for (LivingUnit enemy : enemies) {
            if(enemy instanceof Building) {
                Building building = (Building) enemy;
                if(building.getType() == BuildingType.FACTION_BASE) {
                    maxRange = Math.max(maxRange, game.getFactionBaseAttackRange());
                } else {
                    maxRange = Math.max(maxRange, game.getGuardianTowerAttackRange());
                }
            } else if(enemy instanceof Wizard) {
                maxRange = Math.max(maxRange, game.getWizardCastRange());
            } else if(enemy instanceof Minion) {
                Minion minion = (Minion) enemy;
                if(minion.getType() == MinionType.ORC_WOODCUTTER) {
                    maxRange = Math.max(maxRange, game.getOrcWoodcutterAttackRange());
                } else {
                    maxRange = Math.max(maxRange, game.getFetishBlowdartAttackRange());
                }
            } else {
                System.err.println("Unknown enemy type: " + enemy.getClass());
            }
        }
        return maxRange;
    }

    /**
     * negative value
     */
    private double getTimeForRetreat(double enemyAttackRange, double distanceToMe, int ticksInReserve) {
        double distanceToSave = enemyAttackRange - distanceToMe + getBackwardSpeed()*ticksInReserve;
        if(distanceToSave < 0) {
            return 0;
        }
        return (distanceToSave / getBackwardSpeed());
    }

    private List<LivingUnit> getNearDangerousEnemies() {
        Zone myZone = strategy.getCapturedZones().get(strategy.getCurrentZoneNumber());
        List<LivingUnit> enemies = strategy.getZoneStatistics().get(myZone).getEnemies();
        if (validZone(strategy.getCurrentZoneNumber() + 1)) {
            Zone nextZone = strategy.getCapturedZones().get(strategy.getCurrentZoneNumber() + 1);
            enemies.addAll(strategy.getZoneStatistics().get(nextZone).getEnemies());
        }
        enemies.sort(new NearestToSelfComparator(self));
        return enemies;
    }

    private List<LivingUnit> getNearHelpfulAllies() {
        Zone selfZone = strategy.getCapturedZones().get(strategy.getCurrentZoneNumber());
        List<LivingUnit> allies = strategy.getZoneStatistics().get(selfZone).getAllies();
        if (validZone(strategy.getCurrentZoneNumber() + 1)) {
            Zone nextZone = strategy.getCapturedZones().get(strategy.getCurrentZoneNumber() + 1);
            allies.addAll(strategy.getZoneStatistics().get(nextZone).getAllies());
        }
        return allies;
    }

    protected boolean safe() {
        Zone myZone = strategy.getCapturedZones().get(strategy.getCurrentZoneNumber());
        return strategy.getZoneStatistics().get(myZone).getEnemies().size() == 0;
    }

    protected boolean nextSafe() {
        if (validZone(strategy.getCurrentZoneNumber() + 1)) {
            Zone myZone = strategy.getCapturedZones().get(strategy.getCurrentZoneNumber() + 1);
            Map<Zone, ZoneStatistic> zoneStatistic = strategy.getZoneStatistics();
            return zoneStatistic.get(myZone).getEnemies().size() == 0;
        } else {
            return false;
        }
    }

    protected boolean safeByNumber(int num) {
        if (validZone(num)) {
            Zone myZone = strategy.getCapturedZones().get(num);
            Map<Zone, ZoneStatistic> zoneStatistic = strategy.getZoneStatistics();
            return zoneStatistic.get(myZone).getEnemies().size() == 0;
        } else {
            return false;
        }
    }

    protected boolean validZone(int zoneNum) {
        return zoneNum < strategy.getCapturedZones().size();
    }

    protected void goTo(Point2D point) {
        double angle = self.getAngleTo(point.getX(), point.getY());
        goByLookAngle(angle);
    }

    private boolean killNearTree() {
        Tree[] trees = world.getTrees();
        Tree nearTree = findNearTree(trees);
        if (nearTree == null) {
            return false;
        }
        double angleToTree = self.getAngleTo(nearTree);
        move.setTurn(angleToTree);
        if (StrictMath.abs(angleToTree) < game.getStaffSector() / 2.0D) {
            move.setCastAngle(angleToTree);
            move.setAction(ActionType.MAGIC_MISSILE);
        }
        return true;
    }

    public Tree findNearTree(Tree... trees) {
        return Arrays.stream(trees)
                .min((t1, t2) -> Double.compare(t1.getDistanceTo(self), t2.getDistanceTo(self)))
                .orElse(null);
    }

    protected void updateStayingTime() {
        if (previousPoint.equals(strategy.getCurrentPosition())) {
            stayingTime++;
        } else {
            stayingTime = 0;
            previousPoint = strategy.getCurrentPosition();
        }
        System.out.println("Staying time: " + stayingTime);
    }

    protected void goBack(Point2D backPoint) {
        double angle = self.getAngleTo(backPoint.getX(), backPoint.getY());
        double lookAngle = angle > 0 ? angle - Math.PI : angle + Math.PI;
        goBackByLookAngle(lookAngle);
    }

    protected void goBackByLookAngle(double lookAngle) {
        updateStayingTime();
        if (stayingTime > 10) {
            LivingUnit enemy = findNearEnemy();
            if (enemy != null) {
                double distance = enemy.getDistanceTo(self);
                if (distance <= game.getWizardCastRange()) {
                    double angle = self.getAngleTo(enemy);
                    move.setTurn(angle);
                    if (StrictMath.abs(angle) < game.getStaffSector() / 2.0D) {
                        move.setCastAngle(angle);
                        move.setMinCastDistance(distance - enemy.getRadius() + game.getMagicMissileRadius());

                        if (game.isSkillsEnabled()) {
                            int frostBoltRemainingCooldown = self.getRemainingCooldownTicksByAction()[ActionType.FROST_BOLT.ordinal()];
                            int maxCooldown = Math.max(frostBoltRemainingCooldown, self.getRemainingActionCooldownTicks());
                            if ((self.getLevel() >= 6) && maxCooldown == 0) {
                                move.setAction(ActionType.FROST_BOLT);
                                return;
                            }
                        }
                        if (self.getRemainingCooldownTicksByAction()[ActionType.MAGIC_MISSILE.ordinal()] == 0) {
                            move.setAction(ActionType.MAGIC_MISSILE);
                            return;
                        } else {
                            move.setAction(ActionType.STAFF);
                            return;
                        }
                    }
                }
            }
        }
        updateStayingTime();
        move.setTurn(lookAngle);
        if (StrictMath.abs(lookAngle) < game.getStaffSector() / 4.0D) {
            move.setSpeed(-getBackwardSpeed());
            move.setStrafeSpeed(calculateBackwardStrafeSpeed(move.getSpeed()));
        }
    }

    protected void goByLookAngle(double lookAngle) {
        updateStayingTime();
        if (stayingTime > 10) {
            LivingUnit enemy = findNearEnemy();
            if (enemy != null) {
                double distance = enemy.getDistanceTo(self);
                if (distance <= game.getWizardCastRange()) {
                    double angle = self.getAngleTo(enemy);
                    move.setTurn(angle);
                    if (StrictMath.abs(angle) < game.getStaffSector() / 2.0D) {
                        move.setCastAngle(angle);
                        move.setMinCastDistance(distance - enemy.getRadius() + game.getMagicMissileRadius());

                        if (game.isSkillsEnabled()) {
                            int frostBoltRemainingCooldown = self.getRemainingCooldownTicksByAction()[ActionType.FROST_BOLT.ordinal()];
                            int maxCooldown = Math.max(frostBoltRemainingCooldown, self.getRemainingActionCooldownTicks());
                            if ((self.getLevel() >= 6) && maxCooldown == 0) {
                                move.setAction(ActionType.FROST_BOLT);
                                return;
                            }
                        }
                        move.setAction(ActionType.MAGIC_MISSILE);
                    }
                }
            } else {
                boolean successful = killNearTree();
                if (!successful) {
                    System.out.println("Not solves");
                    return;
                }
            }
        }
        updateStayingTime();
        move.setTurn(lookAngle);
        if (StrictMath.abs(lookAngle) < game.getStaffSector() / 4.0D) {
            move.setSpeed(getForwardSpeed());
            double strafeSpeed = calculateForwardStrafeSpeed(move.getSpeed());
            move.setStrafeSpeed(strafeSpeed);
            if (strafeSpeed != 0) {
                move.setSpeed(0);
            }
        }
    }

    protected LivingUnit findNearEnemy() {
        return Stream.concat(
                Arrays.stream(world.getWizards()).filter(wizard -> wizard.getFaction() != self.getFaction()),
                Arrays.stream(world.getMinions()).filter(minion -> minion.getFaction() != self.getFaction() && minion.getFaction() != Faction.NEUTRAL)
        ).min((e1, e2) -> Double.compare(e1.getDistanceTo(self), e2.getDistanceTo(self))).orElse(null);
    }

    protected double calculateForwardStrafeSpeed(double speed) {
        int leftUnitsAmount = 0;
        int rightUnitsAmount = 0;
        List<LivingUnit> forwardObstacles = findForwardObstacles(speed);
        for (LivingUnit livingUnit : forwardObstacles) {
            if (isMe(livingUnit)) continue;
            double angleToUnit = self.getAngleTo(livingUnit);
            if (angleToUnit < 0 && angleToUnit > -Math.PI / 2) {
                leftUnitsAmount++;
            } else if (angleToUnit > 0 && angleToUnit < Math.PI / 2) {
                rightUnitsAmount++;
            }
        }
        return game.getWizardStrafeSpeed() * Integer.compare(leftUnitsAmount, rightUnitsAmount);
    }

    private boolean isMe(LivingUnit livingUnit) {
        return livingUnit instanceof Wizard && ((Wizard) livingUnit).isMe();
    }

    protected double calculateBackwardStrafeSpeed(double speed) {
        int leftUnitsAmount = 0;
        int rightUnitsAmount = 0;
        List<LivingUnit> forwardObstacles = findForwardObstacles(speed);
        for (LivingUnit livingUnit : forwardObstacles) {
            double angleToUnit = self.getAngleTo(livingUnit);
            if (angleToUnit >= -Math.PI && angleToUnit < -Math.PI / 2) {
                leftUnitsAmount++;
            } else if (angleToUnit <= Math.PI && angleToUnit > Math.PI / 2) {
                rightUnitsAmount++;
            }
        }
        return game.getWizardStrafeSpeed() * Integer.compare(leftUnitsAmount, rightUnitsAmount);
    }

    protected List<LivingUnit> findForwardObstacles(double speed) {
        return collectAllLivingUnits().stream()
                .filter(unit -> unit.getDistanceTo(self) < speed * 2 + self.getRadius() + unit.getRadius())
                .collect(Collectors.toList());
    }

    protected List<LivingUnit> collectAllLivingUnits() {
        List<LivingUnit> livingUnits = new ArrayList<>();
        livingUnits.addAll(Arrays.asList(world.getWizards()));
        livingUnits.addAll(Arrays.asList(world.getMinions()));
        livingUnits.addAll(Arrays.asList(world.getTrees()));
        livingUnits.addAll(Arrays.asList(world.getBuildings()));
        return livingUnits;
    }

    protected Zone findNearSafeZone() {
        return Zones.SAVE_ZONES.stream().filter(zone -> strategy.getZoneStatistics().get(zone).getEnemies().isEmpty())
                .min((z1, z2) -> Double.compare(z1.getCentroid().getDistanceTo(self), z2.getCentroid().getDistanceTo(self)))
                .orElse(Zones.LAST_HOME);
    }

    private double getBackwardSpeed() {
        return game.getWizardBackwardSpeed() * speedMultiplier();
    }

    private double getForwardSpeed() {
        return game.getWizardForwardSpeed() * speedMultiplier();
    }

    private double speedMultiplier() {
        return hasSpeedUp() ? 1.0 + game.getHastenedRotationBonusFactor() : 1.0;
    }

    private boolean hasSpeedUp() {
        Status[] statuses = self.getStatuses();
        for (Status status : statuses) {
            if (status.getType() == StatusType.HASTENED) {
                return true;
            }
        }
        return false;
    }

    private Minion minionsAreNear() {
        Minion nearMinion = null;
        double distanceToNearstMinion = Double.MAX_VALUE / 2;
        for (Minion minion : world.getMinions()) {
            if (minion.getFaction() == self.getFaction() || minion.getFaction() == Faction.NEUTRAL) continue;
            double criticalDistance;
            if (MinionType.ORC_WOODCUTTER == minion.getType()) {
                criticalDistance = game.getOrcWoodcutterAttackRange() + minion.getRadius() + self.getRadius();
            } else {
                criticalDistance = game.getFetishBlowdartAttackRange();
            }
            double distanceToMinion = self.getDistanceTo(minion);
            if (distanceToMinion <= criticalDistance + 10) {
                if (nearMinion == null || distanceToMinion < distanceToNearstMinion) {
                    distanceToNearstMinion = distanceToMinion;
                    nearMinion = minion;
                }
            }

        }
        return nearMinion;
    }

    protected Point2D getPreviousWaypoint(List<Point2D> waypoints) {
        Point2D firstWaypoint = waypoints.get(0);

        for (int waypointIndex = waypoints.size() - 1; waypointIndex > 0; --waypointIndex) {
            Point2D waypoint = waypoints.get(waypointIndex);

            if (waypoint.getDistanceTo(self) <= POINT_RADIUS) {
                return waypoints.get(waypointIndex - 1);
            }

            if (firstWaypoint.getDistanceTo(waypoint) < firstWaypoint.getDistanceTo(self)) {
                return waypoint;
            }
        }

        return firstWaypoint;
    }

    protected Point2D getNextWaypoint(List<Point2D> waypoints) {
        int lastWaypointIndex = waypoints.size() - 1;
        Point2D lastWaypoint = waypoints.get(lastWaypointIndex);

        for (int waypointIndex = 0; waypointIndex < lastWaypointIndex; ++waypointIndex) {
            Point2D waypoint = waypoints.get(waypointIndex);

            if (waypoint.getDistanceTo(self) <= POINT_RADIUS) {
                return waypoints.get(waypointIndex + 1);
            }

            if (lastWaypoint.getDistanceTo(waypoint) < lastWaypoint.getDistanceTo(self)) {
                return waypoint;
            }
        }

        return lastWaypoint;
    }

    protected List<Point2D> getReplaceLastPoint(List<Point2D> waypoints, Point2D newPoint) {
        List<Point2D> points = new ArrayList<>(waypoints);
        points.remove(waypoints.size() - 1);
        points.add(newPoint);
        return points;
    }

    public List<Point2D> calculateWayPointsByGraph(Point2D target) {
        Point2D selfPoint = strategy.getCurrentPosition();
        Point2D nearToSelfGraphPoint = Points.CHECK_POINTS.get(0);
        Point2D nearToTargetGraphPoint = Points.CHECK_POINTS.get(0);
        for (Point2D checkPoint : Points.CHECK_POINTS) {
            if (checkPoint.getDistanceTo(selfPoint) < nearToSelfGraphPoint.getDistanceTo(selfPoint)) {
                nearToSelfGraphPoint = checkPoint;
            }
            if (checkPoint.getDistanceTo(target) < nearToTargetGraphPoint.getDistanceTo(target)) {
                nearToTargetGraphPoint = checkPoint;
            }
        }
        if (nearToSelfGraphPoint.getDistanceTo(nearToTargetGraphPoint) > POINT_RADIUS) {
            GraphMapper graphMapper = strategy.getGraphMapper().copy();
            GameMapGraph graph = strategy.getGraph().copy();
            addEdgeBetweenAbsoluteAndGraphPoint(selfPoint, nearToSelfGraphPoint, graphMapper, graph);
            addEdgeBetweenAbsoluteAndGraphPoint(target, nearToTargetGraphPoint, graphMapper, graph);
            List<GameMapGraph.Node> bestWay = graph
                    .findBestWayDijkstra(
                            graphMapper.map(nearToSelfGraphPoint),
                            graphMapper.map(nearToTargetGraphPoint));
            return graphMapper.map(bestWay);
        } else {
            return new ArrayList<>(Collections.singletonList(target));
        }
    }

}
