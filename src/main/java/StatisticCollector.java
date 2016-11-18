import model.Faction;
import model.LivingUnit;
import model.Wizard;
import model.World;

import java.util.*;

public class StatisticCollector {

    private Wizard self;
    private World world;

    public StatisticCollector(Wizard self, World world) {
        this.self = self;
        this.world = world;
    }

    public Map<Zone, ZoneStatistic> collectZoneStatistic() {
        Map<Zone, ZoneStatistic> statisticsByZones = new HashMap<>();
        List<LivingUnit> livingUnits = getLivingUnits();
        for (Zone battleZone : Zones.ALL_STATIC) {
            List<LivingUnit> enemies = new ArrayList<>();
            List<LivingUnit> allies = new ArrayList<>();
            Set<LivingUnit> containedUnits = new HashSet<>();
            for (LivingUnit livingUnit : livingUnits) {
                if (battleZone.contains(new Point2D(livingUnit.getX(), livingUnit.getY()))) {
                    containedUnits.add(livingUnit);
                    if (livingUnit.getFaction() == self.getFaction()) {
                        allies.add(livingUnit);
                    } else if (livingUnit.getFaction() != Faction.NEUTRAL) {
                        enemies.add(livingUnit);
                    }
                }
            }
            livingUnits.removeAll(containedUnits);
            statisticsByZones.put(battleZone, new ZoneStatistic(enemies, allies));
        }
        return statisticsByZones;
    }

    public List<LivingUnit> getLivingUnits() {
        List<LivingUnit> targets = new ArrayList<>();
        targets.addAll(Arrays.asList(world.getBuildings()));
        targets.addAll(Arrays.asList(world.getWizards()));
        targets.addAll(Arrays.asList(world.getMinions()));
        return targets;
    }

    public double superiorityOfAllies() {
        List<LivingUnit> livingUnits = getLivingUnits();
        double allies = 0;
        double enemies = 0;
        for (LivingUnit livingUnit : livingUnits) {
            if (livingUnit.getDistanceTo(self) > self.getVisionRange()) {
                continue;
            }
            if (livingUnit.getFaction() == self.getFaction()) {
                allies++;
            } else if (livingUnit.getFaction() != Faction.NEUTRAL) {
                enemies++;
            }
        }
        if (enemies == 0) {
            return 10000;
        }
        return allies / enemies;
    }

}
