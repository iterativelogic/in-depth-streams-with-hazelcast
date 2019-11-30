package com.hazelcast.training.streams.server;

import com.hazelcast.jet.Jet;
import com.hazelcast.jet.JetInstance;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServerBean
{
    @Bean
    public JetInstance instance(){
        return Jet.newJetInstance();
    }

    public static void main( String[] args ) {
        SpringApplication.run(ServerBean.class, args);
    }
}
