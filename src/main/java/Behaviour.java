import model.*;

import java.util.List;
import java.util.Map;

public abstract class Behaviour {
    private static final double LOW_HP_FACTOR = 0.35D;
    private static final double POINT_RADIUS = 70.0D;

    protected Wizard self;
    protected World world;
    protected Game game;
    protected Move move;

    protected MyStrategy strategy;

    public Behaviour(Wizard self, World world, Game game, Move move, MyStrategy strategy) {
        this.self = self;
        this.world = world;
        this.game = game;
        this.move = move;
        this.strategy = strategy;
    }

    abstract void perform();

    public void doIt() {
        if (self.getLife() < self.getMaxLife() * LOW_HP_FACTOR || minionsAreNear()) {
            Zone nearSafeZone = findNearSafeZone();
            goBack(nearSafeZone.getCentroid());
            return;
        }
        if(safe()) {
            goTo(strategy.getCapturedZones().get(strategy.getCurrentZoneNumber() + 1).getCentroid());
            return;
        }
        perform();
    }

    private boolean safe() {
        Zone myZone = strategy.getCapturedZones().get(strategy.getCurrentZoneNumber());
        Map<Zone, ZoneStatistic> zoneStatistic = strategy.getStatisticCollector().collectZoneStatistic();
        return zoneStatistic.get(myZone).getEnemies().size() == 0;
    }

    private void goTo(Point2D point) {
        double angle = self.getAngleTo(point.getX(), point.getY());
        move.setTurn(angle);
        if (StrictMath.abs(angle) < game.getStaffSector() / 4.0D) {
            move.setSpeed(game.getWizardForwardSpeed());
        }
    }

    private void goBack(Point2D backPoint) {
        double angle = self.getAngleTo(backPoint.getX(), backPoint.getY());
        double lookAngle = angle > 0 ? angle - 180 : angle + 180;
        move.setTurn(lookAngle);
        if (StrictMath.abs(lookAngle) < game.getStaffSector()) {
            move.setSpeed(getBackwardSpeed());
        }
    }

    protected Zone findNearSafeZone() {
        List<Zone> capturedZones = strategy.getCapturedZones();
        int currentZoneNumber = strategy.getCurrentZoneNumber();
        if(currentZoneNumber > 0) {
            return  capturedZones.get(currentZoneNumber - 1);
        } else {
            return Zones.LAST_HOME;
        }
    }

    private double getBackwardSpeed() {
        return game.getWizardBackwardSpeed() * speedMultiplier() ;
    }

    private double speedMultiplier() {
        return hasSpeedUp() ? 1.0 + game.getHastenedRotationBonusFactor() : 1.0;
    }

    private boolean hasSpeedUp() {
        Status[] statuses = self.getStatuses();
        for (Status status : statuses) {
            if(status.getType() == StatusType.HASTENED) {
                return true;
            }
        }
        return false;
    }

    private boolean minionsAreNear() {
        for (Minion minion : world.getMinions()) {
            if (minion.getFaction() == self.getFaction()) continue;
            double criticalDistance;
            if(MinionType.ORC_WOODCUTTER == minion.getType()) {
                criticalDistance = game.getOrcWoodcutterAttackRange();
            } else {
                criticalDistance = game.getFetishBlowdartAttackRange();
            }
            if (self.getDistanceTo(minion) <= criticalDistance + 20) {
                return true;
            }

        }
        return false;
    }
}
