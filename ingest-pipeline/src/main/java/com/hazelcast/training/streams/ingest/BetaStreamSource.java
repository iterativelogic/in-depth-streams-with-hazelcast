package com.hazelcast.training.streams.ingest;

import com.google.gson.Gson;
import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.jet.datamodel.Tuple2;
import com.hazelcast.jet.pipeline.SourceBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class BetaStreamSource {

    private URL url;
    private Gson gson;

    // current state
    private float highestTimestamp;
    private char [] buffer = new char[10000];

    public static BetaStreamSource create(String url){
        return new BetaStreamSource(url);
    }

    public BetaStreamSource(String url){
        try {
            this.url = new URL(url);
        } catch(MalformedURLException x){
            throw new RuntimeException(url + " is not a valid URL");
        }

        gson = new Gson();
    }

    public void fillBuffer(SourceBuilder.TimestampedSourceBuffer<Ping> buffer){
        Ping[] pings = poll(highestTimestamp, 100);

        for(Ping ping: pings){
            if (ping.getTime() > highestTimestamp) highestTimestamp = ping.getTime();
            buffer.add(ping, (long) (ping.getTime() * 1000.0) );
        }

    }

    private Ping []poll(float since, int limit){
        // It would be more efficient to keep the connection open but this is more robust since the connection
        // will be rebuilt each time.
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("since",Float.toString(highestTimestamp));
            connection.setRequestProperty("limit", Integer.toString(limit));

            if (connection.getResponseCode() != 200)
                throw new RuntimeException("Received a " + connection.getResponseCode() + " response code while polling " + url);

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            return gson.fromJson(reader, Ping[].class);
        } catch(IOException x){
            throw new RuntimeException("An error occurred while accessing the web service at: " + url);
        } finally {
            if (connection != null) connection.disconnect();
        }
    }
}
