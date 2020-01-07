package com.hazelcast.training.streams.server;

import com.hazelcast.core.MapLoader;
import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.Logger;
import com.hazelcast.training.streams.model.Area;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class AreaMapLoader implements MapLoader<String, Area> {

    private Map<String, Area> testAreas;

    public AreaMapLoader(){
        testAreas = new HashMap<>(5);

        Area indianapolisArea = new Area();
        indianapolisArea.setName("Indianapolis");
        indianapolisArea.setMinLatitude(39.5f);
        indianapolisArea.setMaxLatitude(40.5f);
        indianapolisArea.setMinLongitude(-87.0f);
        indianapolisArea.setMaxLongitude(-85.0f);
        testAreas.put(indianapolisArea.getName(), indianapolisArea);

        Area memphisArea = new Area();
        memphisArea.setName("Memphis");
        memphisArea.setMinLatitude(34.2f);
        memphisArea.setMaxLatitude(36f);
        memphisArea.setMinLongitude(-91f);
        memphisArea.setMaxLongitude(-89f);
        testAreas.put(memphisArea.getName(), memphisArea);

        Area charlotteArea = new Area();
        charlotteArea.setName("Charlotte");
        charlotteArea.setMinLatitude(35f);
        charlotteArea.setMaxLatitude(36f);
        charlotteArea.setMinLongitude(-82.5f);
        charlotteArea.setMaxLongitude(-80f);
        testAreas.put(charlotteArea.getName(), charlotteArea);
    }


    @Override
    public Area load(String area) {
        return testAreas.get(area);
    }

    @Override
    public Map<String, Area> loadAll(Collection<String> collection) {
        HashMap<String, Area> result = new HashMap<>(collection.size());
        for(String area: collection){
            result.put(area, testAreas.get(area));
        }

        return result;
    }

    @Override
    public Iterable<String> loadAllKeys() {
        return testAreas.keySet();
    }
}
