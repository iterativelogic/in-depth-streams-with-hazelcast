package com.hazelcast.training.streams.monitor;

import com.hazelcast.training.streams.model.Ping;

import java.io.Serializable;

public class DistanceAccumulator implements Serializable {

    private double distance;
    private Ping startPing;

    public double accumulate(Ping ping){
        if (startPing == null) {
            startPing = ping;
            return 0.0;  //RETURN
        }

        // ignore any out of order pings that are from before start ping
        if (ping.getTime() < startPing.getTime()){
            return distance;  // RETURN
        }

        distance += GeoUtils.distance(startPing.getLatitude(), startPing.getLongitude(), ping.getLatitude(), ping.getLongitude());
        startPing = ping;
        return distance;
    }
}
