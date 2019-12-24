package com.hazelcast.training.streams.monitor;

import com.google.gson.Gson;
import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.Logger;
import com.hazelcast.map.EntryBackupProcessor;
import com.hazelcast.map.EntryProcessor;
import com.hazelcast.training.streams.model.Ping;

import java.io.Serializable;
import java.util.Map;

/**
 * Updates the status of an entry based on a computed speed value.  If the current status is "CRASHED" do not update
 * the status at all, otherwise, if the speed is greater than the given limit and the status is not already
 * "SPEEDING" then update it to "SPEEDING".  If the speed is less than or equal to the given limit and it is not
 * already "" or null, update it to "".  Avoid unnecessary updates (ones that would have no effect) by not calling
 * the "setValue" method on the entry.  This prevents unnecessary invocation of map listeners.
 */

public class UpdateSpeedEntryProcessor implements EntryProcessor<String, HazelcastJsonValue>, EntryBackupProcessor<String, HazelcastJsonValue>, Serializable {
    private static double highSpeedLimit = (75.0 * 5280.0 * 12.0 * 2.54 ) / (3600.0 * 100.0);

    private static ILogger log = Logger.getLogger(UpdateSpeedEntryProcessor.class);

    private double speed;

    public UpdateSpeedEntryProcessor(double speed){
        this.speed = speed;
    }

    @Override
    public void processBackup(Map.Entry<String, HazelcastJsonValue> entry) {
        process(entry);
    }

    @Override
    public Object process(Map.Entry<String, HazelcastJsonValue> entry) {
        Ping ping = gson.fromJson(entry.getValue().toString(), Ping.class);
        String status = ping.getStatus();
        if (status == null || status.equals("")){
            if (speed > highSpeedLimit){
                ping.setStatus("SPEEDING");
                entry.setValue(new HazelcastJsonValue(gson.toJson(ping)));
            }
        } else if (status.equals("SPEEDING")){
            if (speed <= highSpeedLimit){
                ping.setStatus("");
                entry.setValue(new HazelcastJsonValue(gson.toJson(ping)));
            }
        } else {
            // CRASHED is the only other choice
        }

        return null;
    }

    @Override
    public EntryBackupProcessor<String, HazelcastJsonValue> getBackupProcessor() {
        return this;
    }

    private static Gson gson = new Gson();
}
