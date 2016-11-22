import model.Unit;
import model.Wizard;

import java.util.Comparator;

public class NearestToSelfComparator implements Comparator<Unit> {

    private Wizard self;

    public NearestToSelfComparator(Wizard self) {
        this.self = self;
    }

    @Override
    public int compare(Unit o1, Unit o2) {
        return Double.compare(o1.getDistanceTo(self), o2.getDistanceTo(self));
    }
}
