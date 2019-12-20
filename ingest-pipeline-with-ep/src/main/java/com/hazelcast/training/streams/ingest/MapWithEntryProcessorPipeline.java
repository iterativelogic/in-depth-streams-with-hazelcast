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

public class MapWithEntryProcessorPipeline implements Serializable {

    private static long MAXIMUM_LATENESS_MS = 20000;

    public static void main(String[] args) {

        JetInstance jet = JetBootstrap.getInstance();

        // we assume that one argument is provided - the URL of the web service
        if (args.length < 1)
            throw new RuntimeException("The URL of the  Beta GPS data source is a required argument");

        JobConfig config = new JobConfig();
        config.setProcessingGuarantee(ProcessingGuarantee.AT_LEAST_ONCE);
        config.setSnapshotIntervalMillis(10 * 1000);

        String url = args[0];
        Pipeline pipeline = buildPipeline(url);
        jet.newJob(pipeline, config).join();
    }

    public static Pipeline buildPipeline(String betaURL) {
        Pipeline pipeline = Pipeline.create();

        StreamSource<Ping> dataSourceBeta = SourceBuilder.timestampedStream("Beta Web Service", ctx -> BetaStreamSource.create(betaURL))
                .<Ping>fillBufferFn((streamSource, buffer) -> streamSource.fillBuffer(buffer))
                .createSnapshotFn(source -> source.snapshot())
                .restoreSnapshotFn((source, snapshots) -> source.restore(snapshots.get(0)))
                .build();


        StreamStage<Ping> sourceBeta = pipeline.drawFrom(dataSourceBeta).withNativeTimestamps(MAXIMUM_LATENESS_MS).setName("Source Beta");


        sourceBeta.drainTo(Sinks.mapWithEntryProcessor("vehicles", ping -> ping.getVin(), ping -> new PingUpdateEntryProcessor(ping))).setName("Update vehicles map");

        return pipeline;
    }

}
