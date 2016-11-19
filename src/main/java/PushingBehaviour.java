import model.*;

import java.util.*;
import java.util.stream.Collectors;

public class PushingBehaviour extends Behaviour {

    public PushingBehaviour(Wizard self, World world, Game game, Move move, MyStrategy strategy) {
        super(self, world, game, move, strategy);
    }

    @Override
    public void perform() {
        if(safe() && nextSafe()) {
            strategy.setWizardState(WizardState.WALKING);
            return;
        }
        System.out.println("Pushing");
        List<LivingUnit> sortedByWeakFactorNearstEnemies = getSortedByWeakFactorNearstEnemies();
        LivingUnit nearestTarget = sortedByWeakFactorNearstEnemies.isEmpty() ? null : sortedByWeakFactorNearstEnemies.get(0);
        //to bonus
//            if ((self.getX() <= 600 && self.getY() <= 600 && (nearestTarget == null || nearestTarget.getX() > 900)) ||
//                    (self.getX() >= game.getMapSize() - 600 && self.getY() >= game.getMapSize() - 600 && (nearestTarget == null || nearestTarget.getY() <= 3500))) {
//                if ((world.getTickIndex() + 100) / 2500 > lastTickCheck) {
//                    movingState = MovingState.TO_BONUS;
//                    bonusIsChecked = false;
//                    lastTickCheck++;
//                    goTo(getNextWaypointToBonus());
//                    return;
//                }
//            }


        // Если видим противника ... //todo не только ближайший, но и с меньшим здоровьем
        boolean alliesOwnTerritory = strategy.getStatisticCollector().superiorityOfAllies() > 0.75;
        if (nearestTarget != null && alliesOwnTerritory) {
            double distance = self.getDistanceTo(nearestTarget);

            if (distance <= self.getCastRange()) {

                // ... и он в пределах досягаемости наших заклинаний, ...

                double angle = self.getAngleTo(nearestTarget);

                if (self.getLife() / self.getMaxLife() > 0.6) {
                    if (nearestTarget.getLife() - game.getMagicMissileDirectDamage() < game.getMagicMissileDirectDamage()) {
                        if (distance > self.getCastRange() / 2) {
                            //подходим ближе, чтобы смочь добить
                            goTo(new Point2D(nearestTarget.getX(), nearestTarget.getY()));
                            return;
                        }
                    }
                }

                // ... то поворачиваемся к цели.
                move.setTurn(angle);

                // Если цель перед нами, ...
                if (StrictMath.abs(angle) < game.getStaffSector() / 2.0D) {
                    // ... то атакуем.
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
                return;
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
