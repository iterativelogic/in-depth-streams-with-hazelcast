package com.hazelcast.training.streams.server;

import com.hazelcast.jet.Jet;
import com.hazelcast.jet.JetInstance;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class SpringServer
{
    public static void main( String[] args ) {
        ClassPathXmlApplicationContext app = new ClassPathXmlApplicationContext("jet-spring-application.xml");
    }
}
