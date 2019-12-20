package com.hazelcast.training.streams.ingest;

import com.google.gson.Gson;
import com.hazelcast.training.streams.model.Ping;

public class Test {
    public static void main(String []args){
        Gson gson = new Gson();

        String test = "[{ \"vin\": \"1FTZR45E12TA86347\", \"latitude\": 42.303641257536555, \"longitude\": -83.24329888027565, \"time\": 1576850597.9718485, \"sequence\": 595, \"obd_codes\": [] }, { \"vin\": \"2CNDL63F766064463\", \"latitude\": 40.0471388888889, \"longitude\": -80.92820370370369, \"time\": 1576850600.7830048, \"sequence\": 596, \"obd_codes\": [] }, { \"vin\": \"1C6RD7NT8CS140823\", \"latitude\": 33.66938095238097, \"longitude\": -84.57513095238085, \"time\": 1576850600.048076, \"sequence\": 597, \"obd_codes\": [ \"U0001\" ] }, { \"vin\": \"2T2BK1BA2DC182435\", \"latitude\": 41.7723290598291, \"longitude\": -86.30016025641038, \"time\": 1576850600.0378027, \"sequence\": 598, \"obd_codes\": [] }, { \"vin\": \"4T1BE46K77U056879\", \"latitude\": 41.543010018214936, \"longitude\": -87.30375227686703, \"time\": 1576850601.6122417, \"sequence\": 599, \"obd_codes\": [ \"B0001\" ] }, { \"vin\": \"1FAFP363X3W226269\", \"latitude\": 38.46162393162387, \"longitude\": -90.24224358974348, \"time\": 1576850607.0085046, \"sequence\": 600, \"obd_codes\": [] } ]";

        Ping[] pings = gson.fromJson(test, Ping[].class);

        for (Ping ping : pings) {
            System.out.println(ping);
            System.out.println(gson.toJson(ping));
        }

    }
}
