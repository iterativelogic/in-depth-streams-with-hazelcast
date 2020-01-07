package com.hazelcast.training.streams.monitor;

import com.hazelcast.map.EntryBackupProcessor;
import com.hazelcast.map.EntryProcessor;
import com.hazelcast.training.streams.model.Area;

import java.io.Serializable;
import java.util.Map;

public abstract class VehicleAreaEntryProcessor implements EntryProcessor<String, Area>, EntryBackupProcessor<String, Area>, Serializable {
    protected String vin;

    public VehicleAreaEntryProcessor(String vin) {
        this.vin = vin;
    }

    @Override
    public void processBackup(Map.Entry<String, Area> entry) {
        Area area = entry.getValue();
        if (processVin(area, false)) entry.setValue(area);
    }

    @Override
    public Object process(Map.Entry<String, Area> entry) {
        Area area = entry.getValue();
        if (processVin(area, true)) entry.setValue(area);

        return null;
    }

    // returns true if the vin was actually there
    protected abstract boolean processVin(Area area, boolean loggingEnabled);

    @Override
    public EntryBackupProcessor<String, Area> getBackupProcessor() {
        return this;
    }
}
