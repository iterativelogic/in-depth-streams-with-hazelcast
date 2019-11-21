package com.hazelcast.training.streams.server;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

public class Server
{
    public static void main( String[] args ) {
        HazelcastInstance hz = Hazelcast.newHazelcastInstance();
    }
}