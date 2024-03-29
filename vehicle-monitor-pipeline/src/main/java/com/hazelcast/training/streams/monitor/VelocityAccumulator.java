package com.hazelcast.training.streams.monitor;

import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.Logger;
import com.hazelcast.training.streams.model.Ping;

import java.io.Serializable;

//TODO in Lab 7

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
    }

    public void combine(VelocityAccumulator va){
    }

    // returns the velocity in meters per second
    public Double getResult(){
        double distance = GeoUtils.distance(earliestLat, earliestLon, latestLat, latestLon);

        double time = latestTime - earliestTime;
        log.fine(String.format("%s traveling at %f m/s as of %.0f", vin, distance / time, latestTime));
        return distance / time;
    }

}
