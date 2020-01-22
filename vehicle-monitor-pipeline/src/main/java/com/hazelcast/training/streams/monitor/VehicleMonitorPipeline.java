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

        StreamStage<EventJournalMapEvent<String, HazelcastJsonValue>> oldNew = pipeline.drawFrom(Sources.<EventJournalMapEvent<String, HazelcastJsonValue>, String, HazelcastJsonValue>mapJournal(
                "vehicles",
                item -> item.getType() == EntryEventType.ADDED || item.getType() == EntryEventType.UPDATED,
                item -> item, JournalInitialPosition.START_FROM_CURRENT))
                .withTimestamps(journalEvent -> extractTimestampFromPingEntry(journalEvent.getNewValue().toString()), MAXIMUM_LATENESS_MS).setName("Old and new value from puts to vehicles");

        StreamStage<Tuple2<Ping, Ping>> oldNewPing = oldNew.map(event -> Tuple2.tuple2(event.getOldValue() == null ? null : gson.fromJson(event.getOldValue().toString(), Ping.class), gson.fromJson(event.getNewValue().toString(), Ping.class))).setName("Convert to Old/New Pings");

        StreamStage<Ping> pings = oldNewPing.map(Tuple2::f1).setName("Select new Ping");

        StreamStage<Ping> crashes = pings.<ArrayList<String>>filterStateful(ArrayList::new, (state, ping) -> isCrashed(state, ping)).setName("filter crashes");

        // tuple (vin, city name)
        StreamStage<Tuple2<String, String>> closestCity = crashes.mapUsingContext(ContextFactory.withCreateFn(jet -> jet.getHazelcastInstance().<String, City>getMap("cities")), (map, ping) -> Tuple2.tuple2(ping.getVin(), closestCity(map, ping))).setName("find closest city");

        closestCity.drainTo(Sinks.mapWithUpdating("vehicles", (Tuple2<String, String> item) -> item.f0(),  (HazelcastJsonValue oldVal, Tuple2<String,String> newVal) -> update(oldVal, newVal) )).setName("Update vehicles map");

        closestCity.drainTo(Sinks.logger(entry -> String.format("CRASH DETECTED: %s HELP DISPATCHED FROM: %s", entry.f0(), entry.f1()))).setName("Dispatch Help");


        AggregateOperation1<Ping, VelocityAccumulator, Double> velocityAggregator =
                AggregateOperation.withCreate(VelocityAccumulator::new)
                        .<Ping>andAccumulate((va, p) -> va.accumulate(p))
                        .andCombine((l, r) -> l.combine(r))
                        .andExportFinish(acc -> acc.getResult());



        StageWithKeyAndWindow<Ping, String> pingWindows = pings.groupingKey(Ping::getVin).window(WindowDefinition.sliding(240 * 1000, 15 * 1000));
        StreamStage<KeyedWindowResult<String, Double>> velocities = pingWindows.aggregate(velocityAggregator).setName("Calculate velocity");


        velocities.drainTo(Sinks.mapWithEntryProcessor(
                "vehicles",
                item -> item.getKey(),
                item -> new UpdateSpeedEntryProcessor(item.getValue()))).setName("Update status");

        // returns tuple (VIN, Make)
        BatchStage<Tuple2<String, String>> vehicleMakes = pipeline.drawFrom(Sources.jdbc(jdbcURL, "SELECT vin, make FROM vehicles",
                rs -> Tuple2.tuple2(rs.getString(1).trim(), rs.getString(2).trim()))).setName("Vehicle Makes");

        StreamStage<Tuple2<Ping, String>> pingsWithMake = pings.hashJoin(vehicleMakes, JoinClause.<String, Ping, Tuple2<String, String>>onKeys(Ping::getVin, Tuple2::f0).projecting(Tuple2::f1), (ping, make) -> Tuple2.tuple2(ping, make));

        // returns tuple2(ping, make) but all the makes are Mercedes - would be nice to allow map method here
        StreamStage<Ping> luxuryPings = pingsWithMake.filter(t2 -> t2.f1() != null && t2.f1().equals("MERCEDES-BENZ")).map(Tuple2::f0).setName("Select Luxury Vehicles");

        // returns (vin, distance)
        StreamStage<Tuple2<String, Double>> luxuryDistances = luxuryPings.groupingKey(Ping::getVin).mapStateful(DistanceAccumulator::new, (accumulator, vin, ping) -> Tuple2.tuple2(vin, accumulator.accumulate(ping))).setName("Calculate distance");

        luxuryDistances.groupingKey(Tuple2::f0).filterStateful(0, () -> new EdgeFilter(MILEAGE_LIMIT_METERS), ( filter,  item) -> filter.apply(item.f1()))
                .drainTo(Sinks.logger( item -> String.format("VEHICLE %s HAS EXCEEDED THE MILEAGE LIMIT, NOW AT %d KM",item.f0(), item.f1().intValue() / 1000)));


        // returns 3-tuple (vin, old area, new area)
        StreamStage<Tuple3<String, Area, Area>> oldNewAreas = oldNewPing.mapUsingContext(
                ContextFactory.withCreateFn(jet -> jet.getHazelcastInstance().<String, Area>getMap("areas")),
                (areas, oldNewPings) -> mapOldNewPingsToOldNewAreas(areas, oldNewPings)).setName("Convert Old/New Pings to Areas");

        StreamStage<Tuple3<String, Area, Area>> areaChanges = oldNewAreas.filter(VehicleMonitorPipeline::filterChanges);

        // since they are separate map entries we can't handle the remove and the add at the same time
        areaChanges.filter(item -> item.f1() != null).drainTo(Sinks.mapWithEntryProcessor("areas",
                item -> item.f1().getName(),
                item -> new VehicleLeftAreaEntryProcessor(item.f0()))).setName("Remove vehicle from area");

        areaChanges.filter(item -> item.f2() != null).drainTo(Sinks.mapWithEntryProcessor("areas",
                item -> item.f2().getName(),
                item -> new VehicleEnteredAreaEntryProcessor(item.f0()))).setName("Add vehicle to area");

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
        String vin = ping.getVin();
        String []obd_codes = ping.getOBDCodes();

        if (obd_codes != null){
            int size = obd_codes.length;
            for(int i=0; i< size; ++i){
                String code = obd_codes[i];
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
    private static String closestCity(IMap<String, City> cityMap, Ping ping){
        return cityMap.aggregate(new ClosestCityAggregator(ping.getLatitude(), ping.getLongitude()));
    }

    private static Tuple3<String, Area, Area> mapOldNewPingsToOldNewAreas(IMap<String, Area> areas, Tuple2<Ping, Ping> oldNewPings){
        Area oldArea = null;
        Area newArea = null;
        Ping oldPing = oldNewPings.f0();
        Ping newPing = oldNewPings.f1();

        Collection<Area> allAreas = areas.values();
        for (Area area : allAreas) {
            if (oldPing != null && area.contains(oldPing)) oldArea = area;
            if (area.contains(newPing)) newArea = area;

            if ( newArea != null && (oldPing == null || oldArea != null)) break;
        }

        return Tuple3.tuple3(newPing.getVin(), oldArea, newArea);
    }

    private static boolean filterChanges(Tuple3<String, Area, Area> item){
        Area oldArea = item.f1();
        Area newArea = item.f2();

        if (oldArea == null && newArea == null) return false;

        return oldArea == null || newArea == null || oldArea.getName().equals(newArea.getName());
    }
}
