package hu.theblueguy7.model;

public class Rover {
    private int x, y;
    private double battery = 100.0;
    private int mineralsCollected = 0;
    private int totalDistance = 0; // for logging

    public Rover(int startX, int startY) {
        this.x = startX;
        this.y = startY;
    }

    public void updateBattery(double consumption, double charge) {
        battery = battery - consumption + charge;
        if (battery > 100.0) battery = 100.0;
        if (battery < 0.0) battery = 0.0;
    }

    public void setPosition(int x, int y) { this.x = x; this.y = y; }
    public void addDistance(int d) { this.totalDistance += d; }
    public void addMineral() { this.mineralsCollected++; }

    public int getX() { return x; }
    public int getY() { return y; }
    public double getBattery() { return battery; }
    public int getMinerals() { return mineralsCollected; }
    public int getTotalDistance() { return totalDistance; }
}