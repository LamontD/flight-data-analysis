package com.lamontd.travel.flight.index;

import java.util.Set;

/**
 * Minimal index interface for route network analysis.
 * Provides airport lists and route distance lookups needed for graph operations.
 */
public interface RouteIndex {
    /**
     * @return All airports that serve as origins in the dataset
     */
    Set<String> getOriginAirports();

    /**
     * @return All airports that serve as destinations in the dataset
     */
    Set<String> getDestinationAirports();

    /**
     * Gets all actual routes that exist in the flight data.
     * A route is represented as "ORIGIN-DESTINATION" (e.g., "ATL-ORD").
     * Only routes with actual flights are included.
     *
     * @return Set of route keys in "ORIGIN-DESTINATION" format
     */
    Set<String> getActualRoutes();

    /**
     * Calculate or retrieve the distance between two airports in miles.
     * @param origin Origin airport code
     * @param destination Destination airport code
     * @return Distance in miles, or 0.0 if airports not found or distance not computable
     */
    double getRouteDistance(String origin, String destination);
}
