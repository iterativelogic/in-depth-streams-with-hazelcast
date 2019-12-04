package com.hazelcast.training.streams.ingest;

import com.google.gson.Gson;
import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.jet.Jet;
import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.datamodel.Tuple2;
import com.hazelcast.jet.pipeline.*;
import com.hazelcast.jet.server.JetBootstrap;

public class GPSIngestPipeline {

    public static void main(String []args){

        JetInstance jet = null;
        String JET_MODE = System.getenv("JET_MODE");
        if (JET_MODE != null && JET_MODE.equals("LOCAL")){
            jet = Jet.newJetInstance();
            jet.getHazelcastInstance().getMap("vehicles").addEntryListener(new DebugMapListener(), true);
        } else {
            jet = JetBootstrap.getInstance();
        }

        // we assume that one argument is provided - the URL of the web service
        if (args.length == 0)
            throw new RuntimeException("URL of Beta GPS Source is Required");

        String url = args[0];
        Pipeline pipeline = buildPipeline(url);
        jet.newJob(pipeline);
    }

    public static Pipeline buildPipeline(String betaURL){
        Pipeline pipeline = Pipeline.create();

        StreamSource<Ping> beta_web_service = SourceBuilder.timestampedStream("Beta Web Service", ctx -> BetaStreamSource.create(betaURL))
                .<Ping>fillBufferFn((streamSource, buffer) -> streamSource.fillBuffer(buffer))
                .build();

        StreamStage<Ping> read_from_web_service = pipeline.drawFrom(beta_web_service).withNativeTimestamps(20000).setName("Read From Web Service");

        StreamStage<Tuple2<String, HazelcastJsonValue>> jsonStreamStage = read_from_web_service.mapUsingContext(ContextFactory.withCreateFn(jet -> new Gson()), (gson, ping) -> Tuple2.tuple2(ping.getVin(), new HazelcastJsonValue(gson.toJson(ping))))
                .setName("To Map Entry");


        jsonStreamStage.drainTo(Sinks.map("vehicles"));

        return pipeline;
    }

}
