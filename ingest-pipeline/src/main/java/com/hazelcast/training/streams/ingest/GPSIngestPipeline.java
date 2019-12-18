package com.hazelcast.training.streams.ingest;

import com.google.gson.Gson;
import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.jet.Jet;
import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.aggregate.AggregateOperations;
import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.config.ProcessingGuarantee;
import com.hazelcast.jet.datamodel.Tuple2;
import com.hazelcast.jet.datamodel.Tuple4;
import com.hazelcast.jet.pipeline.*;
import com.hazelcast.jet.server.JetBootstrap;
import com.hazelcast.training.streams.model.Ping;

public class GPSIngestPipeline {

    private static long MAXIMUM_LATENESS_MS = 20000;

    public static void main(String[] args) {

        JetInstance jet = null;
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

        JobConfig config = new JobConfig();
        config.setProcessingGuarantee(ProcessingGuarantee.AT_LEAST_ONCE);
        config.setSnapshotIntervalMillis(10 * 1000);

        String dir = args[0];
        String url = args[1];
        Pipeline pipeline = buildPipeline(dir, url);
        jet.newJob(pipeline, config).join();
    }

    public static Pipeline buildPipeline(String alphaDir, String betaURL) {
        Pipeline pipeline = Pipeline.create();

        StreamStage<String> sourceAlpha = pipeline.drawFrom(Sources.fileWatcher(alphaDir)).withTimestamps(line -> Util.timestampFromSourceAlpha(line), MAXIMUM_LATENESS_MS).setName("Source Alpha");

        StreamStage<Ping> mapToPing = sourceAlpha.map(line -> Util.pingFromSourceAlpha(line)).setName("Map To Ping");

        StreamSource<Ping> dataSourceBeta = SourceBuilder.timestampedStream("Beta Web Service", ctx -> BetaStreamSource.create(betaURL))
                .<Ping>fillBufferFn((streamSource, buffer) -> streamSource.fillBuffer(buffer))
                .createSnapshotFn(source -> source.snapshot())
                .restoreSnapshotFn((source, snapshots) -> source.restore(snapshots.get(0)))
                .build();


        StreamStage<Ping> sourceBeta = pipeline.drawFrom(dataSourceBeta).withNativeTimestamps(MAXIMUM_LATENESS_MS).setName("Source Beta");

        StreamStage<Ping> mergeAlphaAndBeta = sourceBeta.merge(mapToPing).setName("Merge Alpha and Beta");

        mergeAlphaAndBeta.drainTo(Sinks.mapWithMerging("vehicles", (Ping ping) -> ping.getVin(), (Ping ping) -> new HazelcastJsonValue(new Gson().toJson(ping)), (oldV, newV) -> oldV));

//        StreamStage<Tuple2<String, HazelcastJsonValue>> jsonStreamStage = mergeAlphaAndBeta.mapUsingContext(ContextFactory.withCreateFn(jet -> new Gson()), (gson, ping) -> Tuple2.tuple2(ping.getVin(), new HazelcastJsonValue(gson.toJson(ping))))
//                .setName("To Map Entry");


        return pipeline;
    }
}
