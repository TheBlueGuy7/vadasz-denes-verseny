package hu.theblueguy7.model;

public enum Speed {
    SLOW(1),
    NORMAL(2),
    FAST(3);

    public final int blocksPerHalfHour;
    Speed(int value) { this.blocksPerHalfHour = value; }
}
