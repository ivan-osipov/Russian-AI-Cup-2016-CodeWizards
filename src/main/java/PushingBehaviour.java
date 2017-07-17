import model.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PushingBehaviour extends Behaviour {

    public PushingBehaviour(Wizard self, World world, Game game, Move move, MyStrategy strategy) {
        super(self, world, game, move, strategy);
    }

    @Override
    public void perform() {
        if(world.getTickIndex() % 3 == 0) {
            System.out.println("Pushing");
        }
        List<LivingUnit> sortedByWeakFactorNearstEnemies = getSortedByWeakFactorNearstEnemies();
        LivingUnit nearestTarget = sortedByWeakFactorNearstEnemies.isEmpty() ? null : sortedByWeakFactorNearstEnemies.get(0);

        boolean alliesOwnTerritory = strategy.getStatisticCollector().superiorityOfAllies() > 1.2;
        if (nearestTarget != null) {
            double distance = self.getDistanceTo(nearestTarget);

            if (distance <= self.getCastRange()) {

                double angle = self.getAngleTo(nearestTarget);

                if (alliesOwnTerritory && self.getLife() / self.getMaxLife() > 0.4) {
                    if (nearestTarget.getLife() - game.getMagicMissileDirectDamage() < game.getMagicMissileDirectDamage()) {
                        if (distance > self.getCastRange() / 1.5) {
                            //подходим ближе, чтобы смочь добить
                            goTo(new Point2D(nearestTarget));
                            return;
                        }
                    }
                }

                move.setTurn(angle);

                if (StrictMath.abs(angle) < game.getStaffSector() / 2.0D) {
                    move.setCastAngle(angle);
                    move.setMinCastDistance(distance - nearestTarget.getRadius() + game.getMagicMissileRadius());

                    if (game.isSkillsEnabled()) {
                        int fireballRemainingCooldown = self.getRemainingCooldownTicksByAction()[ActionType.FIREBALL.ordinal()];
                        int maxCooldown = Math.max(fireballRemainingCooldown, self.getRemainingActionCooldownTicks());
                        if (self.getLevel() >= 16 && maxCooldown == 0) {
                            move.setAction(ActionType.FIREBALL);
                            return;
                        }
                        int frostBoltRemainingCooldown = self.getRemainingCooldownTicksByAction()[ActionType.FROST_BOLT.ordinal()];
                        maxCooldown = Math.max(frostBoltRemainingCooldown, self.getRemainingActionCooldownTicks());
                        if ((self.getLevel() >= 6) && maxCooldown == 0) {
                            move.setAction(ActionType.FROST_BOLT);
                            return;
                        }
                    }
                    move.setAction(ActionType.MAGIC_MISSILE);
                }
            }
        } else {
            if(safe() && nextSafe()) {
                strategy.setWizardState(WizardState.WALKING);
                return;
            }
            if(safe() && strategy.getCurrentZoneNumber() < strategy.getBattleZoneNumber()) {
                if(validZone(strategy.getCurrentZoneNumber() + 1)) {
                    Point2D targetPoint = strategy.getCapturedZones().get(strategy.getCurrentZoneNumber() + 1).getCentroid();
                    goTo(targetPoint);
                }
            } else if(!safe()) {
                Point2D targetPoint = strategy.getCapturedZones().get(strategy.getCurrentZoneNumber()).getCentroid();
                goTo(targetPoint);
            } else {
                //прошли мимо battleZone
                goTo(strategy.getCapturedZones().get(strategy.getBattleZoneNumber()).getCentroid());
            }
        }

    }

    private List<LivingUnit> getSortedByWeakFactorNearstEnemies() {
        List<LivingUnit> targets = new ArrayList<>();
        targets.addAll(Arrays.asList(world.getBuildings()));
        targets.addAll(Arrays.asList(world.getWizards()));
        targets.addAll(Arrays.asList(world.getMinions()));
        return targets.stream()
                .filter(new TargetEnemyFilter(self))
                .sorted(new TargetComparator(self))
                .collect(Collectors.toList());
    }

}
