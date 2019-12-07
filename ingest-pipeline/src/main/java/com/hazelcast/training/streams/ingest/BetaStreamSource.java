package com.hazelcast.training.streams.ingest;

import com.google.gson.Gson;
import com.hazelcast.jet.pipeline.SourceBuilder;
import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.Logger;
import io.prometheus.client.Gauge;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class BetaStreamSource {
    private static ILogger logger = Logger.getLogger(BetaStreamSource.class);

    private String url;
    private Gson gson;

    // current state
    private int highestSequence;
    private char [] buffer = new char[10000];
    private long lastPoll = 0;

    public static BetaStreamSource create(String url){
        return new BetaStreamSource(url);
    }

    private static Gauge high_water_mark = Gauge.build().name("beta_high_water_mark").help("Highest sequence number observed by source Beta").register();

    public BetaStreamSource(String url){
        this.url = url;
        gson = new Gson();
    }

    public void fillBuffer(SourceBuilder.TimestampedSourceBuffer<Ping> buffer){
        Ping[] pings = poll(highestSequence, 200);

        for(Ping p: pings){
            buffer.add(p, (long) (p.getTime() * 1000.0));
        }

        if (pings.length > 0) {
            highestSequence = pings[pings.length - 1].getSequence();
            logger.fine("Added " + pings.length + " pings. Highest sequence number is: " + highestSequence);
        }

    }

    private Ping []poll(float since, int limit){
        long now = System.currentTimeMillis();
        if (now - lastPoll < 2000) return new Ping [0];

        lastPoll = now;

        // It would be more efficient to keep the connection open but this is more robust since the connection
        // will be rebuilt each time.
        HttpURLConnection connection = null;
        try {
            URL url = new URL(this.url + "?since=" + highestSequence + "&limit=" + limit);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");

            if (connection.getResponseCode() != 200)
                throw new RuntimeException("Received a " + connection.getResponseCode() + " response code while polling " + url);

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            Ping []result = gson.fromJson(reader, Ping[].class);

            if (result.length > 0)
                high_water_mark.set(result[result.length - 1].getSequence());

            return result;
        } catch(IOException x){
            throw new RuntimeException("An error occurred while accessing the web service at: " + url);
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    public int snapshot(){
        return this.highestSequence;
    }

    public void restore(int seq){
        this.highestSequence = seq;
    }
}
