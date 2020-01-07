package com.hazelcast.training.streams.monitor;

import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.Logger;
import com.hazelcast.training.streams.model.Area;

public class VehicleEnteredAreaEntryProcessor extends VehicleAreaEntryProcessor {

    private static ILogger log = Logger.getLogger(VehicleEnteredAreaEntryProcessor.class);

    public VehicleEnteredAreaEntryProcessor(String vin){
        super(vin);
    }

    // returns true if the vin was actually there
    @Override
    protected boolean processVin(Area area, boolean loggingEnabled){
        if (area.getVehicles().contains(vin)) return false;

        area.getVehicles().add(vin);
        if (loggingEnabled)
            log.info(String.format("VEHICLE %s HAS ENTERED THE %s AREA", vin, area.getName()));

        return true;
    }

}
