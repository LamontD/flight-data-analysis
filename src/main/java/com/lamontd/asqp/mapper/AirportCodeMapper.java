package com.lamontd.asqp.mapper;

import com.lamontd.asqp.model.AirportInfo;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class AirportCodeMapper {
    private final Map<String, AirportInfo> airportMap;
    private static AirportCodeMapper defaultInstance;

    public AirportCodeMapper() {
        this.airportMap = new HashMap<>();
    }

    public AirportCodeMapper(Map<String, AirportInfo> airportMap) {
        this.airportMap = new HashMap<>(airportMap);
    }

    /**
     * Gets the default singleton instance loaded from the bundled airport data
     */
    public static synchronized AirportCodeMapper getDefault() {
        if (defaultInstance == null) {
            defaultInstance = new AirportCodeMapper();
            try {
                defaultInstance.loadFromOpenFlightsResource("/data/airports.dat");
            } catch (IOException e) {
                System.err.println("Warning: Could not load default airport data: " + e.getMessage());
            }
        }
        return defaultInstance;
    }

    /**
     * Creates a new mapper loaded from OpenFlights airports.dat file
     */
    public static AirportCodeMapper fromOpenFlightsFile(Path filePath) throws IOException {
        AirportCodeMapper mapper = new AirportCodeMapper();
        mapper.loadFromOpenFlightsFile(filePath);
        return mapper;
    }

    /**
     * Loads airport data from OpenFlights airports.dat resource
     */
    public void loadFromOpenFlightsResource(String resourcePath) throws IOException {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                loadFromOpenFlightsReader(reader);
            }
        }
    }

    /**
     * Loads airport data from OpenFlights airports.dat file
     */
    public void loadFromOpenFlightsFile(Path filePath) throws IOException {
        try (Reader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            loadFromOpenFlightsReader(reader);
        }
    }

    /**
     * Loads airport data from OpenFlights format
     * Expected format: AirportID,Name,City,Country,IATA,ICAO,Latitude,Longitude,Altitude,Timezone,DST,Tz,Type,Source
     * No header row, comma-separated, strings in quotes, \N for null
     */
    public void loadFromOpenFlightsReader(Reader reader) throws IOException {
        CSVFormat csvFormat = CSVFormat.DEFAULT
                .builder()
                .setHeader("airport_id", "name", "city", "country", "iata", "icao",
                          "latitude", "longitude", "altitude", "timezone", "dst",
                          "tz_database", "type", "source")
                .setSkipHeaderRecord(false)  // No header in the file
                .setTrim(true)
                .setIgnoreEmptyLines(true)
                .setNullString("\\N")  // OpenFlights uses \N for null
                .build();

        int recordCount = 0;
        int loadedCount = 0;

        try (CSVParser parser = new CSVParser(reader, csvFormat)) {
            for (CSVRecord record : parser) {
                recordCount++;
                try {
                    String iata = record.get("iata");

                    // Skip if no valid IATA code
                    if (iata == null || iata.isEmpty() || "\\N".equals(iata) || iata.length() != 3) {
                        continue;
                    }

                    String name = record.get("name");
                    String city = record.get("city");
                    String country = record.get("country");
                    String icao = record.get("icao");
                    String tzDatabase = record.get("tz_database");
                    String dst = record.get("dst");
                    String type = record.get("type");

                    if (name != null && !name.isEmpty() && city != null && !city.isEmpty()) {
                        AirportInfo.Builder builder = AirportInfo.builder()
                                .code(iata)
                                .name(name)
                                .city(city)
                                .country(country != null && !country.isEmpty() ? country : null)
                                .icao(icao != null && !icao.isEmpty() && !"\\N".equals(icao) ? icao : null)
                                .tzDatabase(tzDatabase != null && !tzDatabase.isEmpty() ? tzDatabase : null)
                                .dst(dst != null && !dst.isEmpty() ? dst : null)
                                .type(type != null && !type.isEmpty() ? type : null);

                        // Parse numeric fields
                        try {
                            String latStr = record.get("latitude");
                            if (latStr != null && !latStr.isEmpty() && !"\\N".equals(latStr)) {
                                builder.latitude(Double.parseDouble(latStr));
                            }
                        } catch (NumberFormatException e) {
                            // Skip invalid latitude
                        }

                        try {
                            String lonStr = record.get("longitude");
                            if (lonStr != null && !lonStr.isEmpty() && !"\\N".equals(lonStr)) {
                                builder.longitude(Double.parseDouble(lonStr));
                            }
                        } catch (NumberFormatException e) {
                            // Skip invalid longitude
                        }

                        try {
                            String altStr = record.get("altitude");
                            if (altStr != null && !altStr.isEmpty() && !"\\N".equals(altStr)) {
                                builder.altitude(Integer.parseInt(altStr));
                            }
                        } catch (NumberFormatException e) {
                            // Skip invalid altitude
                        }

                        try {
                            String tzStr = record.get("timezone");
                            if (tzStr != null && !tzStr.isEmpty() && !"\\N".equals(tzStr)) {
                                builder.timezone(Double.parseDouble(tzStr));
                            }
                        } catch (NumberFormatException e) {
                            // Skip invalid timezone
                        }

                        AirportInfo info = builder.build();
                        airportMap.put(iata, info);
                        loadedCount++;
                    }
                } catch (Exception e) {
                    // Skip invalid records
                    System.err.println("Skipping invalid OpenFlights airport record at line " +
                                     recordCount + ": " + e.getMessage());
                }
            }
        }

        System.out.println("Loaded " + loadedCount + " airports from " + recordCount + " records");
    }

    /**
     * Adds an airport to the mapper
     */
    public void addAirport(AirportInfo airport) {
        airportMap.put(airport.getCode(), airport);
    }

    /**
     * Gets airport info by IATA code
     */
    public Optional<AirportInfo> getAirportInfo(String code) {
        return Optional.ofNullable(airportMap.get(code));
    }

    /**
     * Gets airport name by IATA code, or returns the code if not found
     */
    public String getAirportName(String code) {
        return getAirportInfo(code)
                .map(AirportInfo::getName)
                .orElse(code);
    }

    /**
     * Gets airport city by IATA code, or returns the code if not found
     */
    public String getAirportCity(String code) {
        return getAirportInfo(code)
                .map(AirportInfo::getCity)
                .orElse(code);
    }

    /**
     * Gets short display name (City (CODE)), or returns the code if not found
     */
    public String getShortDisplayName(String code) {
        return getAirportInfo(code)
                .map(AirportInfo::getShortDisplayName)
                .orElse(code);
    }

    /**
     * Gets full display name, or returns the code if not found
     */
    public String getFullDisplayName(String code) {
        return getAirportInfo(code)
                .map(AirportInfo::getFullDisplayName)
                .orElse(code);
    }

    /**
     * Checks if an airport code is known
     */
    public boolean hasAirport(String code) {
        return airportMap.containsKey(code);
    }

    /**
     * Gets the number of airports in the mapper
     */
    public int size() {
        return airportMap.size();
    }

    /**
     * Gets all airport codes
     */
    public Set<String> getAllCodes() {
        return new HashSet<>(airportMap.keySet());
    }

    /**
     * Gets all airport info
     */
    public Collection<AirportInfo> getAllAirports() {
        return new ArrayList<>(airportMap.values());
    }

    /**
     * Finds airports by country
     */
    public List<AirportInfo> getAirportsByCountry(String country) {
        return airportMap.values().stream()
                .filter(a -> a.getCountry().isPresent())
                .filter(a -> a.getCountry().get().equals(country))
                .toList();
    }

    /**
     * Finds airports by city
     */
    public List<AirportInfo> getAirportsByCity(String city) {
        return airportMap.values().stream()
                .filter(a -> a.getCity().equalsIgnoreCase(city))
                .toList();
    }

    /**
     * Searches airports by name (case-insensitive, partial match)
     */
    public List<AirportInfo> searchByName(String searchTerm) {
        String search = searchTerm.toLowerCase();
        return airportMap.values().stream()
                .filter(a -> a.getName().toLowerCase().contains(search) ||
                            a.getCity().toLowerCase().contains(search))
                .toList();
    }
}
