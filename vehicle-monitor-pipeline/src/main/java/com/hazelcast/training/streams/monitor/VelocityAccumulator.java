package com.hazelcast.training.streams.monitor;

import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.Logger;
import com.hazelcast.training.streams.model.Ping;

import java.io.Serializable;

public class VelocityAccumulator  implements Serializable {
    private static ILogger log = Logger.getLogger(VelocityAccumulator.class);

    private String vin;
    private double earliestLat;
    private double earliestLon;
    private double latestLat;
    private double latestLon;
    private double earliestTime;
    private double latestTime;
    private int count;

    public void accumulate(Ping ping){
        vin = ping.getVin();

        ++count;
        if (earliestTime == 0 || ping.getTime() < earliestTime){
            earliestTime = ping.getTime();
            earliestLat = ping.getLatitude();
            earliestLon = ping.getLongitude();
        }

        if (latestTime == 0 || ping.getTime() > latestTime){
            latestTime = ping.getTime();
            latestLat = ping.getLatitude();
            latestLon = ping.getLongitude();
        }
    }

    public void combine(VelocityAccumulator va){
        count += va.count;

        // do nothing if both are uninitialized
        if (earliestTime != 0.0 || va.earliestTime != 0.0 ){
            if (earliestTime == 0.0 ){
                // if this one is uninitialized take the other one
                copyEarliestFrom(va);
            } else if (va.earliestTime != 0.0){
                // if va is initialized, compare and take the earliest, otherwise keep this one
                if (va.earliestTime < earliestTime) copyEarliestFrom(va);
            }
        }

        // do nothing if both are uninitialized
        if (latestTime != 0.0 || va.latestTime != 0.0){
            if (latestTime == 0.0 ){
                // if this one is uninitialized take the other one
                copyLatestFrom(va);
            } else if (va.latestTime != 0.0){
                // if va is initialized, compare and take the latest, otherwise keep this one
                if (va.latestTime > latestTime) copyLatestFrom(va);
            }
        }
    }

    // returns the velocity in meters per second
    public Double getResult(){
        double R = 6371000;

        double phi1 = Math.toRadians(earliestLat);
        double phi2 = Math.toRadians(latestLat);
        double deltaPhi = phi2- phi1;
        double deltaLambda = Math.toRadians(latestLon) - Math.toRadians(earliestLon);

        double a = Math.pow(Math.sin(deltaPhi / 2.0), 2.0) + Math.cos(phi1) * Math.cos(phi2) * Math.pow(Math.sin(deltaLambda / 2.0), 2.0);
        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
        double distance = R*c;

        double time = latestTime - earliestTime;
        log.fine(String.format("%s traveling at %f m/s as of %.0f", vin, distance / time, latestTime));
        return distance / time;
    }

    private void copyEarliestFrom(VelocityAccumulator va){
        this.earliestTime = va.earliestTime;
        this.earliestLat = va.earliestLat;
        this.earliestLon = va.earliestLon;
    }

    private void copyLatestFrom(VelocityAccumulator va){
        this.latestTime = va.latestTime;
        this.latestLat = va.latestLat;
        this.latestLon = va.latestLon;
    }
}
