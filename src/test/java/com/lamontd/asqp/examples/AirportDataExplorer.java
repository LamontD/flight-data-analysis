package com.lamontd.asqp.examples;

import com.lamontd.asqp.mapper.AirportCodeMapper;
import com.lamontd.asqp.model.AirportInfo;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility to explore the OpenFlights airport data
 */
public class AirportDataExplorer {

    public static void main(String[] args) {
        AirportCodeMapper mapper = AirportCodeMapper.getDefault();

        System.out.println("=== OpenFlights Airport Data Summary ===");
        System.out.println("Total airports loaded: " + mapper.size());
        System.out.println();

        // Show airports by country
        System.out.println("=== Top 10 Countries by Airport Count ===");
        Map<String, Long> byCountry = mapper.getAllAirports().stream()
                .filter(a -> a.getCountry().isPresent())
                .collect(Collectors.groupingBy(
                        a -> a.getCountry().get(),
                        Collectors.counting()
                ));

        byCountry.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .forEach(e -> System.out.printf("  %-20s: %d airports%n", e.getKey(), e.getValue()));
        System.out.println();

        // Show US airports
        System.out.println("=== Major US Airports ===");
        String[] majorUSCodes = {"ATL", "ORD", "DFW", "DEN", "LAX", "JFK", "LAS", "MCO", "MIA", "CLT",
                                "SEA", "PHX", "EWR", "SFO", "BOS", "MSP", "DTW", "FLL", "LGA", "CVG"};

        for (String code : majorUSCodes) {
            mapper.getAirportInfo(code).ifPresent(airport -> {
                System.out.printf("%-3s  %-45s %s%n",
                        airport.getCode(),
                        airport.getName(),
                        airport.getCity());
            });
        }
        System.out.println();

        // Show airports with detailed info
        System.out.println("=== Sample Airport Detail (ATL) ===");
        mapper.getAirportInfo("ATL").ifPresent(airport -> {
            System.out.println("Code: " + airport.getCode());
            System.out.println("Name: " + airport.getName());
            System.out.println("City: " + airport.getCity());
            System.out.println("Country: " + airport.getCountry().orElse("N/A"));
            System.out.println("ICAO: " + airport.getIcao().orElse("N/A"));
            System.out.println("Coordinates: " +
                             airport.getLatitude().orElse(0.0) + ", " +
                             airport.getLongitude().orElse(0.0));
            System.out.println("Altitude: " + airport.getAltitude().orElse(0) + " feet");
            System.out.println("Timezone: UTC" +
                             (airport.getTimezone().orElse(0.0) >= 0 ? "+" : "") +
                             airport.getTimezone().orElse(0.0));
            System.out.println("DST: " + airport.getDst().orElse("N/A"));
            System.out.println("TZ Database: " + airport.getTzDatabase().orElse("N/A"));
            System.out.println("Type: " + airport.getType().orElse("N/A"));
            System.out.println("Short Name: " + airport.getShortDisplayName());
            System.out.println("Full Name: " + airport.getFullDisplayName());
        });
        System.out.println();

        // Show airports in a specific city
        System.out.println("=== Airports in New York ===");
        List<AirportInfo> nyAirports = mapper.getAirportsByCity("New York");
        nyAirports.forEach(airport -> {
            System.out.printf("%s - %s (%s)%n",
                    airport.getCode(),
                    airport.getName(),
                    airport.getIcao().orElse("N/A"));
        });
        System.out.println();

        // Search functionality
        System.out.println("=== Search for 'International' Airports (First 10) ===");
        List<AirportInfo> intlAirports = mapper.searchByName("International");
        intlAirports.stream()
                .limit(10)
                .forEach(airport -> {
                    System.out.printf("%s - %s (%s, %s)%n",
                            airport.getCode(),
                            airport.getName(),
                            airport.getCity(),
                            airport.getCountry().orElse("Unknown"));
                });
        System.out.println();

        // Show highest altitude airports
        System.out.println("=== Top 10 Highest Altitude Airports ===");
        mapper.getAllAirports().stream()
                .filter(a -> a.getAltitude().isPresent())
                .sorted(Comparator.comparing(a -> a.getAltitude().get(), Comparator.reverseOrder()))
                .limit(10)
                .forEach(airport -> {
                    System.out.printf("%s - %s (%s): %,d feet%n",
                            airport.getCode(),
                            airport.getCity(),
                            airport.getCountry().orElse("Unknown"),
                            airport.getAltitude().get());
                });
    }
}
