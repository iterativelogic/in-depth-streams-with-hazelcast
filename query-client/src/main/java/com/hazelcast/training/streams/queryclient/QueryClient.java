package com.hazelcast.training.streams.queryclient;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.core.IMap;
import com.hazelcast.query.SqlPredicate;
import com.hazelcast.training.streams.logic.UpdateEntryStatusProcessor;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Set;

public class QueryClient {
    /**
     * This program expects a hazelcast member (e.g. member-hostname:5701).  It will start an interactive console
     * which submits queries as an IMDG client.
     */

    private static HazelcastInstance hz;
    private static IMap<String, HazelcastJsonValue> vehicles;

    public static void main(String []args){
        if (args.length == 0){
            System.out.println("This program requires the address of a Hazelcast cluster member (e.g. som-host:5701).");
            System.exit(1);
        }

        try {
            ClientConfig cfg = new ClientConfig();
            cfg.getNetworkConfig().addAddress(args[0]);
            hz = HazelcastClient.newHazelcastClient(cfg);
            vehicles = hz.getMap("vehicles");

            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

            System.out.println("Enter a command or enter \"help\" for instructions.");
            String line = reader.readLine();
            while (line != null) {
                if (line.trim().toLowerCase().startsWith("help")) {
                    printInstructions();
                } else if (line.trim().toLowerCase().startsWith("exit")) {
                    break;
                } else if (line.trim().toLowerCase().startsWith("query")) {
                    doQuery(line.substring("query".length()).trim());
                } else if (line.trim().toLowerCase().startsWith("select")) {
                    doSelect(line.substring("select".length()).trim());
                } else if (line.trim().toLowerCase().startsWith("deselect")) {
                    doDeselect(line.substring("deselect".length()).trim());
                } else {
                    System.out.println("Unrecognized command.");
                }


                System.out.println("Enter a command or enter \"help\" for instructions.");
                line = reader.readLine();
            }

        } catch(Exception x){
            x.printStackTrace();
            System.exit(1);
        }
        hz.shutdown();
        System.out.println("BYE");
    }

    private static void printInstructions(){
        System.out.println("To issue a query against vehicles:");
        System.out.println("\tquery latitude between 88 and 89 and longitude > -88");
        System.out.println();
        System.out.println("To select a set of vehicles and display them on the map:");
        System.out.println("\tselect latitude between 88 and 89 and longitude > -88");
        System.out.println();
        System.out.println("To deselect a set of vehicles and reset their color on the map:");
        System.out.println("\tdeselect latitude between 88 and 89 and longitude > -88");
        System.out.println();
        System.out.println("To exit:");
        System.out.println("\texit");
        System.out.println();
    }

    private static void doSelect(String query) {
        vehicles.executeOnEntries(new UpdateEntryStatusProcessor("SELECTED"), new SqlPredicate(query));
    }

    private static void doDeselect(String query) {
        vehicles.executeOnEntries(new UpdateEntryStatusProcessor(""), new SqlPredicate(query));
    }

    private static void doQuery(String query) {
        Collection<HazelcastJsonValue> selectedVehicles = vehicles.values(new SqlPredicate(query));
        for(HazelcastJsonValue j: selectedVehicles){
            System.out.println("\t" + j.toString());
        }
    }
}
