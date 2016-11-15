import java.util.Stack;

import static java.util.Arrays.fill;

public class Dijkstra {

    private static int INF = Integer.MAX_VALUE / 2; // "Бесконечность"
    private int nodeAmount; // количество вершин
    private MultiList graph; // описание графа

    public Dijkstra(MultiList graph, int nodeAmount) {
        this.nodeAmount = nodeAmount;
        this.graph = graph;
    }

    /* Алгоритм Дейкстры за O(E log V) */
    public int[] dijkstraRMQ(int start, int end) {
        boolean[] used = new boolean[nodeAmount]; // массив пометок
        int[] prev = new int[nodeAmount]; // массив предков
        double[] dist = new double[nodeAmount]; // массив расстояний
        RMQ rmq = new RMQ(nodeAmount);

                 /* Инициализация */
        fill(prev, -1);
        fill(dist, INF);
        rmq.set(start, dist[start] = 0);

        for (; ; ) {
            int v = rmq.minIndex(); // выбираем ближайшую вершину
            if (v == -1 || v == end) break; // если она не найдена, или является конечной, то выходим

            used[v] = true; // помечаем выбранную вершину
            rmq.set(v, INF); // и сбрасываем ее значение в RMQ

            for (int i = graph.head[v]; i != 0; i = graph.next[i]) { // проходим пр смежным вершинам
                int nv = graph.vert[i];
                double cost = graph.cost[i];
                if (!used[nv] && dist[nv] > dist[v] + cost) { // если можем улучшить оценку расстояния
                    rmq.set(nv, dist[nv] = dist[v] + cost); // улучшаем ее
                    prev[nv] = v; // помечаем предка
                }
            }
        }

                 /* Восстановление пути */
        Stack<Integer> stack = new Stack<>();
        for (int v = end; v != -1; v = prev[v]) {
            stack.push(v);
        }
        int[] sp = new int[stack.size()];
        for (int i = 0; i < sp.length; i++) {
            sp[i] = stack.pop();
        }

                 /* Вывод результата */
//        System.out.printf("Кратчайшее расстояние между %d и %d = %d%n", start + 1, end + 1, dist[end]);
//        System.out.println("Кратчайший путь: " + Arrays.toString(sp));
        return sp;
    }

    /* Класс списка с несколькими головами */
    public static class MultiList {
        int[] head;
        int[] next;
        int[] vert;
        double[] cost;
        int cnt = 1;

        MultiList(int vNum, int eNum) {
            head = new int[vNum];
            next = new int[eNum + 1];
            vert = new int[eNum + 1];
            cost = new double[eNum + 1];
        }

        void add(int u, int v, double w) {
            next[cnt] = head[u];
            vert[cnt] = v;
            cost[cnt] = w;
            head[u] = cnt++;
        }
    }

    /* Класс RMQ */
    public static class RMQ {
        int n;
        double[] val;
        int[] ind;

        RMQ(int size) {
            n = size;
            val = new double[2 * n];
            ind = new int[2 * n];
            fill(val, INF);
            for (int i = 0; i < n; i++)
                ind[n + i] = i;
        }

        void set(int index, double value) {
            val[n + index] = value;
            for (int v = (n + index) / 2; v > 0; v /= 2) {
                int l = 2 * v;
                int r = l + 1;
                if (val[l] <= val[r]) {
                    val[v] = val[l];
                    ind[v] = ind[l];
                } else {
                    val[v] = val[r];
                    ind[v] = ind[r];
                }
            }
        }

        int minIndex() {
            return val[1] < INF ? ind[1] : -1;
        }


    }
}
