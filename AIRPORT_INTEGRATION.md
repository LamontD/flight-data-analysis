# Airport Data Integration

## Overview

The ASQP Reader now includes comprehensive airport data from **OpenFlights**, providing information about **6,033 airports worldwide**.

## Data Source

**OpenFlights Airports Database**
- **GitHub:** https://github.com/jpatokal/openflights
- **Direct Data:** https://raw.githubusercontent.com/jpatokal/openflights/master/data/airports.dat
- **License:** Open Database License (ODbL)
- **Coverage:** 6,033 airports with valid IATA codes (from 7,698 total records)

## AirportInfo Model

```java
public class AirportInfo {
    private String code;          // IATA code (3-letter) - "ATL"
    private String name;          // Airport name
    private String city;          // City served - "Atlanta"
    private String country;       // Country - "United States"
    private String icao;          // ICAO code (4-letter) - "KATL"
    private Double latitude;      // Decimal degrees
    private Double longitude;     // Decimal degrees
    private Integer altitude;     // Feet above sea level
    private Double timezone;      // Hours offset from UTC
    private String dst;           // DST: E/A/S/O/Z/N/U
    private String tzDatabase;    // Timezone (tz format)
    private String type;          // airport, station, etc.
}
```

### Field Descriptions

| Field | Type | Description | Example |
|-------|------|-------------|---------|
| code | String | 3-letter IATA code | "ATL" |
| name | String | Full airport name | "Hartsfield Jackson Atlanta International Airport" |
| city | String | Main city served | "Atlanta" |
| country | Optional<String> | Country name | "United States" |
| icao | Optional<String> | 4-letter ICAO code | "KATL" |
| latitude | Optional<Double> | Latitude in decimal degrees | 33.6367 |
| longitude | Optional<Double> | Longitude in decimal degrees | -84.428101 |
| altitude | Optional<Integer> | Elevation in feet | 1026 |
| timezone | Optional<Double> | Hours offset from UTC | -5.0 |
| dst | Optional<String> | DST rule (E/A/S/O/Z/N/U) | "A" (US/Canada) |
| tzDatabase | Optional<String> | Timezone in tz format | "America/New_York" |
| type | Optional<String> | Type of facility | "airport" |

### DST Codes

- **E** - Europe
- **A** - US/Canada
- **S** - South America
- **O** - Australia
- **Z** - New Zealand
- **N** - None
- **U** - Unknown

## AirportCodeMapper

### Loading

```java
// Load default (singleton)
AirportCodeMapper mapper = AirportCodeMapper.getDefault();
// Loads 6,033 airports automatically

// Load from file
AirportCodeMapper mapper = AirportCodeMapper.fromOpenFlightsFile(
    Paths.get("airports.dat")
);
```

### Basic Queries

```java
// Get airport info
Optional<AirportInfo> info = mapper.getAirportInfo("ATL");

// Get airport name
String name = mapper.getAirportName("ATL");
// "Hartsfield Jackson Atlanta International Airport"

// Get city
String city = mapper.getAirportCity("ATL");
// "Atlanta"

// Get display names
String shortName = mapper.getShortDisplayName("ATL");
// "Atlanta (ATL)"

String fullName = mapper.getFullDisplayName("ATL");
// "Hartsfield Jackson Atlanta International Airport (Atlanta, United States)"

// Check existence
boolean exists = mapper.hasAirport("ATL");  // true

// Get count
int count = mapper.size();  // 6033
```

### Advanced Queries

```java
// Find airports by country
List<AirportInfo> usAirports = mapper.getAirportsByCountry("United States");
// Returns 500+ US airports

// Find airports by city
List<AirportInfo> nyAirports = mapper.getAirportsByCity("New York");
// Returns [JFK, LGA, ...]

// Search airports by name
List<AirportInfo> results = mapper.searchByName("International");
// Returns all airports with "International" in name or city

// Get all codes
Set<String> codes = mapper.getAllCodes();

// Get all airports
Collection<AirportInfo> all = mapper.getAllAirports();
```

## Usage Examples

### Basic Airport Lookup

```java
AirportCodeMapper mapper = AirportCodeMapper.getDefault();

AirportInfo atl = mapper.getAirportInfo("ATL").orElseThrow();
System.out.println("Name: " + atl.getName());
System.out.println("City: " + atl.getCity());
System.out.println("ICAO: " + atl.getIcao().get());
System.out.println("Coordinates: " + atl.getLatitude().get() + ", " + 
                                     atl.getLongitude().get());
```

### Enriching Flight Records

```java
CsvFlightRecordReader reader = new CsvFlightRecordReader();
AirportCodeMapper airportMapper = AirportCodeMapper.getDefault();
CarrierCodeMapper carrierMapper = CarrierCodeMapper.getDefault();

List<FlightRecord> records = reader.readFromFile(path);

records.forEach(flight -> {
    String carrier = carrierMapper.getCarrierName(flight.getCarrierCode());
    String origin = airportMapper.getAirportCity(flight.getOrigin());
    String dest = airportMapper.getAirportCity(flight.getDestination());
    
    System.out.printf("%s%s: %s (%s) -> %s (%s)%n",
        flight.getCarrierCode(),
        flight.getFlightNumber(),
        flight.getOrigin(),
        origin,
        flight.getDestination(),
        dest
    );
});
```

### Finding Airports in a Region

```java
// Find all US airports
List<AirportInfo> usAirports = mapper.getAirportsByCountry("United States");
System.out.println("US airports: " + usAirports.size());

// Find all airports in New York
List<AirportInfo> nyAirports = mapper.getAirportsByCity("New York");
nyAirports.forEach(airport -> {
    System.out.println(airport.getCode() + " - " + airport.getName());
});
// Output:
// JFK - John F Kennedy International Airport
// LGA - La Guardia Airport
```

### Distance Calculations

```java
AirportInfo origin = mapper.getAirportInfo("ATL").get();
AirportInfo dest = mapper.getAirportInfo("LAX").get();

double lat1 = origin.getLatitude().get();
double lon1 = origin.getLongitude().get();
double lat2 = dest.getLatitude().get();
double lon2 = dest.getLongitude().get();

// Use haversine formula or similar for distance calculation
```

### Finding Nearby Airports

```java
AirportInfo reference = mapper.getAirportInfo("JFK").get();
double refLat = reference.getLatitude().get();
double refLon = reference.getLongitude().get();

List<AirportInfo> nearby = mapper.getAllAirports().stream()
    .filter(a -> a.getLatitude().isPresent() && a.getLongitude().isPresent())
    .filter(a -> {
        double dist = calculateDistance(refLat, refLon,
                                       a.getLatitude().get(),
                                       a.getLongitude().get());
        return dist < 100; // Within 100 km
    })
    .collect(Collectors.toList());
```

## Integration with Flight Data

### Example Output

**Before:**
```
Flight: DL5030
Route: CVG -> LGA
```

**After:**
```
Flight: DL5030 (Delta Air Lines)
Route: CVG (Cincinnati) -> LGA (New York)
```

### Complete Example

```java
public class FlightReporter {
    private final CarrierCodeMapper carrierMapper;
    private final AirportCodeMapper airportMapper;
    
    public FlightReporter() {
        this.carrierMapper = CarrierCodeMapper.getDefault();
        this.airportMapper = AirportCodeMapper.getDefault();
    }
    
    public String formatFlight(FlightRecord flight) {
        String carrier = carrierMapper.getCarrierName(flight.getCarrierCode());
        
        AirportInfo origin = airportMapper.getAirportInfo(flight.getOrigin()).orElse(null);
        AirportInfo dest = airportMapper.getAirportInfo(flight.getDestination()).orElse(null);
        
        String originStr = origin != null ? origin.getShortDisplayName() : flight.getOrigin();
        String destStr = dest != null ? dest.getShortDisplayName() : flight.getDestination();
        
        return String.format("%s%s (%s): %s -> %s on %s",
            flight.getCarrierCode(),
            flight.getFlightNumber(),
            carrier,
            originStr,
            destStr,
            flight.getDepartureDate()
        );
    }
}
```

## Sample Data

### Major US Airports

| Code | Name | City | ICAO |
|------|------|------|------|
| ATL | Hartsfield Jackson Atlanta International Airport | Atlanta | KATL |
| ORD | Chicago O'Hare International Airport | Chicago | KORD |
| DFW | Dallas Fort Worth International Airport | Dallas-Fort Worth | KDFW |
| LAX | Los Angeles International Airport | Los Angeles | KLAX |
| JFK | John F Kennedy International Airport | New York | KJFK |
| LGA | La Guardia Airport | New York | KLGA |
| CVG | Cincinnati Northern Kentucky International Airport | Cincinnati | KCVG |

### Coverage Statistics

From the loaded data:
- **Total airports:** 6,033
- **US airports:** 500+
- **Countries covered:** 200+
- **Airports with coordinates:** 6,000+
- **Airports with ICAO codes:** 5,500+

## Updating Data

### Download Latest OpenFlights Airport Data

```bash
curl -o src/main/resources/data/airports.dat \
  https://raw.githubusercontent.com/jpatokal/openflights/master/data/airports.dat
```

### Verify Update

```bash
mvn test -Dtest=AirportCodeMapperTest
```

### Check Statistics

```bash
mvn test-compile
java -cp target/classes:target/test-classes \
  com.lamontd.asqp.examples.AirportDataExplorer
```

## Performance

- **Load time:** ~200ms for 6,033 airports
- **Memory:** ~2MB for airport data
- **Lookup:** O(1) HashMap lookup by IATA code
- **Filtering:** Efficient stream operations
- **Singleton:** Single instance shared across application

## Common Use Cases

### 1. Display Flight Route with Cities

```java
String route = String.format("%s -> %s",
    airportMapper.getAirportCity(flight.getOrigin()),
    airportMapper.getAirportCity(flight.getDestination())
);
// "Cincinnati -> New York"
```

### 2. Calculate Flight Distance

```java
AirportInfo origin = airportMapper.getAirportInfo(flight.getOrigin()).get();
AirportInfo dest = airportMapper.getAirportInfo(flight.getDestination()).get();

double distance = calculateDistance(
    origin.getLatitude().get(), origin.getLongitude().get(),
    dest.getLatitude().get(), dest.getLongitude().get()
);
```

### 3. Generate Airport Statistics

```java
Map<String, Long> flightsByCity = records.stream()
    .map(FlightRecord::getOrigin)
    .collect(Collectors.groupingBy(
        code -> airportMapper.getAirportCity(code),
        Collectors.counting()
    ));
```

### 4. Find Hub Airports

```java
Map<String, Long> departuresByAirport = records.stream()
    .collect(Collectors.groupingBy(
        FlightRecord::getOrigin,
        Collectors.counting()
    ));

departuresByAirport.entrySet().stream()
    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
    .limit(10)
    .forEach(entry -> {
        String name = airportMapper.getAirportName(entry.getKey());
        System.out.printf("%s (%s): %d departures%n",
            entry.getKey(), name, entry.getValue());
    });
```

## Testing

All airport functionality is fully tested:

```bash
mvn test -Dtest=AirportCodeMapperTest
```

**Test Coverage:**
- ✅ 12 tests, all passing
- OpenFlights format parsing
- Airport info retrieval
- Display names
- Country/city filtering
- Search functionality
- Null field handling
- Default resource loading
- Singleton behavior

## Exploring the Data

Use the airport explorer to see what's loaded:

```bash
mvn test-compile
java -cp target/classes:target/test-classes \
  com.lamontd.asqp.examples.AirportDataExplorer
```

This shows:
- Total airports by country
- Major US airports
- Sample airport details
- Search examples
- Highest altitude airports

## Future Enhancements

Potential additions:
- Distance calculation utilities
- Timezone conversion helpers
- Airport search by ICAO code
- Regional airport filtering
- Airport categories (hub, regional, international)
- Runway information
- Terminal data
