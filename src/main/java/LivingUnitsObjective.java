import model.*;

import java.util.Collection;

public class LivingUnitsObjective {

    public static final double ORC_WEIGHT = 0.1;
    public static final double FETISH_WEIGHT = 0.15;
    public static final double TOWER_WEIGHT = 0.45;
    public static final double WIZARD_WEIGHT = 0.3;

    public double estimate(Collection<LivingUnit> livingUnits) {
        double estimation = 0;
        for (LivingUnit livingUnit : livingUnits) {
            if(livingUnit instanceof Minion) {
                Minion minion = (Minion) livingUnit;
                if(minion.getType() == MinionType.ORC_WOODCUTTER) {
                    estimation += ORC_WEIGHT;
                } else {
                    estimation += FETISH_WEIGHT;
                }
            } else if(livingUnit instanceof Wizard) {
                estimation += WIZARD_WEIGHT;
            } else if(livingUnit instanceof Building) {
                Building building = (Building) livingUnit;
                if(building.getType() == BuildingType.GUARDIAN_TOWER) {
                    estimation += TOWER_WEIGHT;
                }
            }
        }
        return estimation;
    }


}
