package com.hazelcast.training.streams.logic;

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
 * Updates the status of an entry.  If the current status is "CRASHED" do not update the status at all, otherwise, if
 * the speed is greater than the given limit and the status is not already the provided value, update it.  Avoid
 * unnecessary updates (ones that would have no effect) by not calling the "setValue" method on the entry.
 * This prevents unnecessary invocation of map listeners.
 */

public class UpdateEntryStatusProcessor implements EntryProcessor<String, HazelcastJsonValue>, EntryBackupProcessor<String, HazelcastJsonValue>, Serializable {

    private static ILogger log = Logger.getLogger(UpdateEntryStatusProcessor.class);

    private String newStatus;

    public UpdateEntryStatusProcessor(String status){
        this.newStatus = status;
    }

    @Override
    public void processBackup(Map.Entry<String, HazelcastJsonValue> entry) {
        process(entry);
    }

    @Override
    public Object process(Map.Entry<String, HazelcastJsonValue> entry) {
        Ping ping = gson.fromJson(entry.getValue().toString(), Ping.class);
        String status = ping.getStatus();
        if (status == null || (! status.equals(newStatus) )){
                ping.setStatus(newStatus);
                entry.setValue(new HazelcastJsonValue(gson.toJson(ping)));
        }

        return null;
    }

    @Override
    public EntryBackupProcessor<String, HazelcastJsonValue> getBackupProcessor() {
        return this;
    }

    private static Gson gson = new Gson();
}
