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

                    if (nx >= 0 && nx < 50 && ny >= 0 && ny < 50 && map[nx][ny].isPassable() && !closedSet[nx][ny]) {
                        // FIX: az atlos lepes ugyan annyiba kerul mint az egyenes
                        Node neighbor = new Node(nx, ny);
                        neighbor.gCost = current.gCost + 1;
                        neighbor.hCost = Math.max(Math.abs(nx - targetX), Math.abs(ny - targetY)); // Chebyshev-distance
                        neighbor.parent = current;

                        openSet.add(neighbor);
                    }
                }
            }
        }
        return null;
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