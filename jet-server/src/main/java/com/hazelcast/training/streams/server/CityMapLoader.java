package com.hazelcast.training.streams.server;

import com.hazelcast.core.MapLoader;
import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.Logger;
import com.hazelcast.training.streams.model.City;

import java.io.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class CityMapLoader implements MapLoader<String, City> {

    private static ILogger log = Logger.getLogger(CityMapLoader.class);
    private static final String CITY_DATA_FILE_ENV_VAR = "CITY_DATA_FILE";

    private Map<String, City> allCities;

    public CityMapLoader(){
        allCities = new HashMap<>(100);
        String filename = System.getenv(CITY_DATA_FILE_ENV_VAR);
        if (filename == null) throw new RuntimeException("No value was provided for the required environment variable: " + CITY_DATA_FILE_ENV_VAR);

        File f = new File(filename);
        if (!f.isFile()) throw new RuntimeException(filename + " does not exist or is not a file");

        if (!f.canRead()) throw new RuntimeException("Cannot read file: " + filename);

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
            int n=0;
            String line = reader.readLine();
            while(line != null){
                ++n;
                if (line.length() > 0) {
                    String []words = line.trim().split(",");
                    if (words.length < 3){
                        log.warning("line " + n + " of " + filename + " cannot be parsed. Skipping");
                        continue;
                    }
                    String city = words[0];
                    float latitude = parseDMS(words[1]);
                    float longitude = parseDMS(words[2]);
                    allCities.put(city, new City(city, latitude, longitude));
                }
                line = reader.readLine();
            }
        } catch(IOException x){
            throw new RuntimeException("Error while reading file: " + filename, x);
        } finally {
            try {
                if (reader != null) reader.close();
            } catch(IOException x){
                log.warning("Error while closing file: " + filename);
            }

        }
    }

    private float parseDMS(String dms){
        String []words = dms.split("\\s");
        float result = Float.parseFloat(words[0]);

        if (result >= 0.0){
            result += Float.parseFloat(words[1])/60.0;
            result += Float.parseFloat(words[2])/3600.0;
        } else {
            result -= Float.parseFloat(words[1])/60.0;
            result -= Float.parseFloat(words[2])/3600.0;
        }
        return result;
    }

    @Override
    public City load(String city) {
        return allCities.get(city);
    }

    @Override
    public Map<String, City> loadAll(Collection<String> collection) {
        HashMap<String, City> result = new HashMap<>(collection.size());
        for(String city: collection){
            result.put(city, allCities.get(city));
        }

        return result;
    }

    @Override
    public Iterable<String> loadAllKeys() {
        return allCities.keySet();
    }
}
