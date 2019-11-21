package com.hazelcast.training.streams.client;

import com.hazelcast.nio.serialization.Portable;
import com.hazelcast.nio.serialization.PortableFactory;

public class DefaultPortableFactory implements PortableFactory {

    public static int ID = 1;

    @Override
    public Portable create(int i) {
//        if (i == Merchant.CLASSID)
//            return new Merchant();
//        else
            return null;
    }
}