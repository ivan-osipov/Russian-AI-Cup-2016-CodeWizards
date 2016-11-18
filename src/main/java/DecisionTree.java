import model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class DecisionTree {

    private List<SelfNode> selfNodes = new ArrayList<>();

    private Objective objective = new Objective();

    public DecisionTree() {
        init();
    }

    private void init() {
        for (ActionType actionType : ActionType.values()) {
            selfNodes.add(new SelfNode(actionType));
        }
    }

    public ActionType decideActionType(World originalWorld, Wizard originalSelf, Move originalMove) {
        Map<SelfNode, Double> estimations = selfNodes.stream().collect(Collectors.toMap(n -> n, node -> node.estimate(originalWorld, originalSelf, originalMove)));
        Optional<SelfNode> bestNode = selfNodes.stream().max((n1, n2) -> estimations.get(n1).compareTo(estimations.get(n2)));
        if (bestNode.isPresent()) {
            return bestNode.get().actionType;
        } else {
            return ActionType.MAGIC_MISSILE;
        }
    }

    private final class SelfNode extends AbstractNode {

        ActionType actionType;

        public SelfNode(ActionType actionType) {
            this.actionType = actionType;
        }

    }

    private final class EnemyNode extends AbstractNode {

    }

    private class AbstractNode {
        private List<? extends AbstractNode> nodes = new ArrayList<>();

        public double estimate(World world, Wizard self, Move move) {
            return objective.estimate(world, self, move);
        }
    }

    private static class Objective {

        public double estimate(World world, Wizard self, Move move) {
            return estimateScore(world) + estimateMana(self);
        }

        private int estimateScore(World world) {
            return world.getMyPlayer().getScore() - 200;
        }

        private int estimateMana(Wizard self) {
            return self.getMana() * (1 / (1 + self.getRemainingActionCooldownTicks() /* докинуть кулдаун скила из move... */));
        }

    }

    public static World updateAttributes(World world, int score) {
        Player[] players = world.getPlayers();
        int myPlayerIndex = myPlayerIndex(players);
        players[myPlayerIndex] = copy(players[myPlayerIndex], score);

        return copy(world, players);
    }

    public static World copy(World world, Player[] players) {
        return new World(world.getTickIndex(), world.getTickCount(), world.getWidth(), world.getHeight(), players, world.getWizards(), world.getMinions(), world.getProjectiles(), world.getBonuses(), world.getBuildings(), world.getTrees());
    }

    public static Player copy(Player player, int score) {
        return new Player(player.getId(), player.isMe(), player.getName(), player.isStrategyCrashed(), score, player.getFaction());
    }

    public static Move copy(Move move, ActionType actionType) {
        Move copy = new Move();
        copy.setAction(actionType);
        return copy;
    }

    private static int myPlayerIndex(Player[] players) {
        for (int i = 0; i < players.length; i++) {
            if (players[i].isMe()) {
                return i;
            }
        }
        return -1;
    }

}
