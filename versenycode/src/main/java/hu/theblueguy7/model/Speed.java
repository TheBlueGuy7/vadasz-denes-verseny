package hu.theblueguy7.model;

public enum Speed {
    SLOW(1), NORMAL(2), FAST(3);
    public final int velocity;
    Speed(int v) { this.velocity = v; }
}