package com.hazelcast.training.streams.monitor;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.hazelcast.core.EntryEventType;
import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.core.IMap;
import com.hazelcast.jet.Jet;
import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.aggregate.AggregateOperation;
import com.hazelcast.jet.aggregate.AggregateOperation1;
import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.config.ProcessingGuarantee;
import com.hazelcast.jet.datamodel.KeyedWindowResult;
import com.hazelcast.jet.datamodel.Tuple2;
import com.hazelcast.jet.datamodel.Tuple3;
import com.hazelcast.jet.pipeline.*;
import com.hazelcast.jet.server.JetBootstrap;
import com.hazelcast.map.journal.EventJournalMapEvent;
import com.hazelcast.training.streams.model.Area;
import com.hazelcast.training.streams.model.City;
import com.hazelcast.training.streams.model.Ping;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

public class VehicleMonitorPipeline implements Serializable {

    private static long MAXIMUM_LATENESS_MS = 20000;
    private static long MILEAGE_LIMIT_METERS = 20 * 1000;

    public static void main(String[] args) {
        JetInstance jet;
        String JET_MODE = System.getenv("JET_MODE");
        if (JET_MODE != null && JET_MODE.equals("LOCAL")) {
            jet = Jet.newJetInstance();
        } else {
            jet = JetBootstrap.getInstance();
        }

        if (args.length < 1)
            throw new RuntimeException("This job requires 1 argument: jdbc connection url");

        String jdbcConnection = args[0];

        JobConfig config = new JobConfig();
        config.setProcessingGuarantee(ProcessingGuarantee.AT_LEAST_ONCE);
        config.setSnapshotIntervalMillis(10 * 1000);

        Pipeline pipeline = buildPipeline(jdbcConnection);
        jet.newJob(pipeline, config);
    }

    private static Pipeline buildPipeline(String jdbcURL) {
        Pipeline pipeline = Pipeline.create();

        // TODO in Lab 5 - Create a pipeline to monitor for crashes

        // TODO in Lab 7 - Extend the pipeline to group by VIN and using a sliding window and a custom aggregator, calculate the velocity of each vehicle

        // TODO in Lab 8 - Extend the pipeline to update the list of vehicles in each area

        // TODO in Lab 10 - Extend the pipeline to track mileage for luxury vehicles and log an alert when the mileage limit is exceeded
        return pipeline;
    }

    /**************** Utility Methods *******************/

    private static long extractTimestampFromPingEntry(String pingAsJson){
        JsonElement pingElement = JsonParser.parseString(pingAsJson);
        float timestamp = pingElement.getAsJsonObject().get("time").getAsFloat();
        return (long) timestamp * 1000;
    }


    // Identifies Pings of crashed vehicles.  If the vehicle has already been identified as a crashed vehicle,
    // return false. For new crashes, return true and add them to the list of known crashes
    private static boolean isCrashed(ArrayList<String> knownCrashes, Ping ping){
        boolean result = false;
        //TODO in Lab 5

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
    private static String closestCity(IMap<String, City> cityMap, Ping ping){
        return cityMap.aggregate(new ClosestCityAggregator(ping.getLatitude(), ping.getLongitude()));
    }

}
