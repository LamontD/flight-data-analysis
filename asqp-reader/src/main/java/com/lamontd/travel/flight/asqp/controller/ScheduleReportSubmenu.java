package com.lamontd.travel.flight.asqp.controller;

import com.lamontd.travel.flight.asqp.index.FlightDataIndex;
import com.lamontd.travel.flight.asqp.view.FlightScheduleView;

import java.util.Scanner;

/**
 * Submenu controller for Schedule Report options
 */
public class ScheduleReportSubmenu implements SubmenuController {

    @Override
    public void display(FlightDataIndex index, Scanner scanner) {
        boolean running = true;

        while (running) {
            displayMenu();

            String input = scanner.nextLine().trim();

            switch (input) {
                case "1":
                    new FlightScheduleView().render(index, scanner);
                    break;
                case "2":
                    running = false;
                    break;
                default:
                    System.out.println("\nInvalid option. Please select 1-2.");
            }
        }
    }

    private void displayMenu() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("SCHEDULE REPORT");
        System.out.println("=".repeat(50));
        System.out.println("1. Schedule Analysis");
        System.out.println("2. Return to Main Menu");
        System.out.println("=".repeat(50));
        System.out.print("Select an option (1-2): ");
    }
}
