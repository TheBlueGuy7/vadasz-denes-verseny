package hu.theblueguy7.model;

import java.util.List;

public class SimulationFrame {
    public final int step;
    public final double timeHours;
    public final int roverX, roverY;
    public final double battery;
    public final Speed speed;
    public final int mineralsCollected;
    public final int totalDistance;
    public final String action;
    public final boolean isDay;
    public final CellType[][] mapSnapshot;
    public final List<int[]> currentPath;
    public final String fileLogLine;
    public final String consoleLogLine;

    public SimulationFrame(int step, double timeHours, int roverX, int roverY,
                           double battery, Speed speed, int mineralsCollected, int totalDistance,
                           String action, boolean isDay, CellType[][] mapSnapshot,
                           List<int[]> currentPath, String fileLogLine, String consoleLogLine) {
        this.step = step;
        this.timeHours = timeHours;
        this.roverX = roverX;
        this.roverY = roverY;
        this.battery = battery;
        this.speed = speed;
        this.mineralsCollected = mineralsCollected;
        this.totalDistance = totalDistance;
        this.action = action;
        this.isDay = isDay;
        this.mapSnapshot = mapSnapshot;
        this.currentPath = currentPath;
        this.fileLogLine = fileLogLine;
        this.consoleLogLine = consoleLogLine;
    }

    public static CellType[][] deepCopyMap(CellType[][] original) {
        CellType[][] copy = new CellType[original.length][];
        for (int i = 0; i < original.length; i++) {
            copy[i] = original[i].clone();
        }
        return copy;
    }
}
