package com.lamontd.travel.flight.asqp.examples;

import com.lamontd.travel.flight.mapper.AirportCodeMapper;
import com.lamontd.travel.flight.mapper.CarrierCodeMapper;
import com.lamontd.travel.flight.mapper.CountryCodeMapper;
import com.lamontd.travel.flight.model.AirportInfo;
import com.lamontd.travel.flight.model.CarrierInfo;
import com.lamontd.travel.flight.model.CountryInfo;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility to explore ISO 3166-1 country data and its integration
 */
public class CountryDataExplorer {

    public static void main(String[] args) {
        CountryCodeMapper countryMapper = CountryCodeMapper.getDefault();
        AirportCodeMapper airportMapper = AirportCodeMapper.getDefault();
        CarrierCodeMapper carrierMapper = CarrierCodeMapper.getDefault();

        System.out.println("=== ISO 3166-1 Country Data Summary ===");
        System.out.println("Total countries loaded: " + countryMapper.size());
        System.out.println();

        // Show sample countries
        System.out.println("=== Sample Countries ===");
        String[] sampleCodes = {"US", "CA", "GB", "FR", "DE", "CN", "JP", "AU", "BR", "IN"};
        for (String code : sampleCodes) {
            countryMapper.getByAlpha2(code).ifPresent(country -> {
                System.out.printf("%-3s  %-3s  %s%n",
                        country.getAlpha2Upper(),
                        country.getAlpha3Upper(),
                        country.getName());
            });
        }
        System.out.println();

        // Show detailed country info
        System.out.println("=== Detailed Country Info (United States) ===");
        countryMapper.getByAlpha2("US").ifPresent(country -> {
            System.out.println("ID: " + country.getId());
            System.out.println("Alpha-2: " + country.getAlpha2Upper());
            System.out.println("Alpha-3: " + country.getAlpha3Upper());
            System.out.println("Name: " + country.getName());
        });
        System.out.println();

        // Test conversions
        System.out.println("=== Code Conversions ===");
        System.out.println("US -> " + countryMapper.alpha2ToAlpha3("US").orElse("N/A"));
        System.out.println("USA -> " + countryMapper.alpha3ToAlpha2("USA").orElse("N/A"));
        System.out.println("GB -> " + countryMapper.alpha2ToAlpha3("GB").orElse("N/A"));
        System.out.println("GBR -> " + countryMapper.alpha3ToAlpha2("GBR").orElse("N/A"));
        System.out.println();

        // Search functionality
        System.out.println("=== Search: 'United' ===");
        List<CountryInfo> unitedCountries = countryMapper.searchByName("United");
        unitedCountries.forEach(country -> {
            System.out.printf("%s - %s%n", country.getAlpha2Upper(), country.getName());
        });
        System.out.println();

        // Integration with airport data
        System.out.println("=== Airport-Country Integration ===");
        System.out.println("US Airports with country validation:");
        List<AirportInfo> usAirports = airportMapper.getAirportsByCountry("United States");
        System.out.println("Found " + usAirports.size() + " airports");

        // Show a few examples
        usAirports.stream()
                .limit(5)
                .forEach(airport -> {
                    String country = airport.getCountry().orElse("Unknown");
                    // Try to find the country code
                    CountryInfo countryInfo = countryMapper.getByName(country).orElse(null);
                    String countryCode = countryInfo != null ? countryInfo.getAlpha2Upper() : "??";

                    System.out.printf("  %s (%s) - %s [%s]%n",
                            airport.getCode(),
                            countryCode,
                            airport.getName(),
                            country);
                });
        System.out.println();

        // Integration with carrier data
        System.out.println("=== Carrier-Country Integration ===");
        Map<String, Long> carriersByCountry = carrierMapper.getAllCarriers().stream()
                .filter(c -> c.getCountry().isPresent())
                .collect(Collectors.groupingBy(
                        c -> c.getCountry().get(),
                        Collectors.counting()
                ));

        System.out.println("Top 10 countries by carrier count:");
        carriersByCountry.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .forEach(entry -> {
                    CountryInfo countryInfo = countryMapper.getByName(entry.getKey()).orElse(null);
                    String code = countryInfo != null ? countryInfo.getAlpha2Upper() : "??";
                    System.out.printf("  %-3s  %-25s: %d carriers%n",
                            code, entry.getKey(), entry.getValue());
                });
        System.out.println();

        // Show countries with no carriers
        System.out.println("=== Statistics ===");
        long countriesWithCarriers = countryMapper.getAllCountries().stream()
                .filter(country -> {
                    List<CarrierInfo> carriers = carrierMapper.getAllCarriers().stream()
                            .filter(c -> c.getCountry().isPresent())
                            .filter(c -> c.getCountry().get().equalsIgnoreCase(country.getName()))
                            .toList();
                    return !carriers.isEmpty();
                })
                .count();

        System.out.println("Total countries in database: " + countryMapper.size());
        System.out.println("Countries with airlines: " + countriesWithCarriers);
        System.out.println("Countries without airlines: " + (countryMapper.size() - countriesWithCarriers));

        // Show alphabetically first and last
        System.out.println();
        System.out.println("=== First and Last Countries (Alphabetically) ===");
        List<CountryInfo> sorted = countryMapper.getAllCountries().stream()
                .sorted(Comparator.comparing(CountryInfo::getName))
                .toList();

        if (!sorted.isEmpty()) {
            CountryInfo first = sorted.get(0);
            CountryInfo last = sorted.get(sorted.size() - 1);
            System.out.println("First: " + first.getAlpha2Upper() + " - " + first.getName());
            System.out.println("Last: " + last.getAlpha2Upper() + " - " + last.getName());
        }
    }
}
