package com.hazelcast.training.streams.ingest;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.core.IMap;
import com.hazelcast.training.streams.model.Ping;

public class EPTest {
    public static void main(String []args){
        HazelcastInstance hz = HazelcastClient.newHazelcastClient();

        IMap<String, HazelcastJsonValue> vehicles = hz.<String, HazelcastJsonValue>getMap("vehicles");

        Ping ping = new Ping();
        ping.setVin("TEST0000000000000");
        ping.setLatitude(88.8f);
        ping.setLongitude(-77.7f);
        int seq = 0;
        while (true) {
            ping.setSequence(seq++);
            ping.setTime((float) System.currentTimeMillis()/ 1000);
            PingUpdateEntryProcessor ep = new PingUpdateEntryProcessor(ping);
            vehicles.executeOnKey(ping.getVin(), ep);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException x){
                // ok
            }
        }
    }
}
