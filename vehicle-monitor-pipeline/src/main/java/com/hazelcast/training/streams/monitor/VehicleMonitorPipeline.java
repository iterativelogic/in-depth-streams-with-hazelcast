package com.hazelcast.training.streams.monitor;

import com.google.gson.*;
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
import com.hazelcast.training.streams.model.Ping;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;

public class VehicleMonitorPipeline implements Serializable {

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

        closestCity.drainTo(Sinks.mapWithUpdating("vehicles", (Tuple2<String, String> item) -> item.f0(),  (HazelcastJsonValue oldVal, Tuple2<String,String> newVal) -> update(oldVal, newVal) )).setName("Update vehicles map");

        closestCity.drainTo(Sinks.logger(entry -> String.format("CRASH DETECTED: %s HELP DISPATCHED FROM: %s", entry.f0(), entry.f1()))).setName("Dispatch Help");
        return pipeline;
    }

    /**************** Utility Methods *******************/

    public static long extractTimestampFromPingEntry(String pingAsJson){
        JsonElement pingElement = JsonParser.parseString(pingAsJson);
        float timestamp = pingElement.getAsJsonObject().get("time").getAsFloat();
        return (long) timestamp * 1000;
    }

    // Identifies Pings of crashed vehicles.  If the vehicle has already been identified as a crashed vehicle,
    // return false. For new crashes, return true and add them to the list of known crashes
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

    // given a vin, city tuple, update ONLY the status and note fields of the existing map entry and
    // return it as a HazelcastJsonValue
    private static HazelcastJsonValue update(HazelcastJsonValue oldVal, Tuple2<String,String> newVal){
        Ping result;
        if (oldVal == null){
            // should never happen!
            result = new Ping();
            result.setVin(newVal.f0());
        } else {
            Ping oldPing = gson.fromJson(oldVal.toString(), Ping.class);
            oldPing.setStatus("CRASHED");
            oldPing.setNote(String.format("help dispatched from %s", newVal.f1()));
            result = oldPing;
        }

        return new HazelcastJsonValue(gson.toJson(result));
    }

    private static Gson gson = new Gson();

    // invoke the closest city aggregator
    public static String closestCity(IMap<String, City> cityMap, String jsonPing){
        Ping ping = gson.fromJson(jsonPing, Ping.class);

        return cityMap.aggregate(new ClosestCityAggregator(ping.getLatitude(), ping.getLongitude()));
    }

}
