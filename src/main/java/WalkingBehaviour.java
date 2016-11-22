import model.Game;
import model.Move;
import model.Wizard;
import model.World;

import java.util.List;

public class WalkingBehaviour extends Behaviour {

    private Point2D targetPoint;

    private Point2D fixedTargetPoint;

    private List<Point2D> bestWayPoints;

    public WalkingBehaviour(Wizard self, World world, Game game, Move move, MyStrategy strategy) {
        super(self, world, game, move, strategy);
    }

    @Override
    public void perform() {
        System.out.println("Walking");
        if (safe() && nextSafe()) {
            if(strategy.getCurrentZoneNumber() + 2 > strategy.getCapturedZones().size()) {
                System.err.println("No decisions");
                return;
            }
            targetPoint = strategy.getCapturedZones().get(strategy.getCurrentZoneNumber() + 1).getCentroid();
            Point2D selfPoint = strategy.getCurrentPosition();
            Point2D nearToSelfGraphPoint = Points.CHECK_POINTS.get(0);
            Point2D nearToTargetGraphPoint = Points.CHECK_POINTS.get(0);
            for (Point2D checkPoint : Points.CHECK_POINTS) {
                if (checkPoint.getDistanceTo(selfPoint) < nearToSelfGraphPoint.getDistanceTo(selfPoint)) {
                    nearToSelfGraphPoint = checkPoint;
                }
                if (checkPoint.getDistanceTo(targetPoint) < nearToTargetGraphPoint.getDistanceTo(targetPoint)) {
                    nearToTargetGraphPoint = checkPoint;
                }
            }
            if(nearToSelfGraphPoint.getDistanceTo(nearToTargetGraphPoint) > POINT_RADIUS) {
                if(fixedTargetPoint == null || !fixedTargetPoint.equals(targetPoint)) {
                    fixedTargetPoint = targetPoint;
                    GraphMapper graphMapper = strategy.getGraphMapper().copy();
                    GameMapGraph graph = strategy.getGraph().copy();
                    addEdgeBetweenAbsoluteAndGraphPoint(selfPoint, nearToSelfGraphPoint, graphMapper, graph);
                    addEdgeBetweenAbsoluteAndGraphPoint(targetPoint, nearToTargetGraphPoint, graphMapper, graph);
                    List<GameMapGraph.Node> bestWay = graph
                            .findBestWayDijkstra(
                                    graphMapper.map(nearToSelfGraphPoint),
                                    graphMapper.map(nearToTargetGraphPoint));
                    bestWayPoints = graphMapper.map(bestWay);
                }
                goTo(getNextWaypoint(bestWayPoints));
            } else {
                goTo(targetPoint);
            }
        } else {
            System.out.println("Here isn't safe");
            strategy.setWizardState(WizardState.PUSHING);
        }
    }
}
