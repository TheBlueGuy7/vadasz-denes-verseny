package hu.theblueguy7;

import hu.theblueguy7.model.Speed;


public class StrategyManager {
    private PathFinder pf;

    public StrategyManager(PathFinder pf) {
        this.pf = pf;
    }

    // kiszamolja mennyi energy es ido kell
    public RouteEstimate estimateRoute(int pathLength, Speed speed, int startStep) {
        // hany lepes kell
        int halfHoursNeeded = (int) Math.ceil((double) pathLength / speed.velocity);
        double totalEnergy = 0;

        for (int i = 0; i < halfHoursNeeded; i++) {
            boolean isDay = ((startStep + i) % 48) < 32;
            double consumption = 2.0 * Math.pow(speed.velocity, 2); // E = 2 * v^2 [cite: 36]
            if (isDay) consumption -= 10; // nappal tolt a napelem
            totalEnergy += consumption;
        }
        return new RouteEstimate(totalEnergy, halfHoursNeeded);
    }

    public static class RouteEstimate {
        public double energyNeeded;
        public int timeSteps;
        public RouteEstimate(double e, int t) { this.energyNeeded = e; this.timeSteps = t; }
    }
}