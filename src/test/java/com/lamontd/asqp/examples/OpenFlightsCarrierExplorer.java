package com.lamontd.asqp.examples;

import com.lamontd.asqp.mapper.CarrierCodeMapper;
import com.lamontd.asqp.model.CarrierInfo;

import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility to explore the OpenFlights carrier data
 */
public class OpenFlightsCarrierExplorer {

    public static void main(String[] args) {
        CarrierCodeMapper mapper = CarrierCodeMapper.getDefault();

        System.out.println("=== OpenFlights Carrier Data Summary ===");
        System.out.println("Total carriers loaded: " + mapper.size());
        System.out.println();

        // Show all US carriers
        System.out.println("=== US Carriers (Active) ===");
        mapper.getAllCarriers().stream()
                .filter(c -> c.getCountry().isPresent() && c.getCountry().get().equals("United States"))
                .filter(CarrierInfo::isActive)
                .sorted(Comparator.comparing(CarrierInfo::getName))
                .limit(20)
                .forEach(c -> {
                    System.out.printf("%-3s %-30s ICAO: %-4s Callsign: %-15s%n",
                            c.getCode(),
                            c.getName(),
                            c.getIcao().orElse("N/A"),
                            c.getCallsign().orElse("N/A"));
                });
        System.out.println();

        // Show major carriers with full details
        System.out.println("=== Major US Carriers (Full Details) ===");
        String[] majorCarriers = {"DL", "AA", "UA", "WN", "B6", "AS", "NK", "F9"};
        for (String code : majorCarriers) {
            mapper.getCarrierInfo(code).ifPresent(info -> {
                System.out.println("Code: " + info.getCode());
                System.out.println("  Name: " + info.getName());
                System.out.println("  ICAO: " + info.getIcao().orElse("N/A"));
                System.out.println("  Callsign: " + info.getCallsign().orElse("N/A"));
                System.out.println("  Country: " + info.getCountry().orElse("N/A"));
                System.out.println("  Active: " + info.isActive());
                System.out.println();
            });
        }

        // Show carriers by country
        System.out.println("=== Carriers by Country (Top 10) ===");
        Map<String, Long> byCountry = mapper.getAllCarriers().stream()
                .filter(c -> c.getCountry().isPresent())
                .collect(Collectors.groupingBy(
                        c -> c.getCountry().get(),
                        Collectors.counting()
                ));

        byCountry.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .forEach(e -> System.out.println("  " + e.getKey() + ": " + e.getValue()));
    }
}
