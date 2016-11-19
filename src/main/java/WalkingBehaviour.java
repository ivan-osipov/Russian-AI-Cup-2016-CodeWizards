import model.Game;
import model.Move;
import model.Wizard;
import model.World;

import java.util.List;

public class WalkingBehaviour extends Behaviour {

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
            Point2D targetPoint = strategy.getCapturedZones().get(strategy.getCurrentZoneNumber() + 1).getCentroid();
            Point2D selfPoint = new Point2D(self.getX(), self.getY());
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
                GraphMapper graphMapper = strategy.getGraphMapper().copy();
                GameMapGraph graph = strategy.getGraph().copy();
                addEdgeBetweenAbsoluteAndGraphPoint(selfPoint, nearToSelfGraphPoint, graphMapper, graph);
                addEdgeBetweenAbsoluteAndGraphPoint(targetPoint, nearToTargetGraphPoint, graphMapper, graph);
                List<GameMapGraph.Node> bestWay = graph
                        .findBestWayDijkstra(
                                graphMapper.map(nearToSelfGraphPoint),
                                graphMapper.map(nearToTargetGraphPoint));
                List<Point2D> bestWayPoints = graphMapper.map(bestWay);
                goTo(getNextWaypoint(bestWayPoints));
            } else {
                goTo(targetPoint);
            }
            return;
        } else {
            System.out.println("Here isn't safe");
            strategy.setWizardState(WizardState.PUSHING);
        }
    }

    private void addEdgeBetweenAbsoluteAndGraphPoint(Point2D absPoint, Point2D graphPoint, GraphMapper graphMapper, GameMapGraph graph) {
        if(!graphPoint.equals(absPoint)) {
            GameMapGraph.Node targetNode = graphMapper.map(absPoint);
            graph.addNode(targetNode);
            Vector2D targetToNearEdge = new Vector2D(absPoint, graphPoint);
            GameMapGraph.Edge targetEdge = graphMapper.map(targetToNearEdge);
            graph.addEdge(targetEdge);
        }
    }

    private Point2D getPreviousWaypoint(List<Point2D> waypoints) {
        Point2D firstWaypoint = waypoints.get(0);

        for (int waypointIndex = waypoints.size() - 1; waypointIndex > 0; --waypointIndex) {
            Point2D waypoint = waypoints.get(waypointIndex);

            if (waypoint.getDistanceTo(self) <= POINT_RADIUS) {
                return waypoints.get(waypointIndex - 1);
            }

            if (firstWaypoint.getDistanceTo(waypoint) < firstWaypoint.getDistanceTo(self)) {
                return waypoint;
            }
        }

        return firstWaypoint;
    }

    private Point2D getNextWaypoint(List<Point2D> waypoints) {
        int lastWaypointIndex = waypoints.size() - 1;
        Point2D lastWaypoint = waypoints.get(lastWaypointIndex);

        for (int waypointIndex = 0; waypointIndex < lastWaypointIndex; ++waypointIndex) {
            Point2D waypoint = waypoints.get(waypointIndex);

            if (waypoint.getDistanceTo(self) <= POINT_RADIUS) {
                return waypoints.get(waypointIndex + 1);
            }

            if (lastWaypoint.getDistanceTo(waypoint) < lastWaypoint.getDistanceTo(self)) {
                return waypoint;
            }
        }

        return lastWaypoint;
    }
}
