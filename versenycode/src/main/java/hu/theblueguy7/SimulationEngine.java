package hu.theblueguy7;



import hu.theblueguy7.model.CellType;
import hu.theblueguy7.model.Node;
import hu.theblueguy7.model.Rover;
import hu.theblueguy7.model.Speed;

import java.util.*;

public class SimulationEngine {
    private final CellType[][] map;
    private final Rover rover;
    private final PathFinder pathFinder;
    private StrategyManager strategy;
    private List<Node> currentPath = new ArrayList<>();
    private final int startX, startY;
    private boolean isHeadingHome = false;

    public SimulationEngine(CellType[][] map, int startX, int startY) {
        this.map = map;
        this.startX = startX;
        this.startY = startY;
        this.rover = new Rover(startX, startY);
        this.pathFinder = new PathFinder(map);
        this.strategy = new StrategyManager(pathFinder);
    }

    public void runSimulation(int maxHours) {
        int totalSteps = maxHours * 2;
        FileManager.resetLog();

        // main loop
        for (int step = 0; step < totalSteps; step++) {
            boolean isDay = (step % 48) < 32;
            String action = "STANDBY";
            double consumption = 1.0; // standby fogyasztas
            Speed currentSpeed = Speed.SLOW;

            // 1. idokorlat miatti hazateres ellenorzese
            if (!isHeadingHome) {
                List<Node> pathToHome = pathFinder.findPath(rover.getX(), rover.getY(), startX, startY);
                int distToHome = (pathToHome != null) ? pathToHome.size() : 0;

                int stepsToHome = (int) Math.ceil((double) distToHome / Speed.NORMAL.velocity);
                if (step + stepsToHome + 2 >= totalSteps) {
                    currentPath = pathToHome;
                    isHeadingHome = true;
                    System.out.println("Időkorlát közeleg, a rover elindult haza!");
                }
            }

            // 2. banyaszat ellenorzese
            if (!isHeadingHome && map[rover.getX()][rover.getY()].isMineral()) {
                action = "MINING " + map[rover.getX()][rover.getY()].getSymbol();
                consumption = 2.0;
                rover.addMineral();
                map[rover.getX()][rover.getY()] = CellType.GROUND;
            }
            // 3. celkereses
            else if (currentPath == null || currentPath.isEmpty()) {
                if (!isHeadingHome) {
                    currentPath = findBestMineral(step);
                }
                // ha nincs biztonsagos asvany hazamegyunk
                if ((currentPath == null || currentPath.isEmpty()) && (rover.getX() != startX || rover.getY() != startY)) {
                    currentPath = pathFinder.findPath(rover.getX(), rover.getY(), startX, startY);
                    isHeadingHome = true;
                }
            }

            // 4. mozgas vegrehastasa
            if (currentPath != null && !currentPath.isEmpty()) {
                // sebesseg eldontese
                currentSpeed = determineOptimalSpeed(isDay, rover.getBattery(), currentPath.size());
                int blocksToMove = Math.min(currentSpeed.velocity, currentPath.size());

                action = "MOVING (" + currentSpeed + ")";
                consumption = 2.0 * Math.pow(blocksToMove, 2); // [cite: 34]

                for (int i = 0; i < blocksToMove; i++) {
                    Node next = currentPath.remove(0);
                    rover.setPosition(next.x, next.y);
                    rover.addDistance(1);

                    // ha asvanyon allunk akkor megallunk es kibanyasszuk
                    if (!isHeadingHome && map[next.x][next.y].isMineral()) {
                        break;
                    }
                }
            }

            // 5. energia frissitese
            double charge = isDay ? 10.0 : 0.0; // [cite: 40]
            rover.updateBattery(consumption, charge); // [cite: 41]

            // 6. logolas
            String logLine = String.format("[%04d min] POS: (%2d,%2d) | BATT: %5.1f%% | DIST: %4d | MIN: %2d | ACT: %s",
                    step * 30, rover.getX(), rover.getY(), rover.getBattery(), rover.getTotalDistance(), rover.getMinerals(), action);
            FileManager.log(logLine);

            // ha lemerult
            if (rover.getBattery() <= 0) {
                FileManager.log("CRITICAL FAILURE: BATTERY DEAD");
                break;
            }

            // ha hazaert
            if (isHeadingHome && rover.getX() == startX && rover.getY() == startY) {
                FileManager.log("MISSION ACCOMPLISHED: ROVER RETURNED TO BASE");
            }
        }
    }

    // sebesseg eldontese az uthossz fuggvenyeben !NEM VEGLEGES!
    private Speed determineOptimalSpeed(boolean isDay, double battery, int distanceLeft) {
        // ha mar csak 1 lepes van hatra akkor nem kell gyorsan menni
        if (distanceLeft == 1) return Speed.SLOW;

        if (isDay) {
            return (battery > 50 && distanceLeft >= 3) ? Speed.FAST : Speed.NORMAL;
        } else {
            return (battery < 30) ? Speed.SLOW : Speed.NORMAL;
        }
    }

    private List<Node> findBestMineral(int currentStep) {
        // vegigmegy a terkepen, megkeresi a legkozelebbi asvanyt amihez meg van eleg energia
        // StrategyManager.estimateRoute
        return null; // WORK IN PROGRESS...
    }
}