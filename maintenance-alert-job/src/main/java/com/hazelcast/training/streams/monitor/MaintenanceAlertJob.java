package com.hazelcast.training.streams.monitor;

import com.hazelcast.jet.Jet;
import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.config.ProcessingGuarantee;
import com.hazelcast.jet.datamodel.Tuple2;
import com.hazelcast.jet.datamodel.Tuple3;
import com.hazelcast.jet.datamodel.Tuple4;
import com.hazelcast.jet.pipeline.*;
import com.hazelcast.jet.server.JetBootstrap;
import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.Logger;

import java.io.Serializable;

public class MaintenanceAlertJob implements Serializable {

    private static ILogger log = Logger.getLogger(MaintenanceAlertJob.class);

    /**
     * Main method is expecting 2 arguments: The name of the directory containing the notice file and the
     * JDBC Connection URL for the vehicle DB
     */
    public static void main(String[] args) {
        JetInstance jet;
        String JET_MODE = System.getenv("JET_MODE");
        if (JET_MODE != null && JET_MODE.equals("LOCAL")) {
            jet = Jet.newJetInstance();
        } else {
            jet = JetBootstrap.getInstance();
        }

        if (args.length < 2)
            throw new RuntimeException("This job requires 2 arguments: notice directory, jdbc connection url");

        String noticeDir = args[0];
        String jdbcConnection = args[1];

        //TODO for Lab 6: build and run the maintenance alert pipeline.  Don't forget to wait for the job to finish.
    }

    public static Pipeline buildPipeline(String noticeDir, String jdbcConnection) {
        Pipeline pipeline = Pipeline.create();

        //TODO for Lab 6: build the pipeline

        return pipeline;
    }

    /**************** Utility Methods *******************/

    private static String makeVehicleType(int year, String make, String model){
        return String.format("%d|%s|%s",year, make.trim().toLowerCase(), model.trim().toLowerCase());
    }

    // converts one line of the notices file to a 3-tuple (notice num, year|make|model, note)
    private static Tuple3<Integer,String, String> parseMaintenanceNoticeLine(String line){
        String []words = line.split(",");
        return Tuple3.tuple3(Integer.parseInt(words[0].trim()), makeVehicleType(Integer.parseInt(words[1].trim()),words[2],words[3]), words[4]);
    }

}
