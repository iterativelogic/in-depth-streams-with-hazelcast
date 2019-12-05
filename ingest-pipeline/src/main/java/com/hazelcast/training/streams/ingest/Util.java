package com.hazelcast.training.streams.ingest;

public class Util {
    public static long timestampFromSourceAlpha(String line){
        String []words = line.split(",");
        float t = Float.parseFloat(words[3]);
        return (long) (t * 1000.0);
    }

    public static Ping pingFromSourceAlpha(String line){
        Ping result = new Ping();
        String []words = line.split(",");
        result.setVin(words[0]);
        result.setLatitude(Float.parseFloat(words[1]));
        result.setLongitude(Float.parseFloat(words[2]));
        result.setTime(Float.parseFloat(words[3]));
        return result;
    }
}
