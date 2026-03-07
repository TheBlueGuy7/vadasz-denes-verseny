package hu.theblueguy7.model;

import javafx.util.Pair;

public class Rover {
    private int x, y;
    private double battery = 100.0;
    private int collectedMinerals = 0;
    private final int K_CONST = 2;

    public Rover(int startX, int startY) {
        this.x = startX;
        this.y = startY;
    }


    public double calculateMoveConsumption(Speed speed) {
        return K_CONST * Math.pow(speed.blocksPerHalfHour, 2);
    }


    public void updateBattery(double consumption, boolean isDay) {
        if (isDay) {
            battery += 10;
        }
        battery -= consumption;


        if (battery > 100) battery = 100;
        if (battery < 0) battery = 0;
    }


    public double getBattery() { return battery; }
    public void setBattery(int battery) { this.battery = battery; }

    public int getX() { return x; }
    public void setX(int x) { this.x = x; }

    public int getY() { return y; }
    public void setY(int y) { this.y = y; }

    public int getCollectedMinerals() { return collectedMinerals; }
    public void setCollectedMinerals(int collectedMinerals) { this.collectedMinerals = collectedMinerals; }
}
