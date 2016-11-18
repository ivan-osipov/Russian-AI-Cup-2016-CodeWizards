import model.Game;
import model.Move;
import model.Wizard;
import model.World;

public class CaptureBehaviour extends Behaviour {

    public CaptureBehaviour(Wizard self, World world, Game game, Move move, MyStrategy strategy) {
        super(self, world, game, move, strategy);
    }

    @Override
    public void perform() {

    }
}
