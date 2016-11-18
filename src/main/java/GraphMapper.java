import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GraphMapper {

    private Map<Point2D, GameMapGraph.Node> nodeToPoint = new HashMap<>();
    private Map<GameMapGraph.Node, Point2D> pointToNode = new HashMap<>();
    private Map<Vector2D, GameMapGraph.Edge> edgeToVector = new HashMap<>();

    public GameMapGraph.Node map(Point2D point) {
        GameMapGraph.Node mappedNode = nodeToPoint.get(point);
        if(mappedNode != null) {
            return mappedNode;
        }
        GameMapGraph.Node node = new GameMapGraph.Node();
        nodeToPoint.put(point, node);
        pointToNode.put(node, point);
        return node;
    }

    public GameMapGraph.Edge map(Vector2D vector) {
        GameMapGraph.Edge mappedEdge = edgeToVector.get(vector);
        if(mappedEdge != null) {
            return mappedEdge;
        }
        GameMapGraph.Node start = nodeToPoint.get(vector.getStart());
        GameMapGraph.Node end = nodeToPoint.get(vector.getEnd());
        mappedEdge = new GameMapGraph.Edge(start, end, vector.getStart().getDistanceTo(vector.getEnd()));
        edgeToVector.put(vector, mappedEdge);
        return mappedEdge;
    }

    public List<Point2D> map(List<GameMapGraph.Node> nodes) {
        return nodes.stream().map(node -> pointToNode.get(node)).collect(Collectors.toList());
    }

}
