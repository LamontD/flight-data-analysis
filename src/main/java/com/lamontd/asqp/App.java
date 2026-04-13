package com.lamontd.asqp;

import com.lamontd.asqp.model.FlightRecord;
import com.lamontd.asqp.reader.CsvFlightRecordReader;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

public class App {

    /**
     * Pre-computed indices for efficient data access
     */
    private static class FlightDataIndex {
        final List<FlightRecord> allRecords;

        // Indexed by various keys for O(1) or O(log n) lookups
        final Map<String, List<FlightRecord>> byCarrier;
        final Map<String, List<FlightRecord>> byOriginAirport;
        final Map<String, List<FlightRecord>> byDestinationAirport;
        final Map<String, List<FlightRecord>> byTailNumber;
        final Map<String, List<FlightRecord>> byFlightNumber;
        final Map<LocalDate, List<FlightRecord>> byDate;

        // Cached statistics (computed once)
        final long totalFlights;
        final long operatedFlights;
        final long cancelledFlights;
        final Map<String, Long> carrierCounts;
        final LocalDate minDate;
        final LocalDate maxDate;
        final long uniqueCarriers;
        final long uniqueAirports;

        FlightDataIndex(List<FlightRecord> records) {
            this.allRecords = records;
            this.totalFlights = records.size();

            System.out.println("Building data indices for efficient querying...");
            long startTime = System.currentTimeMillis();

            // Build all indices in a single pass where possible
            this.byCarrier = records.stream()
                    .collect(Collectors.groupingBy(FlightRecord::getCarrierCode));

            this.byOriginAirport = records.stream()
                    .collect(Collectors.groupingBy(FlightRecord::getOrigin));

            this.byDestinationAirport = records.stream()
                    .collect(Collectors.groupingBy(FlightRecord::getDestination));

            this.byTailNumber = records.stream()
                    .filter(r -> r.getTailNumber() != null && !r.getTailNumber().isEmpty())
                    .collect(Collectors.groupingBy(FlightRecord::getTailNumber));

            this.byFlightNumber = records.stream()
                    .collect(Collectors.groupingBy(
                            r -> r.getCarrierCode() + r.getFlightNumber()
                    ));

            this.byDate = records.stream()
                    .collect(Collectors.groupingBy(FlightRecord::getDepartureDate));

            // Compute statistics once
            this.operatedFlights = records.stream()
                    .filter(r -> !r.isCancelled())
                    .count();
            this.cancelledFlights = totalFlights - operatedFlights;

            this.carrierCounts = byCarrier.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> (long) e.getValue().size()
                    ));

            this.minDate = records.stream()
                    .map(FlightRecord::getDepartureDate)
                    .min(LocalDate::compareTo)
                    .orElse(null);

            this.maxDate = records.stream()
                    .map(FlightRecord::getDepartureDate)
                    .max(LocalDate::compareTo)
                    .orElse(null);

            this.uniqueCarriers = byCarrier.size();

            Set<String> airports = new HashSet<>();
            airports.addAll(byOriginAirport.keySet());
            airports.addAll(byDestinationAirport.keySet());
            this.uniqueAirports = airports.size();

            long endTime = System.currentTimeMillis();
            System.out.printf("Indices built in %d ms%n", (endTime - startTime));
            System.out.printf("Indexed: %d carriers, %d airports, %d tail numbers, %d flight numbers, %d dates%n",
                    byCarrier.size(), airports.size(), byTailNumber.size(), byFlightNumber.size(), byDate.size());
        }

        List<FlightRecord> getByCarrier(String carrierCode) {
            return byCarrier.getOrDefault(carrierCode, Collections.emptyList());
        }

        List<FlightRecord> getByOriginAirport(String airportCode) {
            return byOriginAirport.getOrDefault(airportCode, Collections.emptyList());
        }

        List<FlightRecord> getByTailNumber(String tailNumber) {
            return byTailNumber.getOrDefault(tailNumber.toUpperCase(), Collections.emptyList());
        }

        List<FlightRecord> getByFlightNumber(String carrierCode, String flightNumber) {
            return byFlightNumber.getOrDefault(carrierCode + flightNumber, Collections.emptyList());
        }
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java -jar asqp-reader.jar <csv-file-path>");
            System.out.println("\nExample with sample data:");
            processSampleData();
            return;
        }

        String filePath = args[0];
        List<FlightRecord> records = processFile(filePath);
        if (records != null) {
            FlightDataIndex index = new FlightDataIndex(records);
            runInteractiveMenu(index);
        }
    }

    private static void processSampleData() {
        Path samplePath = Paths.get("src/main/resources/data/sample-data.asc.groomed");
        List<FlightRecord> records = processFile(samplePath.toString());
        if (records != null) {
            FlightDataIndex index = new FlightDataIndex(records);
            showDataOverview(index);
        }
    }

    private static void runInteractiveMenu(FlightDataIndex index) {
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            System.out.println("\n" + "=".repeat(50));
            System.out.println("ASQP Flight Data Analysis Menu");
            System.out.println("=".repeat(50));
            System.out.println("1. Data Overview");
            System.out.println("2. Carrier View");
            System.out.println("3. Airport View");
            System.out.println("4. Airplane View");
            System.out.println("5. Flight View");
            System.out.println("6. Exit");
            System.out.println("=".repeat(50));
            System.out.print("Select an option (1-6): ");

            String input = scanner.nextLine().trim();

            switch (input) {
                case "1":
                    showDataOverview(index);
                    break;
                case "2":
                    showCarrierView(index, scanner);
                    break;
                case "3":
                    showAirportView(index, scanner);
                    break;
                case "4":
                    showAirplaneView(index, scanner);
                    break;
                case "5":
                    showFlightView(index, scanner);
                    break;
                case "6":
                    running = false;
                    System.out.println("\nThank you for using ASQP Flight Data Analysis!");
                    break;
                default:
                    System.out.println("\nInvalid option. Please select 1-6.");
            }
        }
        scanner.close();
    }

    private static void showDataOverview(FlightDataIndex index) {
        com.lamontd.asqp.mapper.CancellationCodeMapper cancellationMapper =
            com.lamontd.asqp.mapper.CancellationCodeMapper.getDefault();

        System.out.println("\n" + "=".repeat(50));
        System.out.println("DATA OVERVIEW");
        System.out.println("=".repeat(50));

        // Use pre-computed statistics from index
        System.out.println("\nTotal Flights: " + index.totalFlights);
        System.out.println("  - Operated Flights: " + index.operatedFlights + " (" +
                String.format("%.1f%%", (index.operatedFlights * 100.0 / index.totalFlights)) + ")");
        System.out.println("  - Cancelled Flights: " + index.cancelledFlights + " (" +
                String.format("%.1f%%", (index.cancelledFlights * 100.0 / index.totalFlights)) + ")");

        // Cancellation breakdown
        if (index.cancelledFlights > 0) {
            Map<String, Long> cancellationReasons = index.allRecords.stream()
                    .filter(FlightRecord::isCancelled)
                    .collect(Collectors.groupingBy(
                            r -> r.getCancellationCode().orElse("Unknown"),
                            Collectors.counting()
                    ));

            System.out.println("\n  Cancellation Breakdown:");
            cancellationReasons.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .forEach(entry -> {
                        String description = cancellationMapper.getFullDescription(entry.getKey());
                        System.out.printf("    %s: %,d (%.1f%%)%n",
                                description,
                                entry.getValue(),
                                (entry.getValue() * 100.0 / index.cancelledFlights));
                    });
        }

        // Use pre-computed date range
        if (index.minDate != null && index.maxDate != null) {
            System.out.println("\nDate Range: " + index.minDate + " to " + index.maxDate);
        }

        // Use pre-computed unique counts
        System.out.println("Unique Carriers: " + index.uniqueCarriers);
        System.out.println("Unique Airports: " + index.uniqueAirports);
    }

    private static void showCarrierView(FlightDataIndex index, Scanner scanner) {
        com.lamontd.asqp.mapper.CarrierCodeMapper carrierMapper =
            com.lamontd.asqp.mapper.CarrierCodeMapper.getDefault();
        com.lamontd.asqp.mapper.AirportCodeMapper airportMapper =
            com.lamontd.asqp.mapper.AirportCodeMapper.getDefault();

        System.out.println("\n" + "=".repeat(50));
        System.out.println("CARRIER VIEW");
        System.out.println("=".repeat(50));

        // Use pre-computed carrier counts from index
        System.out.println("\nFlights by Carrier:");
        index.carrierCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(entry -> {
                    String carrierName = carrierMapper.getCarrierName(entry.getKey());
                    System.out.println("  " + entry.getKey() + " (" + carrierName + "): " +
                            entry.getValue() + " flights");
                });

        System.out.print("\nEnter carrier code (or press Enter to cancel): ");
        String carrierCode = scanner.nextLine().trim().toUpperCase();

        if (carrierCode.isEmpty()) {
            return;
        }

        // Use indexed lookup - O(1) instead of O(n)
        List<FlightRecord> carrierFlights = index.getByCarrier(carrierCode);

        if (carrierFlights.isEmpty()) {
            System.out.println("\nNo flights found for carrier code: " + carrierCode);
            return;
        }

        String carrierName = carrierMapper.getCarrierName(carrierCode);
        System.out.println("\n" + "-".repeat(50));
        System.out.println("Carrier: " + carrierCode + " (" + carrierName + ")");
        System.out.println("Total Flights: " + carrierFlights.size());
        System.out.println("-".repeat(50));

        // Group by origin airport
        Map<String, Long> airportCounts = carrierFlights.stream()
                .collect(Collectors.groupingBy(
                        FlightRecord::getOrigin,
                        Collectors.counting()
                ));

        System.out.println("\nFlights by Origin Airport (largest to smallest):");
        airportCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(entry -> {
                    String airport = entry.getKey();
                    String city = airportMapper.getAirportCity(airport);
                    long count = entry.getValue();
                    String bar = createBar(count, 40,
                            airportCounts.values().stream().mapToLong(Long::longValue).max().orElse(1));
                    System.out.printf("  %s (%s): %3d flights %s%n",
                            airport, city, count, bar);
                });
    }

    private static void showAirportView(FlightDataIndex index, Scanner scanner) {
        com.lamontd.asqp.mapper.AirportCodeMapper airportMapper =
            com.lamontd.asqp.mapper.AirportCodeMapper.getDefault();
        com.lamontd.asqp.mapper.CancellationCodeMapper cancellationMapper =
            com.lamontd.asqp.mapper.CancellationCodeMapper.getDefault();

        System.out.println("\n" + "=".repeat(50));
        System.out.println("AIRPORT VIEW");
        System.out.println("=".repeat(50));

        System.out.print("\nEnter airport code: ");
        String airportCode = scanner.nextLine().trim().toUpperCase();

        if (airportCode.isEmpty()) {
            return;
        }

        // Use indexed lookup - O(1) instead of O(n)
        List<FlightRecord> airportFlights = index.getByOriginAirport(airportCode);

        if (airportFlights.isEmpty()) {
            System.out.println("\nNo flights found departing from airport: " + airportCode);
            return;
        }

        String city = airportMapper.getAirportCity(airportCode);
        System.out.println("\n" + "-".repeat(50));
        System.out.println("Airport: " + airportCode + " (" + city + ")");
        System.out.println("Total Departing Flights: " + airportFlights.size());
        System.out.println("-".repeat(50));

        // Group by date and cancellation status
        Map<LocalDate, Long> operatedByDate = airportFlights.stream()
                .filter(r -> !r.isCancelled())
                .collect(Collectors.groupingBy(
                        FlightRecord::getDepartureDate,
                        Collectors.counting()
                ));

        Map<LocalDate, Long> cancelledByDate = airportFlights.stream()
                .filter(FlightRecord::isCancelled)
                .collect(Collectors.groupingBy(
                        FlightRecord::getDepartureDate,
                        Collectors.counting()
                ));

        // Get all dates
        Set<LocalDate> allDates = new TreeSet<>();
        allDates.addAll(operatedByDate.keySet());
        allDates.addAll(cancelledByDate.keySet());

        long maxCount = Math.max(
                operatedByDate.values().stream().mapToLong(Long::longValue).max().orElse(1),
                cancelledByDate.values().stream().mapToLong(Long::longValue).max().orElse(1)
        );

        System.out.println("\nDaily Flight Histogram:");
        for (LocalDate date : allDates) {
            long operated = operatedByDate.getOrDefault(date, 0L);
            long cancelled = cancelledByDate.getOrDefault(date, 0L);

            System.out.println("\n  " + date + ":");
            String operatedBar = createBar(operated, 40, maxCount);
            String cancelledBar = createBar(cancelled, 40, maxCount);

            System.out.printf("    Operated:  %3d %s%n", operated, operatedBar);
            System.out.printf("    Cancelled: %3d %s%n", cancelled, cancelledBar);
        }

        // Summary statistics
        long totalOperated = operatedByDate.values().stream().mapToLong(Long::longValue).sum();
        long totalCancelled = cancelledByDate.values().stream().mapToLong(Long::longValue).sum();

        System.out.println("\n  Summary:");
        System.out.println("    Total Operated: " + totalOperated);
        System.out.println("    Total Cancelled: " + totalCancelled);
        System.out.printf("    Cancellation Rate: %.1f%%%n",
                (totalCancelled * 100.0 / (totalOperated + totalCancelled)));

        // Cancellation breakdown by reason
        if (totalCancelled > 0) {
            Map<String, Long> cancellationReasons = airportFlights.stream()
                    .filter(FlightRecord::isCancelled)
                    .collect(Collectors.groupingBy(
                            r -> r.getCancellationCode().orElse("Unknown"),
                            Collectors.counting()
                    ));

            System.out.println("\n  Cancellation Reasons:");
            cancellationReasons.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .forEach(entry -> {
                        String description = cancellationMapper.getFullDescription(entry.getKey());
                        System.out.printf("    %s: %d (%.1f%%)%n",
                                description,
                                entry.getValue(),
                                (entry.getValue() * 100.0 / totalCancelled));
                    });
        }
    }

    private static void showAirplaneView(FlightDataIndex index, Scanner scanner) {
        com.lamontd.asqp.mapper.AirportCodeMapper airportMapper =
            com.lamontd.asqp.mapper.AirportCodeMapper.getDefault();

        System.out.println("\n" + "=".repeat(50));
        System.out.println("AIRPLANE VIEW");
        System.out.println("=".repeat(50));

        System.out.print("\nEnter tail number: ");
        String tailNumber = scanner.nextLine().trim().toUpperCase();

        if (tailNumber.isEmpty()) {
            return;
        }

        // Use indexed lookup - O(1) instead of O(n)
        List<FlightRecord> allPlaneFlights = index.getByTailNumber(tailNumber);

        if (allPlaneFlights.isEmpty()) {
            System.out.println("\nNo flights found for tail number: " + tailNumber);
            return;
        }

        // Filter out cancelled and sort
        List<FlightRecord> planeFlights = allPlaneFlights.stream()
                .filter(r -> !r.isCancelled())
                .sorted(Comparator.comparing(FlightRecord::getDepartureDate)
                        .thenComparing(r -> r.getGateDeparture().orElse(LocalTime.MIN)))
                .toList();

        if (planeFlights.isEmpty()) {
            System.out.println("\nNo operated flights found for tail number: " + tailNumber);
            return;
        }

        System.out.println("\n" + "-".repeat(50));
        System.out.println("Tail Number: " + tailNumber);
        System.out.println("Total Operated Flights: " + planeFlights.size());
        System.out.println("-".repeat(50));

        // Group flights by departure date
        Map<LocalDate, List<FlightRecord>> flightsByDate = planeFlights.stream()
                .collect(Collectors.groupingBy(
                        FlightRecord::getDepartureDate,
                        TreeMap::new,
                        Collectors.toList()
                ));

        System.out.println("\nDaily Flight History:");

        for (Map.Entry<LocalDate, List<FlightRecord>> entry : flightsByDate.entrySet()) {
            LocalDate date = entry.getKey();
            List<FlightRecord> dailyFlights = entry.getValue();

            // Sort by departure time
            dailyFlights.sort(Comparator.comparing(r -> r.getGateDeparture().orElse(LocalTime.MIN)));

            // Build the route chain
            StringBuilder route = new StringBuilder();
            for (int i = 0; i < dailyFlights.size(); i++) {
                FlightRecord flight = dailyFlights.get(i);
                if (i == 0) {
                    route.append(flight.getOrigin());
                }
                route.append(" -> ").append(flight.getDestination());
            }

            System.out.println("\n  " + date + ":");
            System.out.println("    Route: " + route);
            System.out.println("    Legs: " + dailyFlights.size());

            // Show detailed leg information
            for (FlightRecord flight : dailyFlights) {
                String originCity = airportMapper.getAirportCity(flight.getOrigin());
                String destCity = airportMapper.getAirportCity(flight.getDestination());
                String depTime = flight.getGateDeparture().map(LocalTime::toString).orElse("--:--");
                String arrTime = flight.getGateArrival().map(LocalTime::toString).orElse("--:--");

                System.out.printf("      %s%s: %s (%s) [%s] -> %s (%s) [%s]%n",
                        flight.getCarrierCode(),
                        flight.getFlightNumber(),
                        flight.getOrigin(),
                        originCity,
                        depTime,
                        flight.getDestination(),
                        destCity,
                        arrTime);
            }
        }

        // Summary statistics
        System.out.println("\n" + "-".repeat(50));
        System.out.println("Summary:");
        System.out.println("  Total Days Operated: " + flightsByDate.size());
        System.out.println("  Total Flights: " + planeFlights.size());
        System.out.printf("  Average Flights per Day: %.1f%n",
                (double) planeFlights.size() / flightsByDate.size());

        // Most common airports
        Map<String, Long> airportFrequency = planeFlights.stream()
                .flatMap(r -> java.util.stream.Stream.of(r.getOrigin(), r.getDestination()))
                .collect(Collectors.groupingBy(
                        airport -> airport,
                        Collectors.counting()
                ));

        System.out.println("\n  Most Visited Airports:");
        airportFrequency.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .forEach(e -> {
                    String city = airportMapper.getAirportCity(e.getKey());
                    System.out.printf("    %s (%s): %d times%n", e.getKey(), city, e.getValue());
                });
    }

    private static void showFlightView(FlightDataIndex index, Scanner scanner) {
        com.lamontd.asqp.mapper.AirportCodeMapper airportMapper =
            com.lamontd.asqp.mapper.AirportCodeMapper.getDefault();
        com.lamontd.asqp.mapper.CarrierCodeMapper carrierMapper =
            com.lamontd.asqp.mapper.CarrierCodeMapper.getDefault();
        com.lamontd.asqp.mapper.CancellationCodeMapper cancellationMapper =
            com.lamontd.asqp.mapper.CancellationCodeMapper.getDefault();

        System.out.println("\n" + "=".repeat(50));
        System.out.println("FLIGHT VIEW");
        System.out.println("=".repeat(50));

        System.out.print("\nEnter flight number (format: CARRIER FLIGHT#, e.g., WN 5114): ");
        String input = scanner.nextLine().trim();

        if (input.isEmpty()) {
            return;
        }

        // Parse the input
        String[] parts = input.split("\\s+");
        if (parts.length != 2) {
            System.out.println("\nInvalid format. Please use format: CARRIER FLIGHT#");
            return;
        }

        String carrierCode = parts[0].toUpperCase();
        String flightNumber = parts[1];

        // Use indexed lookup - O(1)
        List<FlightRecord> flightRecords = index.getByFlightNumber(carrierCode, flightNumber);

        if (flightRecords.isEmpty()) {
            System.out.println("\nNo flights found for: " + carrierCode + " " + flightNumber);
            return;
        }

        String carrierName = carrierMapper.getCarrierName(carrierCode);
        System.out.println("\n" + "-".repeat(50));
        System.out.println("Flight: " + carrierCode + " " + flightNumber + " (" + carrierName + ")");
        System.out.println("Total Flight Records: " + flightRecords.size());
        System.out.println("-".repeat(50));

        // Group by departure date
        Map<LocalDate, List<FlightRecord>> flightsByDate = flightRecords.stream()
                .collect(Collectors.groupingBy(
                        FlightRecord::getDepartureDate,
                        TreeMap::new,
                        Collectors.toList()
                ));

        System.out.println("\nDaily Flight Report:");

        for (Map.Entry<LocalDate, List<FlightRecord>> entry : flightsByDate.entrySet()) {
            LocalDate date = entry.getKey();
            List<FlightRecord> dailyLegs = entry.getValue();

            // Sort legs by departure time
            dailyLegs.sort(Comparator.comparing(r -> r.getGateDeparture().orElse(LocalTime.MIN)));

            // Check if cancelled
            boolean allCancelled = dailyLegs.stream().allMatch(FlightRecord::isCancelled);
            boolean anyCancelled = dailyLegs.stream().anyMatch(FlightRecord::isCancelled);

            System.out.println("\n  " + date + ":");

            // Build the route chain (show for both operated and cancelled)
            StringBuilder route = new StringBuilder();
            for (int i = 0; i < dailyLegs.size(); i++) {
                FlightRecord leg = dailyLegs.get(i);
                if (i == 0) {
                    route.append(leg.getOrigin());
                }
                route.append(" -> ").append(leg.getDestination());
            }

            if (allCancelled) {
                System.out.println("    Status: CANCELLED");
                System.out.println("    Scheduled Route: " + route);
                System.out.println("    Legs: " + dailyLegs.size());
                if (!dailyLegs.isEmpty()) {
                    FlightRecord first = dailyLegs.get(0);
                    String reason = first.getCancellationCode()
                            .map(cancellationMapper::getFullDescription)
                            .orElse("Unknown reason");
                    System.out.println("    Reason: " + reason);
                }
            } else {
                System.out.println("    Status: OPERATED");
                System.out.println("    Route: " + route);
                System.out.println("    Legs: " + dailyLegs.size());

                if (anyCancelled) {
                    long cancelledLegs = dailyLegs.stream().filter(FlightRecord::isCancelled).count();
                    System.out.println("    Note: " + cancelledLegs + " leg(s) cancelled");
                }
            }

            // Show detailed leg information (for both operated and cancelled)
            System.out.println("    Details:");
            for (int i = 0; i < dailyLegs.size(); i++) {
                FlightRecord leg = dailyLegs.get(i);
                String originCity = airportMapper.getAirportCity(leg.getOrigin());
                String destCity = airportMapper.getAirportCity(leg.getDestination());
                String depTime = leg.getScheduledCrsDeparture() != null ?
                        leg.getScheduledCrsDeparture().toString() : "--:--";
                String arrTime = leg.getScheduledCrsArrival() != null ?
                        leg.getScheduledCrsArrival().toString() : "--:--";
                String status = leg.isCancelled() ? " [CANCELLED]" : "";
                String tailInfo = leg.getTailNumber() != null ? " (Tail: " + leg.getTailNumber() + ")" : "";

                System.out.printf("      Leg %d: %s (%s) [Sched: %s] -> %s (%s) [Sched: %s]%s%s%n",
                        i + 1,
                        leg.getOrigin(),
                        originCity,
                        depTime,
                        leg.getDestination(),
                        destCity,
                        arrTime,
                        tailInfo,
                        status);
            }
        }

        // Summary statistics
        long totalOperated = flightsByDate.values().stream()
                .filter(legs -> legs.stream().anyMatch(l -> !l.isCancelled()))
                .count();
        long totalCancelled = flightsByDate.values().stream()
                .filter(legs -> legs.stream().allMatch(FlightRecord::isCancelled))
                .count();

        // Calculate on-time performance (within 15 minutes of scheduled time)
        List<FlightRecord> operatedFlights = flightsByDate.values().stream()
                .flatMap(List::stream)
                .filter(r -> !r.isCancelled())
                .toList();

        long onTimeDepartures = operatedFlights.stream()
                .filter(r -> {
                    if (r.getScheduledCrsDeparture() == null || r.getGateDeparture().isEmpty()) {
                        return false;
                    }
                    LocalTime scheduled = r.getScheduledCrsDeparture();
                    LocalTime actual = r.getGateDeparture().get();
                    long minutesDiff = java.time.Duration.between(scheduled, actual).toMinutes();
                    return Math.abs(minutesDiff) <= 15;
                })
                .count();

        long onTimeArrivals = operatedFlights.stream()
                .filter(r -> {
                    if (r.getScheduledCrsArrival() == null || r.getGateArrival().isEmpty()) {
                        return false;
                    }
                    LocalTime scheduled = r.getScheduledCrsArrival();
                    LocalTime actual = r.getGateArrival().get();
                    long minutesDiff = java.time.Duration.between(scheduled, actual).toMinutes();
                    return Math.abs(minutesDiff) <= 15;
                })
                .count();

        long flightsWithDepData = operatedFlights.stream()
                .filter(r -> r.getScheduledCrsDeparture() != null && r.getGateDeparture().isPresent())
                .count();

        long flightsWithArrData = operatedFlights.stream()
                .filter(r -> r.getScheduledCrsArrival() != null && r.getGateArrival().isPresent())
                .count();

        System.out.println("\n" + "-".repeat(50));
        System.out.println("Summary:");
        System.out.println("  Total Days: " + flightsByDate.size());
        System.out.println("  Days Operated: " + totalOperated);
        System.out.println("  Days Cancelled: " + totalCancelled);
        if (flightsByDate.size() > 0) {
            System.out.printf("  Completion Rate: %.1f%%%n",
                    (totalOperated * 100.0 / flightsByDate.size()));
        }

        // On-time performance (within 15 minutes)
        if (flightsWithDepData > 0) {
            System.out.printf("  On-Time Departures: %d / %d (%.1f%%)%n",
                    onTimeDepartures, flightsWithDepData,
                    (onTimeDepartures * 100.0 / flightsWithDepData));
        }
        if (flightsWithArrData > 0) {
            System.out.printf("  On-Time Arrivals: %d / %d (%.1f%%)%n",
                    onTimeArrivals, flightsWithArrData,
                    (onTimeArrivals * 100.0 / flightsWithArrData));
        }

        // Most common route
        Map<String, Long> routeFrequency = flightsByDate.values().stream()
                .filter(legs -> legs.stream().anyMatch(l -> !l.isCancelled()))
                .map(legs -> {
                    StringBuilder route = new StringBuilder();
                    List<FlightRecord> sortedLegs = legs.stream()
                            .sorted(Comparator.comparing(r -> r.getGateDeparture().orElse(LocalTime.MIN)))
                            .toList();
                    for (int i = 0; i < sortedLegs.size(); i++) {
                        FlightRecord leg = sortedLegs.get(i);
                        if (i == 0) {
                            route.append(leg.getOrigin());
                        }
                        route.append(" -> ").append(leg.getDestination());
                    }
                    return route.toString();
                })
                .collect(Collectors.groupingBy(
                        route -> route,
                        Collectors.counting()
                ));

        if (!routeFrequency.isEmpty()) {
            System.out.println("\n  Route Variations:");
            routeFrequency.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .forEach(e -> {
                        System.out.printf("    %s: %d times%n", e.getKey(), e.getValue());
                    });
        }
    }

    private static List<FlightRecord> processFile(String filePath) {
        com.lamontd.asqp.mapper.CarrierCodeMapper carrierMapper =
            com.lamontd.asqp.mapper.CarrierCodeMapper.getDefault();
        com.lamontd.asqp.mapper.AirportCodeMapper airportMapper =
            com.lamontd.asqp.mapper.AirportCodeMapper.getDefault();
        CsvFlightRecordReader reader = new CsvFlightRecordReader(airportMapper);
        com.lamontd.asqp.mapper.CountryCodeMapper countryMapper =
            com.lamontd.asqp.mapper.CountryCodeMapper.getDefault();
        Path path = Paths.get(filePath);

        try {
            System.out.println("Reading flight records from: " + filePath);
            System.out.println("Loaded " + carrierMapper.size() + " carriers, " +
                             airportMapper.size() + " airports, and " +
                             countryMapper.size() + " countries");

            List<FlightRecord> records = reader.readFromFile(path);

            System.out.println("\nSuccessfully loaded " + records.size() + " flight records");
            return records;

        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private static void performDataQualityChecks(List<FlightRecord> records) {
        com.lamontd.asqp.mapper.CarrierCodeMapper carrierMapper =
            com.lamontd.asqp.mapper.CarrierCodeMapper.getDefault();
        com.lamontd.asqp.mapper.AirportCodeMapper airportMapper =
            com.lamontd.asqp.mapper.AirportCodeMapper.getDefault();

        long cancelledCount = records.stream()
                .filter(FlightRecord::isCancelled)
                .count();

        System.out.println("Cancelled flights: " + cancelledCount);
        System.out.println("Operated flights: " + (records.size() - cancelledCount));

            // Data Quality Validation
            System.out.println("\n=== Data Quality Checks ===");

            // Check for missing carriers
            Set<String> missingCarriers = records.stream()
                    .map(FlightRecord::getCarrierCode)
                    .filter(code -> !carrierMapper.hasCarrier(code))
                    .collect(Collectors.toSet());

            if (!missingCarriers.isEmpty()) {
                System.out.println("WARNING: Found " + missingCarriers.size() +
                                 " unknown carrier code(s):");
                missingCarriers.forEach(code ->
                    System.out.println("  - " + code + " (not in carrier database)"));
            } else {
                System.out.println("✓ All carrier codes found in database");
            }

            // Check for missing airports
            Set<String> allAirportCodes = new HashSet<>();
            records.forEach(record -> {
                allAirportCodes.add(record.getOrigin());
                allAirportCodes.add(record.getDestination());
            });

            Set<String> missingAirports = allAirportCodes.stream()
                    .filter(code -> !airportMapper.hasAirport(code))
                    .collect(Collectors.toSet());

            if (!missingAirports.isEmpty()) {
                System.out.println("WARNING: Found " + missingAirports.size() +
                                 " unknown airport code(s):");
                missingAirports.forEach(code ->
                    System.out.println("  - " + code + " (not in airport database)"));
            } else {
                System.out.println("✓ All airport codes found in database");
            }

            // Check for invalid flight times (arrival before departure)
            List<FlightRecord> invalidTimeFlights = records.stream()
                    .filter(record -> !record.isCancelled())
                    .filter(record -> {
                        // Check if both UTC gate departure and arrival are present
                        if (record.getUtcGateDeparture().isPresent() &&
                            record.getUtcGateArrival().isPresent()) {
                            // Check if arrival is before departure (on same day)
                            return record.getUtcGateArrival().get()
                                        .isBefore(record.getUtcGateDeparture().get());
                        }
                        return false;
                    })
                    .toList();

            if (!invalidTimeFlights.isEmpty()) {
                System.out.println("WARNING: Found " + invalidTimeFlights.size() +
                                 " flight(s) with arrival before departure:");
                invalidTimeFlights.stream()
                        .limit(5)  // Show first 5 examples
                        .forEach(flight -> {
                            System.out.printf("  - %s%s on %s: Departed %s, Arrived %s%n",
                                flight.getCarrierCode(),
                                flight.getFlightNumber(),
                                flight.getDepartureDate(),
                                flight.getGateDeparture().get(),
                                flight.getGateArrival().get());
                        });
                if (invalidTimeFlights.size() > 5) {
                    System.out.println("  ... and " + (invalidTimeFlights.size() - 5) + " more");
                }
                System.out.println("  (Note: These may be flights crossing midnight)");
            } else {
                System.out.println("✓ All flight times appear valid");
            }

            // Additional check: wheels up/down consistency (using UTC times for accurate comparison)
            List<FlightRecord> invalidWheelsTime = records.stream()
                    .filter(record -> !record.isCancelled())
                    .filter(record -> {
                        if (record.getUtcWheelsUp().isPresent() &&
                            record.getUtcWheelsDown().isPresent()) {
                            return record.getUtcWheelsDown().get()
                                        .isBefore(record.getUtcWheelsUp().get());
                        }
                        return false;
                    })
                    .toList();

            if (!invalidWheelsTime.isEmpty()) {
                System.out.println("WARNING: Found " + invalidWheelsTime.size() +
                                 " flight(s) with wheels down before wheels up (UTC):");
                invalidWheelsTime.stream()
                        .limit(3)
                        .forEach(flight -> {
                            System.out.printf("  - %s%s on %s: Wheels up %s (local), Wheels down %s (local)%n",
                                flight.getCarrierCode(),
                                flight.getFlightNumber(),
                                flight.getDepartureDate(),
                                flight.getWheelsUp().get(),
                                flight.getWheelsDown().get());
                        });
                if (invalidWheelsTime.size() > 3) {
                    System.out.println("  ... and " + (invalidWheelsTime.size() - 3) + " more");
                }
                System.out.println("  (Note: Checked using UTC times to account for timezone differences)");
            }

            // Show carrier statistics
            Map<String, Long> carrierCounts = records.stream()
                    .collect(Collectors.groupingBy(
                            FlightRecord::getCarrierCode,
                            Collectors.counting()
                    ));

            System.out.println("\nFlights by carrier:");
            carrierCounts.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(10)
                    .forEach(entry -> {
                        String carrierName = carrierMapper.getCarrierName(entry.getKey());
                        System.out.println("  " + entry.getKey() + " (" + carrierName + "): " + entry.getValue());
                    });

            if (!records.isEmpty()) {
                System.out.println("\nFirst record:");
                FlightRecord first = records.get(0);
                String carrierName = carrierMapper.getCarrierName(first.getCarrierCode());
                String originCity = airportMapper.getAirportCity(first.getOrigin());
                String destCity = airportMapper.getAirportCity(first.getDestination());

                System.out.println("  Flight: " + first.getCarrierCode() + first.getFlightNumber() +
                                 " (" + carrierName + ")");
                System.out.println("  Route: " + first.getOrigin() + " (" + originCity + ") -> " +
                                 first.getDestination() + " (" + destCity + ")");
                System.out.println("  Date: " + first.getDepartureDate());
                System.out.println("  Cancelled: " + first.isCancelled());
            }
    }

    /**
     * Creates a histogram of flights per day for each airport (origin) and displays
     * the top 10 airports by total volume.
     *
     * @param records List of flight records to analyze
     */
    private static void analyzeAirportDailyHistogram(List<FlightRecord> records) {
        com.lamontd.asqp.mapper.AirportCodeMapper airportMapper =
            com.lamontd.asqp.mapper.AirportCodeMapper.getDefault();

        // Create nested map: Airport -> Date -> Count
        Map<String, Map<LocalDate, Long>> airportHistogram = records.stream()
                .collect(Collectors.groupingBy(
                        FlightRecord::getOrigin,
                        Collectors.groupingBy(
                                FlightRecord::getDepartureDate,
                                Collectors.counting()
                        )
                ));

        // Calculate total flights per airport
        Map<String, Long> airportTotalFlights = airportHistogram.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().values().stream()
                                .mapToLong(Long::longValue)
                                .sum()
                ));

        // Get top 10 airports by volume
        List<Map.Entry<String, Long>> top10Airports = airportTotalFlights.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .toList();

        System.out.println("\n=== Top 10 Airports by Daily Flight Volume ===\n");

        for (Map.Entry<String, Long> airportEntry : top10Airports) {
            String airportCode = airportEntry.getKey();
            Long totalFlights = airportEntry.getValue();
            String airportCity = airportMapper.getAirportCity(airportCode);
            Map<LocalDate, Long> dailyFlights = airportHistogram.get(airportCode);

            System.out.println(airportCode + " (" + airportCity + ") - Total: " + totalFlights + " flights");

            // Sort daily flights by date
            dailyFlights.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(dateEntry -> {
                        LocalDate date = dateEntry.getKey();
                        Long count = dateEntry.getValue();
                        String bar = createBar(count, 50, getMaxDailyFlights(dailyFlights));
                        System.out.printf("  %s: %3d flights %s%n", date, count, bar);
                    });

            // Calculate statistics
            DoubleSummaryStatistics stats = dailyFlights.values().stream()
                    .mapToDouble(Long::doubleValue)
                    .summaryStatistics();

            System.out.printf("  Stats: Avg=%.1f, Min=%d, Max=%d, Days=%d%n%n",
                    stats.getAverage(),
                    (long)stats.getMin(),
                    (long)stats.getMax(),
                    dailyFlights.size());
        }
    }

    /**
     * Helper method to get the maximum daily flight count for an airport
     */
    private static long getMaxDailyFlights(Map<LocalDate, Long> dailyFlights) {
        return dailyFlights.values().stream()
                .mapToLong(Long::longValue)
                .max()
                .orElse(1);
    }

    /**
     * Creates a simple text-based bar for histogram visualization
     *
     * @param value Current value
     * @param maxBarLength Maximum length of the bar in characters
     * @param maxValue Maximum value in the dataset
     * @return String representation of the bar
     */
    private static String createBar(long value, int maxBarLength, long maxValue) {
        if (maxValue == 0) return "";
        int barLength = (int) ((value * maxBarLength) / maxValue);
        return "[" + "=".repeat(Math.max(0, barLength)) + "]";
    }
}
