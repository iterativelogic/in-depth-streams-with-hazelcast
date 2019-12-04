package com.hazelcast.training.streams.ingest;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.Logger;
import com.hazelcast.map.listener.EntryAddedListener;
import com.hazelcast.map.listener.EntryUpdatedListener;

public class DebugMapListener implements EntryAddedListener<String, HazelcastJsonValue>, EntryUpdatedListener<String, HazelcastJsonValue> {

    private static ILogger logger = Logger.getLogger(DebugMapListener.class);


    @Override
    public void entryAdded(EntryEvent<String, HazelcastJsonValue> entryEvent) {
        logger.info("PUT " + entryEvent.getKey() + " : " + entryEvent.getValue().toString());
    }

    @Override
    public void entryUpdated(EntryEvent<String, HazelcastJsonValue> entryEvent) {
        logger.info("PUT " + entryEvent.getKey() + " : " + entryEvent.getValue().toString());
    }
}
