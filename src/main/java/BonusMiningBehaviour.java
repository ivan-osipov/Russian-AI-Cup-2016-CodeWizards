import model.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BonusMiningBehaviour extends Behaviour {

    enum BonusMiningState {HOPE_OF_TAKE, FIND_FIRST, INTERRUPTED, FIND_SECOND, COME_BACK}

    private boolean lowerBonusExists = false;

    private boolean upperBonusExists = false;

    private Point2D lowerWaitingPoint = new Point2D(2750, 2750);

    private Point2D upperWaitingPoint = new Point2D(1150, 1150);

    private BonusMiningState currentMiningState = BonusMiningState.HOPE_OF_TAKE;

    private Zone firstBonusZone;

    private final List<Integer> bonusRespTimes = new ArrayList<>(Arrays
            .asList(2501, 5001, 7501, 10001, 12501, 15001, 17501, 20001));

    public BonusMiningBehaviour(Wizard self, World world, Game game, Move move, MyStrategy strategy) {
        super(self, world, game, move, strategy);
    }

    public void reset() {
        lowerBonusExists = false;

        upperBonusExists = false;

        currentMiningState = BonusMiningState.HOPE_OF_TAKE;
    }

    public void nowTimeOfOccurrence() {
        lowerBonusExists = true;
        upperBonusExists = true;
        currentMiningState = BonusMiningState.FIND_FIRST;
    }

    @Override
    public void doIt() {
        if(strategy.getStartPoint().equals(currentPosition)) {//если умер
            if(safeByNumber(0) && safeByNumber(1) && safeByNumber(2)){
                strategy.setWayToBonus(calculateWayToBonus());
            } else {
                strategy.setWizardState(WizardState.WALKING);
            }
        }
        zoneStatistics = strategy.getStatisticCollector().collectZoneStatistic();
        perform();
    }

    @Override
    void perform() {
        System.out.println("Bonus mining");
        List<Point2D> wayToBonus = strategy.getWayToBonus();
        Point2D bonusPoint = wayToBonus.get(wayToBonus.size() - 1);
        System.out.println(currentMiningState);
        switch (currentMiningState) {
            case HOPE_OF_TAKE:
                if (Points.LOWER_BONUS_POINT.equals(bonusPoint)) {
                    goTo(getNextWaypoint(getReplaceLastPoint(wayToBonus, lowerWaitingPoint)));
                } else {
                    goTo(getNextWaypoint(getReplaceLastPoint(wayToBonus, upperWaitingPoint)));
                }
                if (lowerBonusExists || upperBonusExists) {
                    currentMiningState = BonusMiningState.FIND_FIRST;
                }
                break;
            case FIND_FIRST:
                updateBonusExistsInformation();
                if (Points.LOWER_BONUS_POINT.equals(bonusPoint)) {
                    firstBonusZone = Zones.MIDDLE_BOTTOM_TRANSITION;
                    if (lowerBonusExists) {
                        if (!wayToBonus.isEmpty()) {
                            goTo(getNextWaypoint(wayToBonus));
                        } else {
                            goTo(nearBonus());
                        }
                    } else if (upperBonusExists) {
                        double owningCoef = estimateUnits(Zones.CENTER);
                        if (zoneStatistics.get(Zones.CENTER).getEnemies().size() == 0 || owningCoef > 2) {
                            currentMiningState = BonusMiningState.FIND_SECOND;
                        } else {
                            currentMiningState = BonusMiningState.INTERRUPTED;
                        }
                    } else {
                        currentMiningState = BonusMiningState.COME_BACK;
                    }
                } else {
                    firstBonusZone = Zones.TOP_MIDDLE_TRANSITION;
                    if (upperBonusExists) {
                        if (!wayToBonus.isEmpty()) {
                            goTo(getNextWaypoint(wayToBonus));
                        } else {
                            goTo(nearBonus());
                        }
                    } else {
                        if (lowerBonusExists) {
                            double owningCoef = estimateUnits(Zones.CENTER);
                            if (zoneStatistics.get(Zones.CENTER).getEnemies().size() == 0 || owningCoef > 2) {
                                currentMiningState = BonusMiningState.FIND_SECOND;
                            } else {
                                currentMiningState = BonusMiningState.INTERRUPTED;
                            }
                        } else {
                            currentMiningState = BonusMiningState.COME_BACK;
                        }
                    }
                }
                break;
            case INTERRUPTED:
            case COME_BACK:
                if (!Zones.CENTER.contains(new Point2D(self.getX(), self.getY()))) {
                    goTo(Zones.CENTER.getCentroid());
                } else {
                    strategy.setWizardState(WizardState.WALKING);
                }
                break;
            case FIND_SECOND:
                updateBonusExistsInformation();
                Point2D nextBonus = null;
                if (lowerBonusExists) {
                    nextBonus = Points.LOWER_BONUS_POINT;
                }
                if (upperBonusExists) {
                    nextBonus = Points.UPPER_BONUS_POINT;
                }
                if (nextBonus == null) {
                    currentMiningState = BonusMiningState.COME_BACK;
                } else {
//                    strategy.setWayToBonus(calculateWayToBonus(nextBonus)); //todo research
                    if (firstBonusZone.contains(strategy.getCurrentPosition())) {
                        double owningCoef = estimateUnits(Zones.CENTER);
                        if (zoneStatistics.get(Zones.CENTER).getEnemies().size() == 0 || owningCoef > 1.5) {
                            goTo(nextBonus);
                        } else {
                            currentMiningState = BonusMiningState.INTERRUPTED;
                        }
                    } else {
                        goTo(nextBonus);
                    }
                }
                break;
        }
    }

    private double estimateUnits(Zone zone) {
        ZoneStatistic zoneStatistic = zoneStatistics.get(zone);
        LivingUnitsObjective objective = new LivingUnitsObjective();
        return objective.estimate(zoneStatistic.getAllies()) / objective.estimate(zoneStatistic.getEnemies());
    }

    public boolean isInterrupted() {
        return currentMiningState == BonusMiningState.INTERRUPTED;
    }

    public Point2D nearBonus() {
        if (Points.LOWER_BONUS_POINT.getDistanceTo(self) < Points.UPPER_BONUS_POINT.getDistanceTo(self)) {
            return Points.LOWER_BONUS_POINT;
        } else {
            return Points.UPPER_BONUS_POINT;
        }
    }

    private void updateBonusExistsInformation() {
        if (Points.LOWER_BONUS_POINT.getDistanceTo(self) <= self.getVisionRange()) {
            Bonus bonus = getBonusByPoint(Points.LOWER_BONUS_POINT);
            lowerBonusExists = (bonus != null);
        } else if (Points.UPPER_BONUS_POINT.getDistanceTo(self) <= self.getVisionRange()) {
            Bonus bonus = getBonusByPoint(Points.UPPER_BONUS_POINT);
            upperBonusExists = (bonus != null);
        }
        Bonus[] bonuses = world.getBonuses();
        for (Bonus bonus : bonuses) {
            Point2D bonusPoint = new Point2D(bonus.getX(), bonus.getY());
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
                    lowerBonusExists = false;
                }
            } else {
                if (bonusIsMained) {
                    upperBonusExists = false;
                }
            }
        }
    }

    public Bonus getBonusByPoint(Point2D point) {
        for (Bonus bonus : world.getBonuses()) {
            if (point.equals(new Point2D(bonus.getX(), bonus.getY()))) {
                return bonus;
            }
        }
        return null;
    }

    public List<Integer> getBonusRespTimes() {
        return bonusRespTimes;
    }
}
