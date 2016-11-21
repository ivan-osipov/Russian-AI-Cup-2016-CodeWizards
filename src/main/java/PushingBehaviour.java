import model.*;

import java.util.*;
import java.util.stream.Collectors;

public class PushingBehaviour extends Behaviour {

    public PushingBehaviour(Wizard self, World world, Game game, Move move, MyStrategy strategy) {
        super(self, world, game, move, strategy);
    }

    @Override
    public void perform() {
        if(safe()) {
            BonusMiningBehaviour bonusMining = (BonusMiningBehaviour) strategy.getBehaviours().get(WizardState.BONUS_MINING);
            if(bonusMining.isInterrupted()) {
                strategy.setWizardState(WizardState.BONUS_MINING);
            }
        }
        if(safe() && nextSafe()) {
            strategy.setWizardState(WizardState.WALKING);
            return;
        }

        System.out.println("Pushing");
        List<LivingUnit> sortedByWeakFactorNearstEnemies = getSortedByWeakFactorNearstEnemies();
        LivingUnit nearestTarget = sortedByWeakFactorNearstEnemies.isEmpty() ? null : sortedByWeakFactorNearstEnemies.get(0);

        boolean alliesOwnTerritory = strategy.getStatisticCollector().superiorityOfAllies() > 0.75;
        if (nearestTarget != null && alliesOwnTerritory) {
            double distance = self.getDistanceTo(nearestTarget);

            if (distance <= self.getCastRange()) {

                double angle = self.getAngleTo(nearestTarget);

                if (self.getLife() / self.getMaxLife() > 0.4) {
                    if (nearestTarget.getLife() - game.getMagicMissileDirectDamage() < game.getMagicMissileDirectDamage()) {
                        if (distance > self.getCastRange() / 2) {
                            //подходим ближе, чтобы смочь добить
                            goTo(new Point2D(nearestTarget.getX(), nearestTarget.getY()));
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
        } else if(nearestTarget == null) {
            if(validZone(strategy.getCurrentZoneNumber() + 1)) {
                Point2D targetPoint = strategy.getCapturedZones().get(strategy.getCurrentZoneNumber() + 1).getCentroid();
                goTo(targetPoint);
            }
        }

    }

    private List<LivingUnit> getSortedByWeakFactorNearstEnemies() {
        List<LivingUnit> targets = new ArrayList<>();
        List<Building> buildings = Arrays.asList(world.getBuildings());
        targets.addAll(buildings);
        Map<LivingUnit, Integer> weights = new HashMap<>();
        for (Building building : buildings) {
            weights.put(building, 1);
        }
        List<Wizard> wizards = Arrays.asList(world.getWizards());
        targets.addAll(wizards);
        for (Wizard wizard : wizards) {
            weights.put(wizard, 2);
        }
        List<Minion> minions = Arrays.asList(world.getMinions());
        targets.addAll(minions);
        for (Minion minion : minions) {
            weights.put(minion, 3);
        }
        return targets.stream().filter(target -> {
            return target.getFaction() != Faction.NEUTRAL && target.getFaction() != self.getFaction() && self.getDistanceTo(target) <= self.getCastRange();
        }).sorted((target1, target2) -> {
            int weightsCompare = Integer.compare(weights.get(target1), weights.get(target2));
            if (weightsCompare == 0) {
                int lifeCompare = Integer.compare(target1.getLife(), target2.getLife());
                if (lifeCompare == 0) {
                    return Double.compare(self.getDistanceTo(target1), self.getDistanceTo(target2));
                }
                return lifeCompare;
            }
            return weightsCompare;
        }).collect(Collectors.toList());
    }

}
