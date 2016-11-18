import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class GraphTester {
    private GameMapGraph graph = new GameMapGraph();

    private GameMapGraph.Node one = new GameMapGraph.Node();
    private GameMapGraph.Node two = new GameMapGraph.Node();
    private GameMapGraph.Node three = new GameMapGraph.Node();
    private GameMapGraph.Node four = new GameMapGraph.Node();

    @Before
    public void setUp() {
        graph.addNode(one);
        graph.addNode(two);
        graph.addNode(three);
        graph.addNode(four);
    }

    @Test
    public void longWayBest() {

        graph.addEdge(new GameMapGraph.Edge(one, two, 0.2));
        graph.addEdge(new GameMapGraph.Edge(two, three, 2.3));
        graph.addEdge(new GameMapGraph.Edge(three, four, 1));
        graph.addEdge(new GameMapGraph.Edge(one, three, 3));

        List<GameMapGraph.Node> bestWay = graph.findBestWayDijkstra(one, four);
        System.out.println(bestWay);

    }

    @Test
    public void shortWayBest() {
        graph.addEdge(new GameMapGraph.Edge(one, two, 0.2));
        graph.addEdge(new GameMapGraph.Edge(two, three, 2.3));
        graph.addEdge(new GameMapGraph.Edge(three, four, 1));
        graph.addEdge(new GameMapGraph.Edge(one, three, 2));

        List<GameMapGraph.Node> bestWay = graph.findBestWayDijkstra(one, four);
        System.out.println(bestWay);

    }

    @Test
    public void reverseWayBest() {
        graph.addEdge(new GameMapGraph.Edge(one, two, 30));
        graph.addEdge(new GameMapGraph.Edge(two, three, 2.3));
        graph.addEdge(new GameMapGraph.Edge(three, two, 2.3));
        graph.addEdge(new GameMapGraph.Edge(three, four, 1));
        graph.addEdge(new GameMapGraph.Edge(one, three, 2));

        List<GameMapGraph.Node> bestWay = graph.findBestWayDijkstra(one, two);
        System.out.println(bestWay);

    }

}
