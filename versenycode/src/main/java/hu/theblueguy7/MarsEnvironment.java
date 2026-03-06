package hu.theblueguy7;

import hu.theblueguy7.model.CellType;

public class MarsEnvironment {
    private final int SIZE = 50; //
    private CellType[][] grid = new CellType[SIZE][SIZE];
    private int currentHalfHours = 0;

    public boolean isDaytime() {
        int cycleTime = currentHalfHours % 48;
        return cycleTime < 32;
    }

    public void nextStep() {
        currentHalfHours++;
    }
}
