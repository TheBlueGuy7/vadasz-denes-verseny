package hu.theblueguy7;



import hu.theblueguy7.model.*;

import java.util.*;

public class SimulationEngine {
    private final CellType[][] map;
    private final Rover rover;
    private final PathFinder pathFinder;
    private final StrategyManager strategy;
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

            if (rover.getBattery() >= 2.0) {
            // 1. check if time limit requires heading home
            if (!isHeadingHome) {
                List<Node> pathToHome = pathFinder.findPath(rover.getX(), rover.getY(), startX, startY);
                int distToHome = (pathToHome != null) ? pathToHome.size() : 0;

                int stepsToHome = (int) Math.ceil((double) distToHome / Speed.NORMAL.velocity);
                if (step + stepsToHome + 2 >= totalSteps) {
                    currentPath = pathToHome;
                    isHeadingHome = true;
                    System.out.println("Time limit approaching, rover heading home!");
                }
            }

            // 2. check for mining
            if (!isHeadingHome && map[rover.getX()][rover.getY()].isMineral()) {
                action = "MINING " + map[rover.getX()][rover.getY()].getSymbol();
                consumption = 2.0;
                rover.addMineral();
                map[rover.getX()][rover.getY()] = CellType.GROUND;
            }
            // 3. find target
            else if (currentPath == null || currentPath.isEmpty()) {
                if (!isHeadingHome) {
                    currentPath = findBestMineral(step);
                }
                // if no safe mineral found, head home
                if ((currentPath == null || currentPath.isEmpty()) && (rover.getX() != startX || rover.getY() != startY)) {
                    currentPath = pathFinder.findPath(rover.getX(), rover.getY(), startX, startY);
                    isHeadingHome = true;
                }
            }

            // 4. execute movement
            if (currentPath != null && !currentPath.isEmpty()) {
                // determine speed
                currentSpeed = determineOptimalSpeed(isDay, rover.getBattery(), currentPath.size());
                int blocksToMove = Math.min(currentSpeed.velocity, currentPath.size());

                action = "MOVING (" + currentSpeed + ")";
                consumption = 2.0 * Math.pow(blocksToMove, 2); // [cite: 34]

                for (int i = 0; i < blocksToMove; i++) {
                    Node next = currentPath.remove(0);
                    rover.setPosition(next.x, next.y);
                    rover.addDistance(1);

                    // stop and mine if standing on a mineral
                    if (!isHeadingHome && map[next.x][next.y].isMineral()) {
                        break;
                    }
                }
            }
            } else {
                action = "WAITING FOR CHARGE";
            }

            // 5. update energy
            double charge = isDay ? 10.0 : 0.0; // [cite: 40]
            rover.updateBattery(consumption, charge); // [cite: 41]

            // 6. logging
            String logLine = String.format("[%05.1f h] POS: (%2d,%2d) | BATT: %5.1f%% | DIST: %4d | MINERALS: %2d | ACT: %s",
                    step * 0.5, rover.getX(), rover.getY(), rover.getBattery(), rover.getTotalDistance(), rover.getMinerals(), action);
            FileManager.log(logLine);

            // if battery depleted
        }
    }

        private Speed determineOptimalSpeed(boolean isDay, double battery, int distanceLeft) {
            if (distanceLeft <= 1) return Speed.SLOW;
            if (distanceLeft == 2) return Speed.NORMAL;
            // daytime
            if (isDay) {
                if (battery < 25.0) return Speed.SLOW;

                if (battery > 85.0) return Speed.FAST;

                return Speed.NORMAL;
            }
            // nighttime
            else {
                if (this.isHeadingHome && battery > 40.0) {
                    return Speed.NORMAL;
                }

                if (battery > 90.0) return Speed.NORMAL;
                return Speed.SLOW;
            }
        }

    private List<Node> findBestMineral(int currentStep) {
        List<Node> minerals = new ArrayList<>();

        for (int i = 0; i < 50; i++) {
            for (int j = 0; j < 50; j++) {
                if (map[i][j].isMineral()) {
                    minerals.add(new Node(i, j));
                }
            }
        }

        minerals.sort((a, b) -> {
            int distA = Math.max(Math.abs(a.x - rover.getX()), Math.abs(a.y - rover.getY()));
            int distB = Math.max(Math.abs(b.x - rover.getX()), Math.abs(b.y - rover.getY()));
            return Integer.compare(distA, distB);
        });

        for (Node target : minerals) {
            List<Node> pathToMineral = pathFinder.findPath(rover.getX(), rover.getY(), target.x, target.y);

            if (pathToMineral != null) {
                if (isSafe(pathToMineral, currentStep)) {
                    return pathToMineral;
                }
            }
        }

        return null;
    }

    private boolean isSafe(List<Node> pathToMineral, int currentStep) {
        Speed estSpeed = Speed.NORMAL;

        StrategyManager.RouteEstimate toGoal = strategy.estimateRoute(pathToMineral.size(), estSpeed, currentStep);

        int stepAfterMining = currentStep + toGoal.timeSteps + 1;
        boolean isDayMining = (stepAfterMining % 48) < 32;
        double miningCost = 2.0 - (isDayMining ? 10.0 : 0.0);

        Node mineralPos = pathToMineral. getLast();
        List<Node> backHomePath = pathFinder.findPath(mineralPos.x, mineralPos.y, startX, startY);

        if (backHomePath == null) return false;

        StrategyManager.RouteEstimate backHome = strategy.estimateRoute(backHomePath.size(), estSpeed, stepAfterMining);

        double totalEnergyNeeded = toGoal.energyNeeded + miningCost + backHome.energyNeeded;

        return rover.getBattery() > (totalEnergyNeeded + 5.0);
    }
}
