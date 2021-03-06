package com.hazelcast.training.streams.model;

import java.io.Serializable;
import java.util.Arrays;

/*
 * Ping is used as a convenience within the Jet pipelines
 */
public class Ping implements Serializable {
    private String vin;
    private float latitude;
    private float longitude;
    private float time;
    private int sequence;
    private String status;
    private String note;

    //TODO Lab 4 add a String [] called obd_codes to hold the engine codes


    public String getVin() {
        return vin;
    }

    public void setVin(String vin) {
        this.vin = vin;
    }

    public float getLatitude() {
        return latitude;
    }

    public void setLatitude(float latitude) {
        this.latitude = latitude;
    }

    public float getLongitude() {
        return longitude;
    }

    public void setLongitude(float longitude) {
        this.longitude = longitude;
    }

    public float getTime() {
        return time;
    }

    public void setTime(float time) {
        this.time = time;
    }

    public int getSequence() {
        return sequence;
    }

    public void setSequence(int sequence) {
        this.sequence = sequence;
    }


    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    //TODO Lab 4: Add getOBDCodes, setOBDCodes, Update the toString method to include OBD codes.


    @Override
    public String toString() {
        return "Ping{" +
                "vin='" + vin + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", time=" + time +
                ", sequence=" + sequence +
                ", status='" + status + '\'' +
                ", note='" + note + '\'' +
                '}';
    }
}
