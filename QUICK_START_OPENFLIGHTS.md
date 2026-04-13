# OpenFlights Data - Quick Start Guide

## Basic Usage

```java
CarrierCodeMapper mapper = CarrierCodeMapper.getDefault();
// Loaded 992+ carriers automatically!
```

## Common Queries

### 1. Look up a carrier by IATA code

```java
String name = mapper.getCarrierName("DL");
// "Delta Air Lines"
```

### 2. Get full carrier details

```java
Optional<CarrierInfo> info = mapper.getCarrierInfo("DL");
info.ifPresent(carrier -> {
    System.out.println("Code: " + carrier.getCode());              // DL
    System.out.println("Name: " + carrier.getName());              // Delta Air Lines
    System.out.println("ICAO: " + carrier.getIcao().orElse("N/A")); // DAL
    System.out.println("Callsign: " + carrier.getCallsign().orElse("N/A")); // DELTA
    System.out.println("Country: " + carrier.getCountry().orElse("N/A")); // United States
});
```

### 3. Find all US carriers

```java
mapper.getAllCarriers().stream()
    .filter(c -> c.getCountry().isPresent())
    .filter(c -> c.getCountry().get().equals("United States"))
    .forEach(c -> System.out.println(c.getCode() + " - " + c.getName()));
```

### 4. Search by callsign

```java
mapper.getAllCarriers().stream()
    .filter(c -> c.getCallsign().isPresent())
    .filter(c -> c.getCallsign().get().contains("DELTA"))
    .forEach(c -> System.out.println(c.getCode() + " - " + c.getName()));
```

### 5. Group carriers by country

```java
Map<String, Long> byCountry = mapper.getAllCarriers().stream()
    .filter(c -> c.getCountry().isPresent())
    .collect(Collectors.groupingBy(
        c -> c.getCountry().get(),
        Collectors.counting()
    ));

byCountry.forEach((country, count) -> 
    System.out.println(country + ": " + count + " carriers"));
```

### 6. Find carriers by ICAO code

```java
String icaoCode = "DAL";
mapper.getAllCarriers().stream()
    .filter(c -> c.getIcao().isPresent())
    .filter(c -> c.getIcao().get().equals(icaoCode))
    .findFirst()
    .ifPresent(c -> System.out.println("Found: " + c.getName()));
```

### 7. Check if a carrier exists

```java
if (mapper.hasCarrier("DL")) {
    System.out.println("Delta exists!");
}
```

### 8. Get total number of carriers

```java
System.out.println("Total carriers: " + mapper.size());
// Total carriers: 992
```

## Integration with Flight Records

### Enrich flight records with carrier info

```java
CsvFlightRecordReader reader = new CsvFlightRecordReader();
CarrierCodeMapper mapper = CarrierCodeMapper.getDefault();

List<FlightRecord> records = reader.readFromFile(path);

records.forEach(flight -> {
    CarrierInfo carrier = mapper.getCarrierInfo(flight.getCarrierCode()).orElse(null);
    if (carrier != null) {
        System.out.printf("Flight %s%s (%s) from %s to %s%n",
            flight.getCarrierCode(),
            flight.getFlightNumber(),
            carrier.getName(),
            flight.getOrigin(),
            flight.getDestination()
        );
    }
});
```

### Generate statistics by carrier

```java
Map<String, Long> flightsByCarrier = records.stream()
    .collect(Collectors.groupingBy(
        FlightRecord::getCarrierCode,
        Collectors.counting()
    ));

System.out.println("Top 10 carriers by flights:");
flightsByCarrier.entrySet().stream()
    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
    .limit(10)
    .forEach(entry -> {
        String name = mapper.getCarrierName(entry.getKey());
        System.out.printf("  %s (%s): %d flights%n", 
            entry.getKey(), name, entry.getValue());
    });
```

## Updating OpenFlights Data

### Download latest data

```bash
curl -o src/main/resources/data/airlines.dat \
  https://raw.githubusercontent.com/jpatokal/openflights/master/data/airlines.dat
```

### Verify the update

```bash
mvn test
```

### Check what changed

```bash
# Count carriers before
java -cp target/classes com.lamontd.asqp.examples.OpenFlightsCarrierExplorer | grep "Total carriers"

# Download update
curl -o src/main/resources/data/airlines.dat ...

# Count carriers after
mvn compile
java -cp target/classes com.lamontd.asqp.examples.OpenFlightsCarrierExplorer | grep "Total carriers"
```

## Common Carriers Reference

### Major US Airlines
| Code | Name | ICAO | Callsign |
|------|------|------|----------|
| DL | Delta Air Lines | DAL | DELTA |
| AA | American Airlines | AAL | AMERICAN |
| UA | United Airlines | UAL | UNITED |
| WN | Southwest Airlines | SWA | SOUTHWEST |
| B6 | JetBlue Airways | JBU | JETBLUE |
| AS | Alaska Airlines | ASA | ALASKA |
| NK | Spirit Airlines | NKS | SPIRIT WINGS |
| F9 | Frontier Airlines | FFT | FRONTIER FLIGHT |

### Regional Carriers
| Code | Name | ICAO | Callsign |
|------|------|------|----------|
| OO | SkyWest Airlines | SKW | SKYWEST |
| EV | ExpressJet Airlines | BTA | JET LINK |
| MQ | Envoy Air | ENY | ENVOY |
| 9E | Endeavor Air | FLG | FLAGSHIP |
| YX | Republic Airways | RPA | BRICKYARD |

### International Examples
| Code | Name | Country |
|------|------|---------|
| BA | British Airways | United Kingdom |
| AF | Air France | France |
| LH | Lufthansa | Germany |
| EK | Emirates | United Arab Emirates |
| QF | Qantas | Australia |
| ANA | All Nippon Airways | Japan |

## Troubleshooting

### Carrier not found?

```java
String code = "XX";
if (!mapper.hasCarrier(code)) {
    System.out.println("Carrier " + code + " not found");
    // Check if it's in the OpenFlights data
    // It might be inactive or not have a valid IATA code
}
```

### Want to add custom carriers?

```java
CarrierCodeMapper mapper = CarrierCodeMapper.getDefault();
// Add or override
mapper.addCarrier("XX", "Custom Airlines", "Custom Airlines Corp");
```

### Need to use old CSV format?

```java
CarrierCodeMapper mapper = new CarrierCodeMapper();
mapper.loadFromResource("/data/carriers.csv");  // Use old format
```

## Performance Tips

1. **Singleton pattern:** Use `getDefault()` to share one instance
   ```java
   // Good - shares one instance
   CarrierCodeMapper mapper = CarrierCodeMapper.getDefault();
   
   // Less efficient - creates new instance each time
   CarrierCodeMapper mapper = new CarrierCodeMapper();
   mapper.loadFromOpenFlightsResource("/data/airlines.dat");
   ```

2. **Cache lookups:** If doing many lookups, get the info once
   ```java
   // Good
   Map<String, CarrierInfo> cache = new HashMap<>();
   for (FlightRecord flight : records) {
       cache.computeIfAbsent(
           flight.getCarrierCode(),
           code -> mapper.getCarrierInfo(code).orElse(null)
       );
   }
   ```

3. **Filter early:** Don't load all carriers if you only need a subset
   ```java
   // If you only need US carriers, filter after loading
   Map<String, CarrierInfo> usCarriers = mapper.getAllCarriers().stream()
       .filter(c -> c.getCountry().equals("United States"))
       .collect(Collectors.toMap(CarrierInfo::getCode, c -> c));
   ```

## More Information

- **Full Integration Guide:** [OPENFLIGHTS_INTEGRATION.md](OPENFLIGHTS_INTEGRATION.md)
- **Migration Details:** [OPENFLIGHTS_MIGRATION_SUMMARY.md](OPENFLIGHTS_MIGRATION_SUMMARY.md)
- **Data Sources:** [CARRIER_DATA_SOURCES.md](CARRIER_DATA_SOURCES.md)
- **API Reference:** [CARRIER_MAPPER_SUMMARY.md](CARRIER_MAPPER_SUMMARY.md)
- **OpenFlights Project:** https://github.com/jpatokal/openflights
