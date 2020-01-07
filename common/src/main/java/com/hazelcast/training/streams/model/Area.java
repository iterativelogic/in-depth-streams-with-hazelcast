package com.hazelcast.training.streams.model;

import java.io.Serializable;
import java.util.ArrayList;

public class Area implements Serializable {
    private String name;
    private float minLatitude;
    private float maxLatitude;
    private float minLongitude;
    private float maxLongitude;
    private ArrayList<String> vehicles;

    public Area(){
        vehicles = new ArrayList<String>();
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float getMinLatitude() {
        return minLatitude;
    }

    public void setMinLatitude(float minLatitude) {
        this.minLatitude = minLatitude;
    }

    public float getMaxLatitude() {
        return maxLatitude;
    }

    public void setMaxLatitude(float maxLatitude) {
        this.maxLatitude = maxLatitude;
    }

    public float getMinLongitude() {
        return minLongitude;
    }

    public void setMinLongitude(float minLongitude) {
        this.minLongitude = minLongitude;
    }

    public float getMaxLongitude() {
        return maxLongitude;
    }

    public void setMaxLongitude(float maxLongitude) {
        this.maxLongitude = maxLongitude;
    }

    public ArrayList<String> getVehicles() {
        return vehicles;
    }

    public boolean contains(Ping p){
        float latitude = p.getLatitude();
        float longitude = p.getLongitude();
        if (latitude > minLatitude && latitude < maxLatitude && longitude > minLongitude && longitude < maxLongitude)
            return true;
        else
            return false;
    }

    @Override
    public String toString() {
        return "Area{" +
                "name='" + name + '\'' +
                ", minLatitude=" + minLatitude +
                ", maxLatitude=" + maxLatitude +
                ", minLongitude=" + minLongitude +
                ", maxLongitude=" + maxLongitude +
                ", vehicles=" + vehicles +
                '}';
    }
}
