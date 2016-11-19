import model.Game;
import model.Move;
import model.Wizard;
import model.World;

public class ProtectionBehaviour extends  Behaviour {

    public ProtectionBehaviour(Wizard self, World world, Game game, Move move, MyStrategy strategy) {
        super(self, world, game, move, strategy);
    }

    @Override
    void perform() {
        System.out.println("Protection");

    }
}
