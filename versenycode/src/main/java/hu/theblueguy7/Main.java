package hu.theblueguy7;


import hu.theblueguy7.model.CellType;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        CellType[][] map = FileManager.loadMap("./map.csv");
        SimulationEngine simulationEngine = new SimulationEngine(map, 0, 0);
        simulationEngine.runSimulation(48);
    }
}