import model.*;

import java.util.*;

public class BonusMiningBehaviour extends Behaviour {

    private Point2D lowerWaitingPoint = new Point2D(2750, 2750);

    private Point2D upperWaitingPoint = new Point2D(1250, 1250);

    private Zone firstBonusZone;

    private Integer currentBonusIteration;

    private boolean preIterationState = true;

    private final List<BonusIteration> bonusIterations = new ArrayList<>(Arrays
            .asList(new BonusIteration(2501), new BonusIteration(5001), new BonusIteration(7501),
                    new BonusIteration(10001), new BonusIteration(12501), new BonusIteration(15001),
                    new BonusIteration(17501), new BonusIteration(20001)));
    private final Map<Integer, BonusIteration> bonusIterationMap = new HashMap<>();

    public BonusMiningBehaviour(Wizard self, World world, Game game, Move move, MyStrategy strategy) {
        super(self, world, game, move, strategy);
        for (int i = 0; i < bonusIterations.size(); i++) {
            bonusIterationMap.put(bonusIterations.get(i).getShowTime(), bonusIterations.get(i));
        }
    }

    public void updateTimeOfOccurrence() {
        BonusIteration bonusIteration = bonusIterationMap.get(world.getTickIndex());
        if(bonusIteration != null) {
            bonusIteration.nowShowTime();
        }
    }

    @Override
    public void doIt() {
        if (isDangerousAround()) return;
        currentBonusIteration = calculateCurrentBonusIteration();
        updateTimeOfOccurrence();
        if (Zones.HOME.contains(strategy.getCurrentPosition()) || Zones.atJungle(strategy.getCurrentPosition())) {//если умер или застрял
            if(currentBonusIteration != null) {
                System.out.println("Bonus mining iteration " + currentBonusIteration + " is interrupted");
//                bonusIterations.get(currentBonusIteration).setState(BonusMiningState.INTERRUPTED);
                bonusIterations.get(currentBonusIteration).setState(BonusMiningState.END_OF_MINING);
            }
            System.out.println("Bonus mining is declined");
            strategy.setWizardState(WizardState.WALKING);
        }
        perform();
    }

    @Override
    void perform() {
        System.out.println("Bonus mining in progress");
        List<Point2D> wayToBonus = strategy.getWayToBonus();
        Point2D bonusPoint = wayToBonus.get(wayToBonus.size() - 1);
        System.out.println("Current bonus iteration: " + currentBonusIteration);
        BonusIteration bonusIteration = bonusIterations.get(currentBonusIteration);
        System.out.println("State: " + bonusIteration.getState());
        switch (bonusIteration.getState()) {
            case HOPE_OF_TAKE:
                if (bonusIterationMap.keySet().contains(world.getTickCount())) {
                    bonusIteration.setState(BonusMiningState.FIND_FIRST);
                    break;
                }
                if (Points.LOWER_BONUS_POINT.equals(bonusPoint)) {
                    wayToBonus = getReplaceLastPoint(wayToBonus, lowerWaitingPoint);
                    Point2D lastPoint = wayToBonus.get(wayToBonus.size() - 1);
                    if (lastPoint.getDistanceTo(self) < POINT_RADIUS) {
                        move.setTurn(self.getAngleTo(lastPoint.getX(), lastPoint.getY()));
                    } else {
                        goTo(getNextWaypoint(wayToBonus));
                    }
                } else {
                    Point2D lastPoint = wayToBonus.get(wayToBonus.size() - 1);
                    wayToBonus = getReplaceLastPoint(wayToBonus, upperWaitingPoint);
                    if (lastPoint.getDistanceTo(self) < POINT_RADIUS) {
                        move.setTurn(self.getAngleTo(lastPoint.getX(), lastPoint.getY()));
                    } else {
                        goTo(getNextWaypoint(wayToBonus));
                    }
                }
                break;
            case FIND_FIRST:
                preIterationState = false;
                updateBonusExistsInformation(currentBonusIteration);
                boolean lowerBonusIsAvailable = bonusIteration.isLowerBonusExists() && !bonusIteration.isLowerBonusHasPotentialOwner();
                boolean upperBonusIsAvailable = bonusIteration.isUpperBonusExists() && !bonusIteration.isUpperBonusHasPotentialOwner();
                if (Points.LOWER_BONUS_POINT.equals(bonusPoint)) {
                    firstBonusZone = Zones.MIDDLE_BOTTOM_TRANSITION;
                    if (lowerBonusIsAvailable) {
                        if (!wayToBonus.isEmpty()) {
                            goTo(getNextWaypoint(wayToBonus));
                        } else {
                            goTo(nearBonus());
                        }
                    } else if (upperBonusIsAvailable) {
                        double owningCoef = estimateUnits(Zones.CENTER);
                        if (strategy.getZoneStatistics().get(Zones.CENTER).getEnemies().size() == 0 || owningCoef > 2) {
                            System.out.println("Center is clear");
                            bonusIteration.setState(BonusMiningState.FIND_SECOND);
                        } else {
                            System.out.println("Need center protection!");
//                            bonusIteration.setState(BonusMiningState.INTERRUPTED);
                            bonusIteration.setState(BonusMiningState.END_OF_MINING);
                        }
                    } else {
                        System.out.println("Iteration number " + bonusIteration + " is finishing");
                        bonusIteration.setState(BonusMiningState.END_OF_MINING);
                    }
                } else {
                    firstBonusZone = Zones.TOP_MIDDLE_TRANSITION;
                    if (upperBonusIsAvailable) {
                        if (!wayToBonus.isEmpty()) {
                            goTo(getNextWaypoint(wayToBonus));
                        } else {
                            goTo(nearBonus());
                        }
                    } else if (lowerBonusIsAvailable) {
                        double owningCoef = estimateUnits(Zones.CENTER);
                        if (strategy.getZoneStatistics().get(Zones.CENTER).getEnemies().size() == 0 || owningCoef > 2) {
                            System.out.println("Go to next bonus");
                            bonusIteration.setState(BonusMiningState.FIND_SECOND);
                        } else {
                            System.out.println("Need center protection!");
//                            bonusIteration.setState(BonusMiningState.INTERRUPTED);
                            bonusIteration.setState(BonusMiningState.END_OF_MINING);
                        }
                    } else {
                        System.out.println("Iteration number " + bonusIteration + " is finishing");
                        bonusIteration.setState(BonusMiningState.END_OF_MINING);
                    }
                }
                break;
            case INTERRUPTED:
                double owningCoef = estimateUnits(Zones.CENTER);
                if (strategy.getZoneStatistics().get(Zones.CENTER).getEnemies().size() == 0 || owningCoef > 2) {
                    System.out.println("Stop interrupting");
                    bonusIteration.setState(BonusMiningState.FIND_SECOND);
                } else {
                    if (!Zones.CENTER.contains(new Point2D(self))) {
                        System.out.println("Go to center");
                        goTo(Zones.CENTER.getCentroid());
                    } else {
                        System.out.println("Bonus mining is interrupted");
                        strategy.setWizardState(WizardState.WALKING);
                    }
                }
                break;
            case END_OF_MINING:
                if (!Zones.CENTER.contains(new Point2D(self.getX(), self.getY()))) {
                    goTo(Zones.CENTER.getCentroid());
                } else {
                    strategy.setWizardState(WizardState.WALKING);
                }
                break;
            case FIND_SECOND:
                updateBonusExistsInformation(currentBonusIteration);
                lowerBonusIsAvailable = bonusIteration.isLowerBonusExists() && !bonusIteration.isLowerBonusHasPotentialOwner();
                upperBonusIsAvailable = bonusIteration.isUpperBonusExists() && !bonusIteration.isUpperBonusHasPotentialOwner();
                Point2D nextBonus = null;
                if (lowerBonusIsAvailable) {
                    nextBonus = Points.LOWER_BONUS_POINT;
                }
                if (upperBonusIsAvailable) {
                    nextBonus = Points.UPPER_BONUS_POINT;
                }
                if (nextBonus == null) {
                    bonusIteration.setState(BonusMiningState.END_OF_MINING);
                } else {
//                    strategy.setWayToBonus(calculateWayToBonus(nextBonus)); //todo research
                    Point2D myPos = strategy.getCurrentPosition();
                    if (firstBonusZone.contains(myPos)) {
                        owningCoef = estimateUnits(Zones.CENTER);
                        if (strategy.getZoneStatistics().get(Zones.CENTER).getEnemies().size() == 0 || owningCoef > 1.5) {
                            goTo(nextBonus);
                        } else {
//                            bonusIteration.setState(BonusMiningState.INTERRUPTED);
                            bonusIteration.setState(BonusMiningState.END_OF_MINING);
                        }
                    } else {
                        if(Zones.HOME.contains(myPos) || Zones.atJungle(myPos)) {
//                            bonusIteration.setState(BonusMiningState.INTERRUPTED);
                            bonusIteration.setState(BonusMiningState.END_OF_MINING);
                            strategy.setWizardState(WizardState.WALKING);
                        } else {
                            goTo(nextBonus);
                        }
                    }
                }
                break;
        }
    }

    private double estimateUnits(Zone zone) {
        ZoneStatistic zoneStatistic = strategy.getZoneStatistics().get(zone);
        LivingUnitsObjective objective = new LivingUnitsObjective();
        return objective.estimate(zoneStatistic.getAllies()) / objective.estimate(zoneStatistic.getEnemies());
    }

    public Point2D nearBonus() {
        if (Points.LOWER_BONUS_POINT.getDistanceTo(self) < Points.UPPER_BONUS_POINT.getDistanceTo(self)) {
            return Points.LOWER_BONUS_POINT;
        } else {
            return Points.UPPER_BONUS_POINT;
        }
    }

    private void updateBonusExistsInformation(Integer currentBonusIteration) {
        if (currentBonusIteration == null) {
            return;
        }

        if (Points.LOWER_BONUS_POINT.getDistanceTo(self) <= self.getVisionRange()) {
            Bonus bonus = getBonusByPoint(Points.LOWER_BONUS_POINT);
            bonusIterations.get(currentBonusIteration).setLowerBonusExists(bonus != null);
        } else if (Points.UPPER_BONUS_POINT.getDistanceTo(self) <= self.getVisionRange()) {
            Bonus bonus = getBonusByPoint(Points.UPPER_BONUS_POINT);
            bonusIterations.get(currentBonusIteration).setUpperBonusExists(bonus != null);
        }
        Bonus[] bonuses = world.getBonuses();
        for (Bonus bonus : bonuses) {
            Point2D bonusPoint = new Point2D(bonus);
            boolean bonusIsMained = false;
            double distanceBetweenBonusAndMe = bonusPoint.getDistanceTo(self);
            for (Wizard wizard : world.getWizards()) {
                if (wizard.isMe()) continue;
                double distanceBetweenBonusAndWizard = bonusPoint.getDistanceTo(wizard);
                if (distanceBetweenBonusAndWizard < (bonus.getRadius() + game.getBonusRadius())) {
                    if (distanceBetweenBonusAndMe > distanceBetweenBonusAndWizard) {
                        bonusIsMained = true;
                        break;
                    }
                }
            }
            if (Points.LOWER_BONUS_POINT.equals(bonusPoint)) {
                if (bonusIsMained) {
                    bonusIterations.get(currentBonusIteration).setLowerBonusHasPotentialOwner(true);
                }
            } else {
                if (bonusIsMained) {
                    bonusIterations.get(currentBonusIteration).setUpperBonusHasPotentialOwner(true);
                }
            }
        }
    }

    private Integer calculateCurrentBonusIteration() {
        if (world.getTickIndex() < bonusIterations.get(0).getShowTime()) {
            return 0;
        }
        for (int i = 0; i < bonusIterations.size() - 1; i++) {
            BonusIteration bonusIteration = bonusIterations.get(i);
            if (world.getTickIndex() >= bonusIteration.getShowTime()
                    && world.getTickIndex() < bonusIterations.get(i + 1).getShowTime()) {
                if(preIterationState
                        && (bonusIteration.getState() == BonusMiningState.END_OF_MINING
                            || bonusIteration.getState() == BonusMiningState.INTERRUPTED)) {
                    return i+1;
                }
                return i;
            }
        }
        return null;
    }

    public void setPreIterationState(boolean preIterationState) {
        this.preIterationState = preIterationState;
    }

    public Bonus getBonusByPoint(Point2D point) {
        for (Bonus bonus : world.getBonuses()) {
            if (point.equals(new Point2D(bonus.getX(), bonus.getY()))) {
                return bonus;
            }
        }
        return null;
    }

//    public List<Integer> getBonusIterations() {
//        return bonusIterations;
//    }
}
