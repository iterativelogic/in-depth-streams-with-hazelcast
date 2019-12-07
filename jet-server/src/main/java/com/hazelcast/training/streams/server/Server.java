package com.hazelcast.training.streams.server;

import com.hazelcast.jet.Jet;
import com.hazelcast.jet.JetInstance;
import io.prometheus.client.exporter.HTTPServer;

import java.io.IOException;

public class Server
{
    public static void main( String[] args ) {

        // this allows application specific stats to be exposed via Prometheus
        try {
            HTTPServer server = new HTTPServer(8001);
        } catch(IOException x){
            throw new RuntimeException("An error occurred while starting the Prometheus HTTP exporter.", x);
        }

        JetInstance jet = Jet.newJetInstance();
    }
}
