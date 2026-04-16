package com.lamontd.travel.flight.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Auto-closeable utility for timing operations and logging their duration.
 * Use with try-with-resources for automatic timing:
 * <pre>
 * try (var timer = new PerformanceTimer("Load data")) {
 *     // operation
 * }
 * </pre>
 */
public class PerformanceTimer implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(PerformanceTimer.class);
    private final String operationName;
    private final long startNanos;

    public PerformanceTimer(String operationName) {
        this.operationName = operationName;
        this.startNanos = System.nanoTime();
        logger.debug("Starting: {}", operationName);
    }

    @Override
    public void close() {
        long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
        logger.info("{} completed in {} ms", operationName, durationMs);
    }
}
