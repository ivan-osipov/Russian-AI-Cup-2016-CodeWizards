import java.util.*;
import java.util.stream.Collectors;

public class GameMapGraph {

    private List<Edge> edges;

    private List<Node> nodes;

    private Map<Node, Set<Node>> adjacentNodes;

    private boolean[][] approachibilityMatrix;

    private double[] nodesMarks;
    private Integer[] approachibilityStepsArray;
    private Map<Node, Integer> nodeIndexes;

    int INF = Integer.MAX_VALUE / 2;

    public GameMapGraph() {
        cleanAll();
    }

    private void cleanAll() {
        edges = new ArrayList<>();
        nodes = new ArrayList<>();
        adjacentNodes = new HashMap<>();
        nodeIndexes = new HashMap<>();
    }

    public List<Node> findBestWayDijkstra(Node source, Node target) {
        Preconditions.isNotEmpty(nodes);
        Preconditions.isNotEmpty(edges);
        if(source.equals(target)) {
            return Collections.emptyList();
        }
        initNodeIndexes();
        int sourceIndex = nodeIndexes.get(source);
        int targetIndex = nodeIndexes.get(target);
        Dijkstra.MultiList graph = new Dijkstra.MultiList(nodes.size(), edges.size());
        for (Edge edge : edges) {
            graph.add(nodeIndexes.get(edge.getFirst()), nodeIndexes.get(edge.getSecond()), edge.getWeight());
        }
        Dijkstra dijkstra = new Dijkstra(graph, nodes.size());
        int[] bestWay = dijkstra.dijkstraRMQ(sourceIndex, targetIndex);

        return Arrays.stream(bestWay).mapToObj(idx -> nodes.get(idx)).collect(Collectors.toList());
    }

    /**
     * @return null -> нет пути, empty - выполнялся поиск пути в тот же узел
     */
    public List<Node> findBestWay(Node source, Node target) {
        Preconditions.isNotEmpty(nodes);
        Preconditions.isNotEmpty(edges);
        if(source.equals(target)) {
            return Collections.emptyList();
        }
        initNodeIndexes();
        Integer sourceIndex = nodeIndexes.get(source);
        init(sourceIndex);
        Integer stepsToTarget = wavePropagation(source, target);
        if(stepsToTarget == null) {
            return null;
        }
        return restoreBestWay(stepsToTarget, target, source);
    }

    private List<Node> restoreBestWay(int stepsToTarget, Node target, Node source) {
        int targetNode = nodeIndexes.get(target);
        int sourceNode = nodeIndexes.get(source);
        int node = targetNode;
        List<Node> bestWay = new ArrayList<>();
        bestWay.add(nodes.get(node));
        while (!approachibilityMatrix[node][sourceNode]) {
            for (int i = 0; i < nodes.size(); i++) {
                if(approachibilityStepsArray[i] == stepsToTarget - 1) {
                    node = i;
                    stepsToTarget = stepsToTarget - 1;
                    bestWay.add(nodes.get(node));
                    break;
                }
            }
        }
        bestWay.add(nodes.get(sourceNode));
        Collections.reverse(bestWay);
        return bestWay;
    }

    private Integer wavePropagation(Node from, Node to) {
        int targetIndex = nodeIndexes.get(to);
        Integer stepsToTarget = null;
        int step = 0;
        Set<Integer> remainedIndexes = new HashSet<>(nodes.size());
        remainedIndexes.add(nodeIndexes.get(from));
        while (stepsToTarget == null && !remainedIndexes.isEmpty() && step < nodes.size()) {
            Set<Integer> newRemainedIndexes = new HashSet<>();
            for (Integer node : remainedIndexes) {
                for (int i = 0; i < nodes.size(); i++) {
                    if (feasible(node, i) && approachibilityStepsArray[i] == null) {
                        approachibilityStepsArray[i] = step + 1;
                        newRemainedIndexes.add(i);
                        if(i == targetIndex) {
                            stepsToTarget = step + 1;
                        }
                    }
                }
            }
            step++;
            remainedIndexes = newRemainedIndexes;
        }
        System.out.println("Steps to target: " + stepsToTarget);
        return stepsToTarget;
    }

    private boolean feasible(int node, int adjacentNodeIndex) {
        return adjacentNodeIndex != node && approachibilityMatrix[node][adjacentNodeIndex];
    }

    public void addEdge(Edge edge) {
        Set<Node> firstCollection = adjacentNodes.get(edge.getFirst());
        Set<Node> secondCollection = adjacentNodes.get(edge.getSecond());
        Preconditions.notNull(firstCollection);
        Preconditions.notNull(secondCollection);

        firstCollection.add(edge.getSecond());
        secondCollection.add(edge.getFirst());
        edges.add(edge);
    }

    public void addNode(Node node) {
        adjacentNodes.put(node, new HashSet<>());
        nodes.add(node);
    }

    private void init(int startIndex) {
        initNodeIndexes();
        approachibilityMatrixInit();
        approachibilityStepsMatrixInit(startIndex);
    }

    private void approachibilityStepsMatrixInit(int node) {
        approachibilityStepsArray = new Integer[nodes.size()];
        approachibilityStepsArray[node] = 0;
    }

    private void initNodeIndexes() {
        for (int i = 0; i < nodes.size(); i++) {
            nodeIndexes.put(nodes.get(i), i);
        }
    }

    private void approachibilityMatrixInit() {
        approachibilityMatrix = new boolean[nodes.size()][nodes.size()];
        nodesMarks = new double[nodes.size()];
        for (int i = 0; i < approachibilityMatrix.length; i++) {
            Arrays.fill(approachibilityMatrix[i], false);
        }
        for (Edge edge : edges) {
            Integer first = nodeIndexes.get(edge.getFirst());
            Integer second = nodeIndexes.get(edge.getSecond());
            approachibilityMatrix[first][second] = true;
            approachibilityMatrix[second][first] = true;
        }
        for (int i = 0; i < nodes.size(); i++) {
            approachibilityMatrix[i][i] = true;
        }
    }

    /*
     Конкретная зона или позиция игрока
     */
    public static class Node {

        private int zoneId;

        public Node(int zoneId) {
            this.zoneId = zoneId;
        }

        public int getZoneId() {
            return zoneId;
        }

        public void setZoneId(int zoneId) {
            this.zoneId = zoneId;
        }


        @Override
        public String toString() {
            return String.valueOf(zoneId);
        }
    }

    /*
     Длина пути между узлами
     */
    public static class Edge {

        private Node first;

        private Node second;

        private double weight;

        public Edge(Node first, Node second, double weight) {
            Preconditions.notNull(first);
            Preconditions.notNull(second);
            this.first = first;
            this.second = second;
            this.weight = weight;
        }

        public Node getFirst() {
            return first;
        }

        public Node getSecond() {
            return second;
        }

        public double getWeight() {
            return weight;
        }
    }

}
