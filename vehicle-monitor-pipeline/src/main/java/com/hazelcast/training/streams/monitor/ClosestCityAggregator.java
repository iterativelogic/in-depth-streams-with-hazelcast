package com.hazelcast.training.streams.monitor;

import com.hazelcast.aggregation.Aggregator;
import com.hazelcast.training.streams.model.City;

import java.io.Serializable;
import java.util.Map;

public class ClosestCityAggregator extends Aggregator<Map.Entry<String, City>, String> implements Serializable {

    public ClosestCityAggregator(double lat, double lon){
        vehicleLat = lat;
        vehicleLon = lon;
    }

    double vehicleLat;
    double vehicleLon;

    double closestCityDistance;
    String closestCity;

    @Override
    public void accumulate(Map.Entry<String, City> entry) {
        double d = distance(vehicleLat, vehicleLon, entry.getValue().getLatitude(), entry.getValue().getLongitude());
        if (closestCityDistance == 0.0 || d < closestCityDistance){
            closestCityDistance = d;
            closestCity = entry.getKey();
        }
    }

    @Override
    public void combine(Aggregator aggregator) {
        ClosestCityAggregator otherAggregator = this.getClass().cast(aggregator);
        if (otherAggregator.closestCityDistance < closestCityDistance){
            closestCity = otherAggregator.closestCity;
            closestCityDistance = otherAggregator.closestCityDistance;
        }
    }

    @Override
    public String aggregate() {
        return closestCity;
    }

    public static double distance(double lat1, double lon1, double lat2, double lon2){
        double R = 6371000;

        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double deltaPhi = phi2- phi1;
        double deltaLambda = Math.toRadians(lon2) - Math.toRadians(lon1);

        double a = Math.pow(Math.sin(deltaPhi / 2.0), 2.0) + Math.cos(phi1) * Math.cos(phi2) * Math.pow(Math.sin(deltaLambda / 2.0), 2.0);
        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
        double distance = R*c;

        return Math.abs(distance);
    }

}
