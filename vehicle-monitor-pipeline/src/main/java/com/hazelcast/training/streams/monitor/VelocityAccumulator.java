package com.hazelcast.training.streams.monitor;

import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.Logger;
import com.hazelcast.training.streams.model.Ping;

import java.io.Serializable;

/**
 * This class will be used as a Jet custom aggregation. For each Ping in a given window, Jet will call the "accumulate"
 * method.  If the pings of one window are spread out over different nodes, then all of the accumulators for a window
 * will be combined using the "combine" method.  After combining, Jet will call "getResult" to get the velocity
 * in meters per second.  To calculate the average velocity over the window, the accumulator only needs to keep track
 * of the earliest and latest ping (based on the time field of the Ping, not the order of invocation).
 */

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
        double distance = GeoUtils.distance(earliestLat, earliestLon, latestLat, latestLon);

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
