import model.Faction;
import model.LivingUnit;
import model.Wizard;

import java.util.function.Predicate;

public class TargetEnemyFilter implements Predicate<LivingUnit> {

    private Wizard self;

    public TargetEnemyFilter(Wizard self) {
        this.self = self;
    }

    @Override
    public boolean test(LivingUnit unit) {
        return unit.getFaction() != Faction.NEUTRAL && unit.getFaction() != self.getFaction() && self.getDistanceTo(unit) <= self.getCastRange();
    }

}
