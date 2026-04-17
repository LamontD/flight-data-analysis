package com.lamontd.travel.flight.asqp.controller;

import com.lamontd.travel.flight.asqp.index.FlightDataIndex;

import java.util.Scanner;

/**
 * Interface for submenu controllers that handle second-level menu navigation
 */
public interface SubmenuController {
    /**
     * Displays the submenu and handles user interaction
     * @param index The flight data index
     * @param scanner The scanner for user input
     */
    void display(FlightDataIndex index, Scanner scanner);
}
