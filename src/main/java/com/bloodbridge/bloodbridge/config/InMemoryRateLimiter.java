package com.bloodbridge.bloodbridge.config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class InMemoryRateLimiter {

    private final Map<String, Window> buckets = new ConcurrentHashMap<>();
    private final int maxRequests;
    private final long windowMillis;

    public InMemoryRateLimiter(int maxRequests, long windowSeconds) {
        this.maxRequests = maxRequests;
        this.windowMillis = windowSeconds * 1000;
    }

    public boolean tryAcquire(String key) {
        long now = System.currentTimeMillis();
        Window window = buckets.computeIfAbsent(key, k -> new Window(now));
        synchronized (window) {
            if (now - window.start > windowMillis) {
                window.start = now;
                window.count.set(0);
            }
            if (window.count.get() >= maxRequests) {
                return false;
            }
            window.count.incrementAndGet();
            return true;
        }
    }

    public int getRemaining(String key) {
        long now = System.currentTimeMillis();
        Window window = buckets.get(key);
        if (window == null) return maxRequests;
        synchronized (window) {
            if (now - window.start > windowMillis) {
                return maxRequests;
            }
            return Math.max(0, maxRequests - window.count.get());
        }
    }

    public int getActiveKeyCount() {
        return buckets.size();
    }

    public void evictStaleKeys() {
        long now = System.currentTimeMillis();
        buckets.entrySet().removeIf(entry -> {
            Window window = entry.getValue();
            synchronized (window) {
                return now - window.start > windowMillis;
            }
        });
    }

    private static class Window {
        volatile long start;
        final AtomicInteger count = new AtomicInteger(0);

        Window(long start) {
            this.start = start;
        }
    }
}
