package com.hazelcast.training.streams.ingest;

import com.google.gson.Gson;
import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.map.EntryBackupProcessor;
import com.hazelcast.map.EntryProcessor;
import com.hazelcast.training.streams.model.Ping;

import java.io.Serializable;
import java.util.Map;

/**
 * This entry processor  updates the latitude, longitude, time, sequence and obd_codes field of the
 * existing entry (if there is one). The status and note fields are updated by another actor and we
 * do not want to overwrite them.
 */
public class PingEntryProcessor  implements EntryProcessor<String, HazelcastJsonValue> , EntryBackupProcessor<String, HazelcastJsonValue>, Serializable {

    // one gson configuration for the whole pipeline.
    private static Gson gson = new Gson();

    private Ping incomingPing;

    public PingEntryProcessor(Ping p){
        incomingPing = p;
        gson = new Gson();
    }

    @Override
    public Object process(Map.Entry<String, HazelcastJsonValue> entry) {
        HazelcastJsonValue result = null;
        HazelcastJsonValue v = entry.getValue();
        if (v != null){
            Ping p = gson.fromJson(v.toString(), Ping.class);
            p.setLatitude(incomingPing.getLatitude());
            p.setLongitude(incomingPing.getLongitude());
            p.setTime(incomingPing.getTime());
            p.setSequence(incomingPing.getSequence());
            p.setObd_codes(incomingPing.getOBDCodes());
            result = new HazelcastJsonValue(gson.toJson(p));
            entry.setValue(result);
        } else {
            result = new HazelcastJsonValue(gson.toJson(incomingPing));
        }

        entry.setValue(result);
        return result;
    }

    @Override
    public EntryBackupProcessor<String, HazelcastJsonValue> getBackupProcessor() {
        return this;
    }

    @Override
    public void processBackup(Map.Entry<String, HazelcastJsonValue> entry) {
        process(entry);
    }
}
