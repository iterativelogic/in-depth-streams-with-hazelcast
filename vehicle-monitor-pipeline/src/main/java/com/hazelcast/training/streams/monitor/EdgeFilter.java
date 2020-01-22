package com.hazelcast.training.streams.monitor;

import java.io.Serializable;

/**
 * only passes an event when some threshold has been exceeded
 */
public class EdgeFilter implements Serializable {

    private boolean triggered;
    private double limit;

    public EdgeFilter(double limit){
        this.limit = limit;
        triggered = false;
    }

    boolean apply(double dist){
        if (triggered) return false;

        if (dist > limit){
            triggered = true;
            return true;
        } else {
            return false;
        }
    }
}
