package com.hazelcast.training.streams.monitor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.core.IMap;
import com.hazelcast.jet.Jet;
import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.config.ProcessingGuarantee;
import com.hazelcast.jet.datamodel.Tuple2;
import com.hazelcast.jet.pipeline.*;
import com.hazelcast.jet.server.JetBootstrap;
import com.hazelcast.training.streams.model.City;

import java.util.ArrayList;
import java.util.Map;

public class VehicleMonitorPipeline {

    private static long MAXIMUM_LATENESS_MS = 20000;

    public static void main(String[] args) {
        JetInstance jet;
        String JET_MODE = System.getenv("JET_MODE");
        if (JET_MODE != null && JET_MODE.equals("LOCAL")) {
            jet = Jet.newJetInstance();
        } else {
            jet = JetBootstrap.getInstance();
        }

        JobConfig config = new JobConfig();
        config.setProcessingGuarantee(ProcessingGuarantee.AT_LEAST_ONCE);
        config.setSnapshotIntervalMillis(10 * 1000);

        Pipeline pipeline = buildPipeline();
        jet.newJob(pipeline, config);
    }

    public static Pipeline buildPipeline() {
        Pipeline pipeline = Pipeline.create();

        StreamStage<Map.Entry<String, HazelcastJsonValue>> vehicles = pipeline.drawFrom(Sources.<String, HazelcastJsonValue>mapJournal("vehicles", JournalInitialPosition.START_FROM_CURRENT))
                .withTimestamps(entry -> extractTimestampFromPingEntry(entry.getValue().toString()), MAXIMUM_LATENESS_MS);

        StreamStage<Map.Entry<String, HazelcastJsonValue>> crashes = vehicles.<ArrayList<String>>filterStateful(ArrayList::new, (state, entry) -> isCrashed(state, entry.getValue().toString())).setName("filter crashes");

        StreamStage<Tuple2<String, String>> closestCity = crashes.mapUsingContext(ContextFactory.withCreateFn(jet -> jet.getHazelcastInstance().<String, City>getMap("cities")), (map, entry) -> Tuple2.tuple2(entry.getKey(), closestCity(map, entry.getValue().toString()))).setName("find closest city");

        closestCity.drainTo(Sinks.logger(entry -> String.format("CRASH DETECTED: %s CLOSEST CITY IS: %s", entry.f0(), entry.f1()))).setName("Log Crashes");
        return pipeline;
    }

    public static long extractTimestampFromPingEntry(String pingAsJson){
        JsonElement pingElement = JsonParser.parseString(pingAsJson);
        float timestamp = pingElement.getAsJsonObject().get("time").getAsFloat();
        return (long) timestamp * 1000;
    }

    public static boolean isCrashed(ArrayList<String> knownCrashes, String pingAsJson){
        boolean result = false;
        JsonObject ping = JsonParser.parseString(pingAsJson).getAsJsonObject();
        String vin = ping.get("vin").getAsString();
        JsonArray obd_codes = ping.getAsJsonArray("obd_codes");

        if (obd_codes != null){
            int size = obd_codes.size();
            for(int i=0; i< size; ++i){
                String code = obd_codes.get(i).getAsString();
                if (code.equals("B0001")){
                    if (!knownCrashes.contains(vin)){
                        knownCrashes.add(vin);
                        result = true;
                        break;
                    }
                }
            }
        }
        return result;
    }

    public static String closestCity(IMap<String, City> cityMap, String jsonPing){
        JsonObject ping = JsonParser.parseString(jsonPing).getAsJsonObject();
        double pingLat = ping.get("latitude").getAsFloat();
        double pingLon = ping.get("longitude").getAsFloat();

        // this is only OK because the city list is short
        double closestDistance = 0.0;
        String closestCity = null;
        for(City city: cityMap.values()){
            double d = distance(pingLat, pingLon, city.getLatitude(), city.getLongitude());
            if (closestCity == null || d < closestDistance){
                closestCity = city.getName();
                closestDistance = d;
            }
        }

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
