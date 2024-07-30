package org.example;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RateLimiter {
    private final int maxRequests;
    private int requestCount;
    private final ScheduledExecutorService scheduler;

    public RateLimiter(int maxRequests, long time, TimeUnit timeUnit) {
        this.maxRequests = maxRequests;
        this.requestCount = 0;
        this.scheduler = Executors.newScheduledThreadPool(1);

        // Schedule a task to reset the request count at fixed rate
        scheduler.scheduleAtFixedRate(this::resetRequestCount, time, time, timeUnit);
    }

    // Method to check if a request is allowed
    public synchronized boolean allowRequest() {
        if (requestCount < maxRequests) {
            requestCount++;
            return true;
        }
        return false;
    }

    public synchronized void await() {
        try {
            this.wait();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // Method to reset the request count
    private synchronized void resetRequestCount() {
        requestCount = 0;
        this.notify();
    }

    // Method to shut down the scheduler
    public void shutdown() {
        scheduler.shutdown();
    }
}

