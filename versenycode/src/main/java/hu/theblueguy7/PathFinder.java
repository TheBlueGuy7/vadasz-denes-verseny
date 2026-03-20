package hu.theblueguy7;

import hu.theblueguy7.model.CellType;
import hu.theblueguy7.model.Node;

import java.util.*;

public class PathFinder {
    private final CellType[][] map;

    public PathFinder(CellType[][] map) {
        this.map = map;
    }

    public List<Node> findPath(int startX, int startY, int targetX, int targetY) {
        PriorityQueue<Node> openSet = new PriorityQueue<>();
        boolean[][] closedSet = new boolean[50][50];

        Node startNode = new Node(startX, startY);
        openSet.add(startNode);

        while (!openSet.isEmpty()) {
            Node current = openSet.poll();
            if (current.x == targetX && current.y == targetY) return retrace(current);

            closedSet[current.x][current.y] = true;

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (dx == 0 && dy == 0) continue;

                    int nx = current.x + dx;
                    int ny = current.y + dy;

                    if (nx >= 0 && nx < 50 && ny >= 0 && ny < 50 && map[nx][ny].isPassable() && !closedSet[nx][ny]
                            && (dx == 0 || dy == 0 || (map[current.x + dx][current.y].isPassable() && map[current.x][current.y + dy].isPassable()))) {
                        double moveCost = (dx == 0 || dy == 0) ? 1.0 : Math.sqrt(2);
                        Node neighbor = new Node(nx, ny);
                        neighbor.gCost = current.gCost + moveCost;

                        // Octile distance heuristic
                        double xDist = Math.abs(nx - targetX);
                        double yDist = Math.abs(ny - targetY);
                        neighbor.hCost = (Math.sqrt(2) - 1) * Math.min(xDist, yDist) + Math.max(xDist, yDist);
                        
                        neighbor.parent = current;

                        openSet.add(neighbor);
                    }
                }
            }
        }
        return null;
    }

    public double[][] distanceMap(int startX, int startY) {
        double[][] dist = new double[50][50];
        for (double[] row : dist) Arrays.fill(row, Double.MAX_VALUE);
        dist[startX][startY] = 0;

        PriorityQueue<Node> open = new PriorityQueue<>(Comparator.comparingDouble(n -> n.gCost));
        open.add(new Node(startX, startY));

        boolean[][] visited = new boolean[50][50];

        while (!open.isEmpty()) {
            Node cur = open.poll();
            if (visited[cur.x][cur.y]) continue;
            visited[cur.x][cur.y] = true;

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (dx == 0 && dy == 0) continue;
                    int nx = cur.x + dx;
                    int ny = cur.y + dy;
                    if (nx < 0 || nx >= 50 || ny < 0 || ny >= 50) continue;
                    if (!map[nx][ny].isPassable() || visited[nx][ny]) continue;
                    // diagonal corner-cutting check
                    if (dx != 0 && dy != 0 && (!map[cur.x + dx][cur.y].isPassable() || !map[cur.x][cur.y + dy].isPassable()))
                        continue;

                    double cost = (dx == 0 || dy == 0) ? 1.0 : Math.sqrt(2);
                    double newDist = cur.gCost + cost;
                    if (newDist < dist[nx][ny]) {
                        dist[nx][ny] = newDist;
                        Node neighbor = new Node(nx, ny);
                        neighbor.gCost = newDist;
                        open.add(neighbor);
                    }
                }
            }
        }
        return dist;
    }

    private List<Node> retrace(Node n) {
        List<Node> path = new ArrayList<>();
        while (n.parent != null) {
            path.addFirst(n);
            n = n.parent;
        }
        return path;
    }
}