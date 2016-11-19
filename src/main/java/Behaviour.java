import model.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public abstract class Behaviour {
    private static final double LOW_HP_FACTOR = 0.05D;
    protected static final double POINT_RADIUS = 1.0D;

    protected Wizard self;
    protected World world;
    protected Game game;
    protected Move move;

    protected MyStrategy strategy;
    protected Map<Zone, ZoneStatistic> zoneStatistics;

    public Behaviour(Wizard self, World world, Game game, Move move, MyStrategy strategy) {
        this.self = self;
        this.world = world;
        this.game = game;
        this.move = move;
        this.strategy = strategy;
    }

    abstract void perform();

    public void doIt() {
        if (self.getX() == 0 && self.getY() == 0) {
            strategy.setWizardState(WizardState.WALKING);
        }
        System.out.println(self.getX() + "   " + self.getY());
        zoneStatistics = strategy.getStatisticCollector().collectZoneStatistic();
        boolean minionsAreNear = minionsAreNear();
        if (towerAndWizardCanKill() || minionsAreNear || self.getLife() < self.getMaxLife() * LOW_HP_FACTOR) {
            if (minionsAreNear || !safe() && !nextSafe()) {
                Zone nearSafeZone = findNearSafeZone();
                goBack(nearSafeZone.getCentroid());
                return;
            }
        }

        perform();
    }

    protected boolean towerAndWizardCanKill() {
        if (self.getLife() > 0.7 * self.getMaxLife()) {
            return false;
        }
        List<LivingUnit> enemies = getNearDangerousEnemies();
        double potentialDamage = 0;
        for (LivingUnit enemy : enemies) {
            if (enemy instanceof Building) {
                Building building = (Building) enemy;
                if (building.getType() == BuildingType.GUARDIAN_TOWER) {
                    double enemyAttackRange = game.getGuardianTowerAttackRange();
                    double distanceToMe = building.getDistanceTo(self);
                    double timeForRetreat = getTimeForRetreat(enemyAttackRange, distanceToMe);
                    if (distanceToMe - enemyAttackRange*0.1 <= enemyAttackRange
                            && (building.getRemainingActionCooldownTicks() < timeForRetreat)) {
                        potentialDamage += game.getGuardianTowerDamage();
                    }
                }
            } else if (enemy instanceof Wizard) {
                Wizard wizard = (Wizard) enemy;
                List<SkillType> skillTypes = Arrays.asList(wizard.getSkills());
                double wizardDamage = 0;
                double distanceToMe = wizard.getDistanceTo(self);
                double timeForRetreat = getTimeForRetreat(game.getWizardCastRange(), distanceToMe);
                if(distanceToMe - game.getWizardCastRange()*0.2 <= game.getWizardCastRange()
                        && wizard.getRemainingActionCooldownTicks() < timeForRetreat) {
                    if (skillTypes.contains(SkillType.FROST_BOLT)
                            && wizard.getMana() >= game.getFrostBoltManacost()
                            && wizard.getRemainingCooldownTicksByAction()[ActionType.FROST_BOLT.ordinal()] < timeForRetreat) {
                        wizardDamage = Math.max(game.getFrostBoltDirectDamage(), wizardDamage);
                    }
                    if (skillTypes.contains(SkillType.FIREBALL)
                            && wizard.getMana() >= game.getFireballManacost()
                            && wizard.getRemainingCooldownTicksByAction()[ActionType.FIREBALL.ordinal()] < timeForRetreat) {
                        wizardDamage = Math.max(game.getFireballExplosionMaxDamage(), wizardDamage);
                    }
                    if (wizard.getMana() >= game.getMagicMissileManacost()) {
                        wizardDamage = Math.max(game.getMagicMissileDirectDamage(), wizardDamage);
                    }
                    potentialDamage += wizardDamage;
                }
            }
        }
        return potentialDamage >= self.getLife();
    }

    private double getTimeForRetreat(double enemyAttackRange, double distanceToMe) {
        double distanceToSave = enemyAttackRange - distanceToMe;
        return (distanceToSave / getBackwardSpeed()) * 8;
    }

    private List<LivingUnit> getNearDangerousEnemies() {
        Zone myZone = strategy.getCapturedZones().get(strategy.getCurrentZoneNumber());
        List<LivingUnit> enemies = zoneStatistics.get(myZone).getEnemies();
        if (validZone(strategy.getCurrentZoneNumber() + 1)) {
            Zone nextZone = strategy.getCapturedZones().get(strategy.getCurrentZoneNumber() + 1);
            enemies.addAll(zoneStatistics.get(nextZone).getEnemies());
        }
        return enemies;
    }

    private List<LivingUnit> getNearHelpfulAllies() {
        Zone selfZone = strategy.getCapturedZones().get(strategy.getCurrentZoneNumber());
        List<LivingUnit> allies = zoneStatistics.get(selfZone).getAllies();
        if (validZone(strategy.getCurrentZoneNumber() + 1)) {
            Zone nextZone = strategy.getCapturedZones().get(strategy.getCurrentZoneNumber() + 1);
            allies.addAll(zoneStatistics.get(nextZone).getAllies());
        }
        return allies;
    }

    protected boolean safe() {
        Zone myZone = strategy.getCapturedZones().get(strategy.getCurrentZoneNumber());
        return zoneStatistics.get(myZone).getEnemies().size() == 0;
    }

    protected boolean nextSafe() {
        if (validZone(strategy.getCurrentZoneNumber() + 1)) {
            Zone myZone = strategy.getCapturedZones().get(strategy.getCurrentZoneNumber() + 1);
            Map<Zone, ZoneStatistic> zoneStatistic = strategy.getStatisticCollector().collectZoneStatistic();
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
        move.setTurn(angle);
        if (StrictMath.abs(angle) < game.getStaffSector() / 4.0D) {
            move.setSpeed(game.getWizardForwardSpeed());
        }
    }

    protected void goBack(Point2D backPoint) {
        double angle = self.getAngleTo(backPoint.getX(), backPoint.getY());
        double lookAngle = angle > 0 ? angle - Math.PI : angle + Math.PI;
        move.setTurn(lookAngle);
        if (StrictMath.abs(lookAngle) < game.getStaffSector()) {
            move.setSpeed(-getBackwardSpeed());
        }
    }

    protected Zone findNearSafeZone() {
        List<Zone> capturedZones = strategy.getCapturedZones();
        int currentZoneNumber = strategy.getCurrentZoneNumber();
        if (currentZoneNumber > 0) {
            return capturedZones.get(currentZoneNumber - 1);
        } else {
            return Zones.LAST_HOME;
        }
    }

    private double getBackwardSpeed() {
        return game.getWizardBackwardSpeed() * speedMultiplier();
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

    private boolean minionsAreNear() {
        for (Minion minion : world.getMinions()) {
            if (minion.getFaction() == self.getFaction() || minion.getFaction() == Faction.NEUTRAL) continue;
            double criticalDistance;
            if (MinionType.ORC_WOODCUTTER == minion.getType()) {
                criticalDistance = game.getOrcWoodcutterAttackRange() + minion.getRadius() + self.getRadius();
            } else {
                criticalDistance = game.getFetishBlowdartAttackRange();
            }
            if (self.getDistanceTo(minion) <= criticalDistance + 50) {//todo наблюдать
                return true;
            }

        }
        return false;
    }
}
