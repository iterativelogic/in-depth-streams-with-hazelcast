package com.hazelcast.training.streams.model;

import java.io.Serializable;

public class VehicleType implements Serializable {
    private int year;
    private String make;
    private String model;

    public VehicleType(int year, String make, String model) {
        this.year = year;
        this.make = make;
        this.model = model;
    }

    public int getYear() {
        return year;
    }

    public String getMake() {
        return make;
    }

    public String getModel() {
        return model;
    }


    @Override
    public String toString() {
        return "VehicleType{" +
                "year=" + year +
                ", make='" + make + '\'' +
                ", model='" + model + '\'' +
                '}';
    }
}
