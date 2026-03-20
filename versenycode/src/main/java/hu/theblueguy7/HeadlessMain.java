package hu.theblueguy7;

import hu.theblueguy7.model.CellType;

import java.io.IOException;

public class HeadlessMain {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java -jar rover-headless.jar <hours> [map.csv]");
            System.exit(1);
        }

        try {
            String mapPath = args.length >= 2 ? args[1] : "./map.csv";
            int hours = Integer.parseInt(args[0]);

            System.out.println("=== Mars Rover Simulation ===");
            System.out.println("Loading map from: " + mapPath);
            System.out.println("Simulation duration: " + hours + " hours");

            CellType[][] map = FileManager.loadMap(mapPath);
            int[] startPos = findStartLocation(map);

            System.out.println("Start position found at: (" + startPos[0] + ", " + startPos[1] + ")");
            System.out.println("Starting simulation...");

            SimulationEngine engine = new SimulationEngine(map, startPos[0], startPos[1]);
            engine.runSimulation(hours);

            System.out.println("Simulation finished. Check 'simulation.log' for results.");
        } catch (NumberFormatException e) {
            System.err.println("Invalid hours value: must be an integer.");
        } catch (IOException e) {
            System.err.println("Error loading map file: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static int[] findStartLocation(CellType[][] map) {
        for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map[i].length; j++) {
                if (map[i][j] == CellType.START) {
                    return new int[]{i, j};
                }
            }
        }
        System.out.println("Warning: No 'S' cell found on map. Defaulting to (0,0).");
        return new int[]{0, 0};
    }
}
