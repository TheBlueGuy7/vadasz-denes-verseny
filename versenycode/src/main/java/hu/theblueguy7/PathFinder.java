package hu.theblueguy7;

import hu.theblueguy7.model.CellType;
import hu.theblueguy7.model.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

public class PathFinder {
    private CellType[][] map;

    public PathFinder(CellType[][] map) {
        this.map = map;
    }

    public List<Node> findPath(int startX, int startY, int targetX, int targetY) {
        PriorityQueue<Node> openSet = new PriorityQueue<>();
        boolean[][] closedSet = new boolean[50][50];

        Node startNode = new Node(startX, startY);
        Node targetNode = new Node(targetX, targetY);
        openSet.add(startNode);

        while (!openSet.isEmpty()) {
            Node current = openSet.poll();

            if (current.x == targetX && current.y == targetY) {
                return retracePath(current);
            }

            closedSet[current.x][current.y] = true;

            // szomszedok megviszgalasa
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (dx == 0 && dy == 0) continue;

                    int nextX = current.x + dx;
                    int nextY = current.y + dy;

                    // terkep szele vagy akadaly ellenorzes
                    if (isValid(nextX, nextY) && !closedSet[nextX][nextY] && map[nextX][nextY] != CellType.OBSTACLE) {
                        double moveCost = (dx == 0 || dy == 0) ? 1.0 : 1.414; // atlosan hosszabb a tavolsag
                        double newGCost = current.gCost + moveCost;

                        Node neighbor = new Node(nextX, nextY);
                        neighbor.gCost = newGCost;
                        neighbor.hCost = estimateDistance(nextX, nextY, targetX, targetY);
                        neighbor.parent = current;

                        openSet.add(neighbor);
                    }
                }
            }
        }
        return null;
    }

    private double estimateDistance(int x1, int y1, int x2, int y2) {
        // csebisev tavolsag kiszamolas
        return Math.max(Math.abs(x1 - x2), Math.abs(y1 - y2));
    }

    private boolean isValid(int x, int y) {
        return x >= 0 && x < 50 && y >= 0 && y < 50;
    }

    private List<Node> retracePath(Node endNode) {
        List<Node> path = new ArrayList<>();
        Node current = endNode;
        while (current != null) {
            path.add(0, current);
            current = current.parent;
        }
        return path;
    }
}