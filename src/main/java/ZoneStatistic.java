import model.LivingUnit;

import java.util.List;

public class ZoneStatistic {

    private List<LivingUnit> enemies;
    private List<LivingUnit> allies;

    public ZoneStatistic(List<LivingUnit> enemies, List<LivingUnit> allies) {
        this.enemies = enemies;
        this.allies = allies;
    }

    public List<LivingUnit> getEnemies() {
        return enemies;
    }

    public List<LivingUnit> getAllies() {
        return allies;
    }
}
