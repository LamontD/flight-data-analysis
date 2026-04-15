package com.lamontd.travel.flight.asqp.view;

import com.lamontd.travel.flight.util.FlightDataIndex;

import java.util.Scanner;

/**
 * Base interface for view renderers
 */
public interface ViewRenderer {
    /**
     * Renders the view
     * @param index Flight data index
     * @param scanner Scanner for user input
     */
    void render(FlightDataIndex index, Scanner scanner);
}
