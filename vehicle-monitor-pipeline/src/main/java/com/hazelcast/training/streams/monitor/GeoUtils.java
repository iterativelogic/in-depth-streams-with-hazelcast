package com.hazelcast.training.streams.monitor;

public class GeoUtils {

    /**
     * Returns the approximate distance in meters between 2 points
     */
    public static double distance(double earliestLat, double earliestLon, double latestLat, double latestLon){
        double R = 6371000;

        double phi1 = Math.toRadians(earliestLat);
        double phi2 = Math.toRadians(latestLat);
        double deltaPhi = phi2- phi1;
        double deltaLambda = Math.toRadians(latestLon) - Math.toRadians(earliestLon);

        double a = Math.pow(Math.sin(deltaPhi / 2.0), 2.0) + Math.cos(phi1) * Math.cos(phi2) * Math.pow(Math.sin(deltaLambda / 2.0), 2.0);
        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
        return R*c;
    }
}
