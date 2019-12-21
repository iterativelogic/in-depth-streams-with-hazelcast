package com.hazelcast.training.streams.ingest;

import com.google.gson.Gson;
import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.jet.Jet;
import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.config.ProcessingGuarantee;
import com.hazelcast.jet.pipeline.*;
import com.hazelcast.jet.server.JetBootstrap;
import com.hazelcast.training.streams.model.Ping;

import java.io.Serializable;

public class GPSIngestPipeline implements Serializable {

    private static long MAXIMUM_LATENESS_MS = 20000;

    public static void main(String[] args) {

        JetInstance jet;
        String JET_MODE = System.getenv("JET_MODE");
        if (JET_MODE != null && JET_MODE.equals("LOCAL")) {
            jet = Jet.newJetInstance();
            jet.getHazelcastInstance().getMap("vehicles").addEntryListener(new DebugMapListener(), true);
        } else {
            jet = JetBootstrap.getInstance();
        }

        // we assume that one argument is provided - the URL of the web service
        if (args.length < 2)
            throw new RuntimeException("Directory of Alpha Source and URL of Beta GPS Source are Required Arguments");

        String dir = args[0];
        String url = args[1];

        // TODO in lab 4, add a job config that specifies an AT_LEAST_ONCE processing guarantee and
        //      a 10 second snapshot interval.
        //      Pass the config object as the second parameter to jet.newJob(pipeline, config)

        Pipeline pipeline = buildPipeline(dir, url);
        jet.newJob(pipeline);
    }

    public static Pipeline buildPipeline(String alphaDir, String betaURL) {
        Pipeline pipeline = Pipeline.create();

        // TODO in lab 3 - build a pipeline to ingest both alpha and beta data sources, convert them to
        //        map entries and save them in the "vehicles" map.


        //TODO in lab 5 - update the ingest pipeline to use Sinks.mapWithUpdating
        return pipeline;
    }

    private static Gson gson = new Gson();


    // Utility method for Lab 5 - updates a HazelcastJsonValue with location and time from a Ping object
    private static HazelcastJsonValue update(HazelcastJsonValue oldVal, Ping newVal){
        Ping result;
        if (oldVal == null){
            result = newVal;
        } else {
            Ping oldPing = gson.fromJson(oldVal.toString(), Ping.class);
            oldPing.setLatitude(newVal.getLatitude());
            oldPing.setLongitude(newVal.getLongitude());
            oldPing.setTime(newVal.getTime());
            oldPing.setSequence(newVal.getSequence());
            // oldPing.setObd_codes(newVal.getOBDCodes()); BE SURE TO uncomment for Lab 5!
            result = oldPing;
        }

        return new HazelcastJsonValue(gson.toJson(result));
    }
}
