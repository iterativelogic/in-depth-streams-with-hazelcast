package com.hazelcast.training.streams.ingest;

import com.google.gson.Gson;
import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.map.EntryBackupProcessor;
import com.hazelcast.map.EntryProcessor;
import com.hazelcast.training.streams.model.Ping;

import java.io.Serializable;
import java.util.Map;

public class PingUpdateEntryProcessor implements EntryProcessor<String, HazelcastJsonValue>, EntryBackupProcessor<String, HazelcastJsonValue>, Serializable {

    private Ping incomingPing;

    public PingUpdateEntryProcessor(Ping p){
        this.incomingPing = p;
    }

    @Override
    public void processBackup(Map.Entry<String, HazelcastJsonValue> entry) {

    }

    @Override
    public Object process(Map.Entry<String, HazelcastJsonValue> entry) {
        HazelcastJsonValue current = entry.getValue();
        if (current != null){
            Ping currentPing = gson.fromJson(current.toString(), Ping.class);
            currentPing.setLatitude(incomingPing.getLatitude());
            currentPing.setLongitude(incomingPing.getLongitude());
            currentPing.setSequence(incomingPing.getSequence());
            currentPing.setTime(incomingPing.getTime());
            currentPing.setObd_codes(incomingPing.getOBDCodes());
            entry.setValue(new HazelcastJsonValue(gson.toJson(currentPing)));
        } else {
            entry.setValue(new HazelcastJsonValue(gson.toJson(incomingPing)));
        }

        return null;
    }

    @Override
    public EntryBackupProcessor<String, HazelcastJsonValue> getBackupProcessor() {
        return this;
    }

    private static Gson gson = new Gson();
}
