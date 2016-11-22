import model.Building;
import model.LivingUnit;
import model.Minion;
import model.Wizard;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class TargetComparator implements Comparator<LivingUnit> {

    private Map<Class<? extends LivingUnit>, Integer> weights = new HashMap<>();

    private Wizard self;

    public TargetComparator(Wizard self) {
        weights.put(Building.class, 1);
        weights.put(Wizard.class, 2);
        weights.put(Minion.class, 3);
        this.self = self;
    }

    @Override
    public int compare(LivingUnit target1, LivingUnit target2) {
        int weightsCompare = Integer.compare(weights.get(target1.getClass()), weights.get(target2.getClass()));
        if (weightsCompare == 0) {
            int lifeCompare = Integer.compare(target1.getLife(), target2.getLife());
            if (lifeCompare == 0) {
                return Double.compare(self.getDistanceTo(target1), self.getDistanceTo(target2));
            }
            return lifeCompare;
        }
        return weightsCompare;
    }
}
