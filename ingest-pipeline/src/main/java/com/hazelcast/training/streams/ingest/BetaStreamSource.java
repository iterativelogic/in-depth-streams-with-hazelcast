package com.hazelcast.training.streams.ingest;

import com.google.gson.Gson;
import com.hazelcast.jet.pipeline.SourceBuilder;
import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.Logger;
import com.hazelcast.training.streams.model.Ping;

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
    private long lastPoll = 0;
    private int highestSequence;
    private char [] buffer = new char[10000];

    public static BetaStreamSource create(String url){
        return new BetaStreamSource(url);
    }

    public BetaStreamSource(String url){
        this.url = url;
        gson = new Gson();
    }

    public void fillBuffer(SourceBuilder.TimestampedSourceBuffer<Ping> buffer){
        // TODO implement the fillBuffer function.  Poll the web service using the code below
        //      and call buffer.add once for each item returned.  Be sure to keep track of the
        //      highest sequence seen so far.
        //
        //      Also, this is a time stamped source so when you
        //      call buffer.add, you will pass a Ping object and a timestamp.  Time stamps should be
        //      a long value indicating the number of milliseconds since some point in time.  The
        //      time stamps in the Ping object are floats representing seconds since some point in
        //      time so you will need to do some conversion.
    }

    public int snapshot(){
        // TODO - implement this to save the current position of the stream
        return 0;
    }

    public void restore(int seq){
        // TODO - implement this to restore the current position of the stream
    }

    private Ping []poll(float since, int limit){
        long now = System.currentTimeMillis();

        // Jet will poll the source many times per second.  To avoid beating up the web service, don't call
        // it unless 2 seconds have elapsed.
        if (now - lastPoll < 2000) return new Ping[0];

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

            return gson.fromJson(reader, Ping[].class);
        } catch(IOException x){
            throw new RuntimeException("An error occurred while accessing the web service at: " + url);
        } finally {
            if (connection != null) connection.disconnect();
        }
    }
}
