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

    public List<SimulationFrame> preCompute(int maxHours) {
        List<SimulationFrame> frames = new ArrayList<>();
        int totalSteps = maxHours * 2 + 1;
        boolean wasCharging = false;
        boolean reachedHome = false;
        int lastMineralCount = 0;
        boolean wasDay = true;

        // initial frame: rover at starting position before any movement
        frames.add(new SimulationFrame(
                0, 0.0, rover.getX(), rover.getY(),
                rover.getBattery(), Speed.SLOW, rover.getMinerals(), rover.getTotalDistance(),
                "DEPLOY", true, SimulationFrame.deepCopyMap(map),
                new ArrayList<>(),
                String.format("[%05.1f h] POS: (%2d,%2d) | BATT: %5.1f%% | DIST: %4d | MINERALS: %2d | ACT: %s",
                        0.0, rover.getX(), rover.getY(), rover.getBattery(), rover.getTotalDistance(), rover.getMinerals(), "DEPLOY"),
                "Mission started! Rover deployed at (" + startX + ", " + startY + ")"
        ));

        for (int step = 1; step < totalSteps; step++) {
            boolean isDay = (step % 48) < 32;
            String action = "STANDBY";
            double consumption = 1.0;
            Speed currentSpeed = Speed.SLOW;
            StringBuilder console = new StringBuilder();

            // day/night transitions
            if (isDay && !wasDay) {
                console.append("Sunrise - solar panels charging\n");
            } else if (!isDay && wasDay) {
                console.append("Nightfall - solar panels offline\n");
            }
            wasDay = isDay;

            if (rover.getBattery() >= 2.0) {
                if (wasCharging) {
                    console.append("Battery recharged to " + String.format("%.1f", rover.getBattery()) + "%, resuming operations\n");
                    wasCharging = false;
                }

                if (!isHeadingHome) {
                    List<Node> pathToHome = pathFinder.findPath(rover.getX(), rover.getY(), startX, startY);
                    int distToHome = (pathToHome != null) ? pathToHome.size() : 0;
                    int stepsToHome = (int) Math.ceil((double) distToHome / Speed.NORMAL.velocity);
                    if (step + stepsToHome + 2 >= totalSteps) {
                        currentPath = pathToHome;
                        isHeadingHome = true;
                        console.append("Time limit approaching, heading home!\n");
                    }
                }

                if (!isHeadingHome && map[rover.getX()][rover.getY()].isMineral()) {
                    char mineralType = map[rover.getX()][rover.getY()].getSymbol();
                    action = "MINING " + mineralType;
                    consumption = 2.0;
                    rover.addMineral();
                    map[rover.getX()][rover.getY()] = CellType.GROUND;
                    console.append("Collected mineral " + mineralType + " (#" + rover.getMinerals() + ")\n");
                    // After mining, re-evaluate: find the next best mineral from here
                    currentPath = null;
                } else if (currentPath == null || currentPath.isEmpty()) {
                    if (!isHeadingHome) {
                        currentPath = findBestMineral(step);
                        if (currentPath != null && !currentPath.isEmpty()) {
                            Node target = currentPath.getLast();
                            console.append("New target: mineral at (" + target.x + ", " + target.y + "), distance: " + currentPath.size() + "\n");
                        }
                    }
                    if ((currentPath == null || currentPath.isEmpty()) && (rover.getX() != startX || rover.getY() != startY)) {
                        currentPath = pathFinder.findPath(rover.getX(), rover.getY(), startX, startY);
                        isHeadingHome = true;
                        console.append("No safe minerals remaining, heading home\n");
                    }
                } else if (!isHeadingHome) {
                    // Check if there's a closer mineral than current target
                    Node currentTarget = currentPath.getLast();
                    int remainingDist = currentPath.size();
                    Node nearby = findNearbyMineral(remainingDist, step);
                    if (nearby != null && !(nearby.x == currentTarget.x && nearby.y == currentTarget.y)) {
                        List<Node> detourPath = pathFinder.findPath(rover.getX(), rover.getY(), nearby.x, nearby.y);
                        if (detourPath != null) {
                            currentPath = detourPath;
                            console.append("Rerouting to closer mineral at (" + nearby.x + ", " + nearby.y + "), distance: " + detourPath.size() + "\n");
                        }
                    }
                }

                if (currentPath != null && !currentPath.isEmpty()) {
                    currentSpeed = determineOptimalSpeed(isDay, rover.getBattery(), currentPath.size());
                    int blocksToMove = Math.min(currentSpeed.velocity, currentPath.size());
                    action = "MOVING (" + currentSpeed + ")";
                    consumption = 2.0 * Math.pow(blocksToMove, 2);

                    for (int i = 0; i < blocksToMove; i++) {
                        Node next = currentPath.remove(0);
                        rover.setPosition(next.x, next.y);
                        rover.addDistance(1);
                        if (!isHeadingHome && map[next.x][next.y].isMineral()) {
                            break;
                        }
                    }
                }

                // arrived home
                if (isHeadingHome && !reachedHome && rover.getX() == startX && rover.getY() == startY) {
                    reachedHome = true;
                    console.append("Rover arrived at base!\n");
                }
            } else {
                action = "WAITING FOR CHARGE";
                if (!wasCharging) {
                    console.append("Battery critical (" + String.format("%.1f", rover.getBattery()) + "%), waiting for charge\n");
                    wasCharging = true;
                }
            }

            double charge = isDay ? 10.0 : 0.0;
            rover.updateBattery(consumption, charge);

            // battery warnings
            double bat = rover.getBattery();
            if (bat <= 0.0) {
                console.append("Battery depleted!\n");
            } else if (bat < 10.0 && bat + consumption - charge >= 10.0) {
                console.append("Battery low: " + String.format("%.1f", bat) + "%\n");
            }

            String logLine = String.format("[%05.1f h] POS: (%2d,%2d) | BATT: %5.1f%% | DIST: %4d | MINERALS: %2d | ACT: %s",
                    step * 0.5, rover.getX(), rover.getY(), rover.getBattery(), rover.getTotalDistance(), rover.getMinerals(), action);

            List<int[]> pathCopy = new ArrayList<>();
            if (currentPath != null) {
                for (Node n : currentPath) {
                    pathCopy.add(new int[]{n.x, n.y});
                }
            }

            // last frame: mission complete
            String consoleMsg = console.isEmpty() ? null : console.toString().stripTrailing();
            if (step == totalSteps - 1) {
                String endMsg = "Mission complete! Minerals: " + rover.getMinerals() + " | Distance: " + rover.getTotalDistance() + " | Battery: " + String.format("%.1f", rover.getBattery()) + "%";
                consoleMsg = consoleMsg == null ? endMsg : consoleMsg + "\n" + endMsg;
            }

            frames.add(new SimulationFrame(
                    step, step * 0.5, rover.getX(), rover.getY(),
                    rover.getBattery(), currentSpeed, rover.getMinerals(), rover.getTotalDistance(),
                    action, isDay, SimulationFrame.deepCopyMap(map),
                    pathCopy, logLine, consoleMsg
            ));
        }

        return frames;
    }

    public void runSimulation(int maxHours) {
        int totalSteps = maxHours * 2;
        FileManager.resetLog();

        // main loop
        for (int step = 0; step < totalSteps; step++) {
            boolean isDay = (step % 48) < 32;
            String action = "STANDBY";
            double consumption = 1.0; // standby consumption
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
                currentPath = null;
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
            } else if (!isHeadingHome) {
                // Check if there's a closer mineral than current target
                Node currentTarget = currentPath.getLast();
                int remainingDist = currentPath.size();
                Node nearby = findNearbyMineral(remainingDist, step);
                if (nearby != null && !(nearby.x == currentTarget.x && nearby.y == currentTarget.y)) {
                    List<Node> detourPath = pathFinder.findPath(rover.getX(), rover.getY(), nearby.x, nearby.y);
                    if (detourPath != null) {
                        currentPath = detourPath;
                    }
                }
            }

            // 4. execute movement
            if (currentPath != null && !currentPath.isEmpty()) {
                // determine speed
                currentSpeed = determineOptimalSpeed(isDay, rover.getBattery(), currentPath.size());
                int blocksToMove = Math.min(currentSpeed.velocity, currentPath.size());

                action = "MOVING (" + currentSpeed + ")";
                consumption = 2.0 * Math.pow(blocksToMove, 2);

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
            double charge = isDay ? 10.0 : 0.0;
            rover.updateBattery(consumption, charge);

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

    private Node findNearbyMineral(int currentTargetDist, int currentStep) {
        double[][] distFromRover = pathFinder.distanceMap(rover.getX(), rover.getY());
        double[][] distFromHome = pathFinder.distanceMap(startX, startY);
        Node best = null;
        double bestDist = currentTargetDist;

        for (int i = 0; i < 50; i++) {
            for (int j = 0; j < 50; j++) {
                if (map[i][j].isMineral()
                        && distFromRover[i][j] < bestDist
                        && distFromHome[i][j] < Double.MAX_VALUE
                        && canAfford((int) Math.ceil(distFromRover[i][j]),
                                     (int) Math.ceil(distFromHome[i][j]), currentStep)) {
                    bestDist = distFromRover[i][j];
                    best = new Node(i, j);
                }
            }
        }
        return best;
    }

    private List<Node> findBestMineral(int currentStep) {
        // Flood-fill from rover to get real distances to all reachable cells
        double[][] distFromRover = pathFinder.distanceMap(rover.getX(), rover.getY());
        // Flood-fill from home to get real distances from each cell back to base
        double[][] distFromHome = pathFinder.distanceMap(startX, startY);

        // Collect all reachable minerals
        List<Node> minerals = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            for (int j = 0; j < 50; j++) {
                if (map[i][j].isMineral()
                        && distFromRover[i][j] < Double.MAX_VALUE
                        && distFromHome[i][j] < Double.MAX_VALUE) {
                    Node n = new Node(i, j);
                    n.gCost = distFromRover[i][j];
                    minerals.add(n);
                }
            }
        }

        // sort by distance from rover (nearest first)
        minerals.sort(Comparator.comparingDouble(n -> n.gCost));

        // try each mineral, nearest first
        for (Node target : minerals) {
            int pathLen = (int) Math.ceil(target.gCost);
            int homeLen = (int) Math.ceil(distFromHome[target.x][target.y]);

            if (canAfford(pathLen, homeLen, currentStep)) {
                List<Node> path = pathFinder.findPath(rover.getX(), rover.getY(), target.x, target.y);
                if (path != null) return path;
            }
        }

        return null;
    }

    private boolean canAfford(int pathLen, int homeLen, int currentStep) {
        double battery = rover.getBattery();
        int step = currentStep;

        for (int i = 0; i < pathLen; i++) {
            boolean isDay = (step % 48) < 32;
            double cost = 2.0 - (isDay ? 10.0 : 0.0);
            battery -= cost;
            if (battery <= 0) return false;
            step++;
        }

        boolean isDayMining = (step % 48) < 32;
        battery -= 2.0 - (isDayMining ? 10.0 : 0.0);
        if (battery <= 0) return false;
        step++;

        for (int i = 0; i < homeLen; i++) {
            boolean isDay = (step % 48) < 32;
            double cost = 2.0 - (isDay ? 10.0 : 0.0);
            battery -= cost;
            if (battery <= 0) return false;
            step++;
        }

        return battery > 2.0;
    }
}
