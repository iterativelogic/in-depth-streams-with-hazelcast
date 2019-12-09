package com.hazelcast.training.streams.ingest;

import java.io.Serializable;
import java.util.Arrays;

/*
 * Ping is used as a convenience within the ingest stream (only)
 */
public class Ping implements Serializable {
    private String vin;
    private float latitude;
    private float longitude;
    private float time;
    private int sequence;
    private String []obd_codes;

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

    public String[] getOBDCodes() {
        return obd_codes;
    }

    @Override
    public String toString() {
        return "Ping{" +
                "vin='" + vin + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", time=" + time +
                ", sequence=" + sequence +
                ", obd_codes=" + Arrays.toString(obd_codes) +
                '}';
    }
}
