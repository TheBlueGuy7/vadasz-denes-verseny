package hu.theblueguy7.model;

import java.util.*;

public class Node implements Comparable<Node> {
    public int x, y;
    public double gCost; // eddigi ut
    public double hCost; // estimated ut a celig
    public Node parent;  // parent node

    public Node(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public double getFCost() {
        return gCost + hCost;
    }

    @Override
    public int compareTo(Node other) {
        return Double.compare(this.getFCost(), other.getFCost());
    }
}
