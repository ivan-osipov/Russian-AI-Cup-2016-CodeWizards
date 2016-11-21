public class BonusIteration {

    private boolean lowerBonusExists = false;

    private boolean upperBonusExists = false;

    private boolean lowerBonusHasPotentialOwner = false;

    private boolean upperBonusHasPotentialOwner = false;

    private BonusMiningState state = BonusMiningState.HOPE_OF_TAKE;

    private int showTime;

    public BonusIteration(int showTime) {
        this.showTime = showTime;
    }

    public void nowShowTime() {
        lowerBonusExists = true;
        upperBonusExists = true;
        state = BonusMiningState.FIND_FIRST;
    }

    public int getShowTime() {
        return showTime;
    }

    public boolean isLowerBonusExists() {
        return lowerBonusExists;
    }

    public void setLowerBonusExists(boolean lowerBonusExists) {
        this.lowerBonusExists = lowerBonusExists;
    }

    public boolean isUpperBonusExists() {
        return upperBonusExists;
    }

    public void setUpperBonusExists(boolean upperBonusExists) {
        this.upperBonusExists = upperBonusExists;
    }

    public boolean isLowerBonusHasPotentialOwner() {
        return lowerBonusHasPotentialOwner;
    }

    public void setLowerBonusHasPotentialOwner(boolean lowerBonusHasPotentialOwner) {
        this.lowerBonusHasPotentialOwner = lowerBonusHasPotentialOwner;
    }

    public boolean isUpperBonusHasPotentialOwner() {
        return upperBonusHasPotentialOwner;
    }

    public void setUpperBonusHasPotentialOwner(boolean upperBonusHasPotentialOwner) {
        this.upperBonusHasPotentialOwner = upperBonusHasPotentialOwner;
    }

    public BonusMiningState getState() {
        return state;
    }

    public void setState(BonusMiningState state) {
        this.state = state;
    }
}
