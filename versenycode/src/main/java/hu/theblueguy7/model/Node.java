package hu.theblueguy7.model;

public class Node implements Comparable<Node> {
    public int x, y;
    public double gCost, hCost; // g: distance so far, h: heuristic
    public Node parent;

    public Node(int x, int y) { this.x = x; this.y = y; }
    public double getFCost() { return gCost + hCost; }

    @Override
    public int compareTo(Node o) { return Double.compare(this.getFCost(), o.getFCost()); }
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Node n)) return false;
        return n.x == this.x && n.y == this.y;
    }
}