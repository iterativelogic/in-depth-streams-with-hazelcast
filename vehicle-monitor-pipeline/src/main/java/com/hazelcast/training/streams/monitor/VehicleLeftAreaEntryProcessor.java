package com.hazelcast.training.streams.monitor;

import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.Logger;
import com.hazelcast.training.streams.model.Area;

public class VehicleLeftAreaEntryProcessor extends VehicleAreaEntryProcessor {

    private static ILogger log = Logger.getLogger(VehicleLeftAreaEntryProcessor.class);

    public VehicleLeftAreaEntryProcessor(String vin){
        super(vin);
    }

    // returns true if the vin was actually there
    @Override
    protected boolean processVin(Area area, boolean loggingEnabled){
        boolean result = area.getVehicles().remove(vin);
        if (result && loggingEnabled)
            log.info(String.format("VEHICLE %s HAS LEFT THE %s AREA", vin, area.getName()));

        return result;
    }

}
