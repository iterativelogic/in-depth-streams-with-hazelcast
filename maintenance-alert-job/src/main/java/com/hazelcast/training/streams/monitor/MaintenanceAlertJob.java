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

        JobConfig config = new JobConfig();
        config.setProcessingGuarantee(ProcessingGuarantee.AT_LEAST_ONCE);
        config.setSnapshotIntervalMillis(10 * 1000);

        Pipeline pipeline = buildPipeline(noticeDir, jdbcConnection);
        jet.newJob(pipeline, config).join();
    }

    public static Pipeline buildPipeline(String noticeDir, String jdbcConnection) {
        Pipeline pipeline = Pipeline.create();

        BatchStage<String> maintenanceNoticeLines = pipeline.drawFrom(Sources.files(noticeDir)).setName("Read maintenance notices from file");

        // 2-tuple (vin, year|make|model)
        BatchStage<Tuple2<String, String>> vehicleTypes = pipeline.drawFrom(Sources.jdbc(jdbcConnection, "SELECT vin, year, make, model FROM vehicles",
                rs -> Tuple2.tuple2(rs.getString(1).trim(), makeVehicleType(rs.getInt(2), rs.getString(3), rs.getString(4))))).setName("Load vehicle reference data");

        //3-tuple (notice num, year|make|model, note)
        BatchStage<Tuple3<Integer, String, String>> maintenanceNotices = maintenanceNoticeLines.map(item -> parseMaintenanceNoticeLine(item)).setName("Parse maintenance notice");

        //4-tuple (notice num, vin, year|make|model, note)
        BatchStage<Tuple4<Integer, String, String, String>> maintenanceItems = maintenanceNotices.hashJoin(
                vehicleTypes,
                JoinClause.onKeys(notice -> notice.f1(), vehicleType -> vehicleType.f1()),
                (notice, type) -> Tuple4.tuple4(notice.f0(), type == null ? null : type.f0(), type == null ? null : type.f1(), notice.f2()))
                .setName("Join notices and vehicles");

        BatchStage<Tuple4<Integer, String, String, String>> filteredItems = maintenanceItems.filter(item -> item.f1() != null).setName("Filter out notices with no matching vehicles");

        filteredItems.drainTo(Sinks.logger(item -> String.format("MAINTENANCE NOTICE: %03d FOR %s (%s) - %s", item.f0(), item.f1(), item.f2(), item.f3()))).setName("Log maintenance items");

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
