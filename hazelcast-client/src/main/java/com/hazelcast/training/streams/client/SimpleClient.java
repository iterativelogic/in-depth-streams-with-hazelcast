package com.hazelcast.training.streams.client;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class SimpleClient {
    private static final String BATCH_SIZE_PREFIX = "--batch=";
    private static final String RATE_PREFIX = "--rate=";
    private static final String COUNT_PREFIX = "--count=";
    private static final String MAP_PREFIX = "--map=";
    private static final long DEFAULT_MERCHANT_COUNT = 1000;
    private static final String MERCHANT_MAP_NAME = "merchants";

    public static void main(String []args) {

        long frequency = 1000L;
        int batchSize = 1;
        long count = DEFAULT_MERCHANT_COUNT;
        String mapName = MERCHANT_MAP_NAME;

        if (args.length > 0){
            for(String arg:args){
                if (arg.startsWith(RATE_PREFIX)) {
                    try {
                        long rate = Long.parseLong(arg.substring(RATE_PREFIX.length()));
                        if (rate < 1 || rate > 1000){
                            System.err.println("rate must be in [1,1000]");
                            System.exit(1);
                        }
                        System.out.println(String.format("rate set to %d", rate));
                        frequency = 1000L/rate;

                    } catch(NumberFormatException x){
                        System.err.println("The rate argument could not be parsed as a number");
                        System.exit(1);
                    }
                } else if (arg.startsWith(COUNT_PREFIX)) {
                    try {
                        count = Long.parseLong(arg.substring(COUNT_PREFIX.length()));
                        System.out.println(String.format("count set to %d", count));
                    } catch (NumberFormatException x) {
                        System.err.println("The count argument could not be parsed as a number");
                        System.exit(1);
                    }
                } else if (arg.startsWith(BATCH_SIZE_PREFIX)){
                    try {
                        batchSize = Integer.parseInt(arg.substring(BATCH_SIZE_PREFIX.length()));
                        System.out.println(String.format("batch size set to %d", batchSize));
                    } catch(NumberFormatException x){
                        System.err.println("Batch size argument could not be parsed as a number");
                        System.exit(1);
                    }
                } else if (arg.startsWith(MAP_PREFIX)){
                    try {
                        mapName = arg.substring(MAP_PREFIX.length());
                        System.out.println(String.format("map name is set to %s", mapName));
                    } catch(NumberFormatException x){
                        System.err.println("Batch size argument could not be parsed as a number");
                        System.exit(1);
                    }
                } else {
                    System.err.println("Unrecognized argument: " + arg);
                    System.exit(1);
                }
            }
        }

        HazelcastInstance hz = HazelcastClient.newHazelcastClient();

        IMap<Integer, Merchant> map = hz.getMap(mapName);

        ScheduledExecutorService execService = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {

            @Override
            public Thread newThread(Runnable r) {
                Thread result = new Thread(r);
                result.setDaemon(true);
                return result;
            }
        });
        execService.scheduleAtFixedRate(new PutterGetter(map, (int) count, batchSize), 0, frequency, TimeUnit.MILLISECONDS);
//        execService.scheduleAtFixedRate(new Echo(), 0, frequency, TimeUnit.MILLISECONDS);

    }

    private static class Echo implements Runnable {
        public void run(){
            System.out.println("DO SOMETHING");
        }
    }

    private  static class PutterGetter implements Runnable {

        private int batchSize;
        private int count;
        private Random rand = new Random();
        private Map<Integer, Merchant> merchants;
        private Map<Integer, Merchant> batch;


        PutterGetter(Map<Integer, Merchant> merchantMap, int count, int batchSize){
            this.count = count;
            this.merchants = merchantMap;
            this.batchSize = batchSize;
            this.batch = new HashMap<>(batchSize);
        }

        @Override
        public void run() {
            boolean put = rand.nextFloat() > .5f;
//            boolean put = true;

            if (put) {
                // put batchSize merchants
                Merchant m = null;
                if (batchSize == 1){
                    m = Merchant.fake(rand.nextInt(count));
                    merchants.put(m.getMerchantId(), m);
                    System.out.println("PUT " + m);
                } else {
                    for(int i=0;i< batchSize; ++i) {
                        m = Merchant.fake(rand.nextInt(count));
                        batch.put(m.getMerchantId(), m);
                    }
                    merchants.putAll(batch);
                    System.out.println("PUT " + batchSize + " merchants");
                }
            } else {
                int id = rand.nextInt(count);
                Merchant m = merchants.get(id);
                System.out.println("GET " + m);
            }
        }

    }
}