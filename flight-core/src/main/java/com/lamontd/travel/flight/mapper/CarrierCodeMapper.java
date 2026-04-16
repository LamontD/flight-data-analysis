package com.lamontd.travel.flight.mapper;

import com.lamontd.travel.flight.model.CarrierInfo;
import com.lamontd.travel.flight.util.PerformanceTimer;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class CarrierCodeMapper {
    private static final Logger logger = LoggerFactory.getLogger(CarrierCodeMapper.class);
    private final Map<String, CarrierInfo> carrierMap;
    private static CarrierCodeMapper defaultInstance;

    public CarrierCodeMapper() {
        this.carrierMap = new HashMap<>();
    }

    public CarrierCodeMapper(Map<String, CarrierInfo> carrierMap) {
        this.carrierMap = new HashMap<>(carrierMap);
    }

    /**
     * Gets the default singleton instance loaded from the bundled carrier data
     */
    public static synchronized CarrierCodeMapper getDefault() {
        if (defaultInstance == null) {
            try (var timer = new PerformanceTimer("Load carrier data")) {
                defaultInstance = new CarrierCodeMapper();
                try {
                    // Try OpenFlights data first
                    defaultInstance.loadFromOpenFlightsResource("/data/airlines.dat");
                    logger.info("Loaded {} carriers from OpenFlights data", defaultInstance.size());
                } catch (IOException e) {
                    logger.warn("Could not load OpenFlights data: {}", e.getMessage());
                    // Fall back to simple CSV if available
                    try {
                        defaultInstance.loadFromResource("/data/carriers.csv");
                        logger.info("Loaded {} carriers from fallback data", defaultInstance.size());
                    } catch (IOException e2) {
                        logger.warn("Could not load default carrier data: {}", e2.getMessage());
                    }
                }
            }
        }
        return defaultInstance;
    }

    /**
     * Creates a new mapper loaded from a resource file
     */
    public static CarrierCodeMapper fromResource(String resourcePath) throws IOException {
        CarrierCodeMapper mapper = new CarrierCodeMapper();
        mapper.loadFromResource(resourcePath);
        return mapper;
    }

    /**
     * Creates a new mapper loaded from a file
     */
    public static CarrierCodeMapper fromFile(Path filePath) throws IOException {
        CarrierCodeMapper mapper = new CarrierCodeMapper();
        mapper.loadFromFile(filePath);
        return mapper;
    }

    /**
     * Loads carrier data from a resource file in the classpath
     */
    public void loadFromResource(String resourcePath) throws IOException {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                loadFromReader(reader);
            }
        }
    }

    /**
     * Loads carrier data from a file
     */
    public void loadFromFile(Path filePath) throws IOException {
        try (Reader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            loadFromReader(reader);
        }
    }

    /**
     * Loads carrier data from a Reader
     * Expected CSV format: code,name,full_name
     */
    public void loadFromReader(Reader reader) throws IOException {
        CSVFormat csvFormat = CSVFormat.DEFAULT
                .builder()
                .setHeader("code", "name", "full_name")
                .setSkipHeaderRecord(true)
                .setTrim(true)
                .setIgnoreEmptyLines(true)
                .setCommentMarker('#')
                .build();

        try (CSVParser parser = new CSVParser(reader, csvFormat)) {
            for (CSVRecord record : parser) {
                String code = record.get("code");
                String name = record.get("name");
                String fullName = record.get("full_name");

                if (code != null && !code.trim().isEmpty()) {
                    CarrierInfo info = new CarrierInfo(
                            code.trim(),
                            name != null ? name.trim() : code.trim(),
                            fullName != null && !fullName.trim().isEmpty() ? fullName.trim() : name.trim()
                    );
                    carrierMap.put(info.getCode(), info);
                }
            }
        }
    }

    /**
     * Loads carrier data from OpenFlights airlines.dat resource
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
     * Loads carrier data from OpenFlights airlines.dat file
     */
    public void loadFromOpenFlightsFile(Path filePath) throws IOException {
        try (Reader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            loadFromOpenFlightsReader(reader);
        }
    }

    /**
     * Loads carrier data from OpenFlights format
     * Expected format: id,Name,Alias,IATA,ICAO,Callsign,Country,Active
     * No header row, comma-separated, strings in quotes, \N for null
     */
    public void loadFromOpenFlightsReader(Reader reader) throws IOException {
        CSVFormat csvFormat = CSVFormat.DEFAULT
                .builder()
                .setHeader("id", "name", "alias", "iata", "icao", "callsign", "country", "active")
                .setSkipHeaderRecord(false)  // No header in the file
                .setTrim(true)
                .setIgnoreEmptyLines(true)
                .setNullString("\\N")  // OpenFlights uses \N for null
                .build();

        try (CSVParser parser = new CSVParser(reader, csvFormat)) {
            for (CSVRecord record : parser) {
                try {
                    String iata = record.get("iata");
                    String active = record.get("active");

                    // Skip if not active or no valid IATA code
                    if (!"Y".equals(active)) {
                        continue;
                    }

                    if (iata == null || iata.isEmpty() || "-".equals(iata) || iata.length() != 2) {
                        continue;
                    }

                    String name = record.get("name");
                    String icao = record.get("icao");
                    String callsign = record.get("callsign");
                    String country = record.get("country");

                    if (name != null && !name.isEmpty()) {
                        CarrierInfo info = CarrierInfo.builder()
                                .code(iata)
                                .name(name)
                                .icao(icao != null && !icao.isEmpty() ? icao : null)
                                .callsign(callsign != null && !callsign.isEmpty() ? callsign : null)
                                .country(country != null && !country.isEmpty() ? country : null)
                                .active(true)
                                .build();
                        carrierMap.put(iata, info);
                    }
                } catch (Exception e) {
                    // Skip invalid records
                    System.err.println("Skipping invalid OpenFlights record: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Adds a carrier to the mapper
     */
    public void addCarrier(String code, String name, String fullName) {
        carrierMap.put(code, new CarrierInfo(code, name, fullName));
    }

    /**
     * Adds a carrier to the mapper (name and fullName are the same)
     */
    public void addCarrier(String code, String name) {
        addCarrier(code, name, name);
    }

    /**
     * Gets carrier info by code
     */
    public Optional<CarrierInfo> getCarrierInfo(String code) {
        return Optional.ofNullable(carrierMap.get(code));
    }

    /**
     * Gets carrier name by code, or returns the code if not found
     */
    public String getCarrierName(String code) {
        return getCarrierInfo(code)
                .map(CarrierInfo::getName)
                .orElse(code);
    }

    /**
     * Gets carrier full name by code, or returns the code if not found
     */
    public String getCarrierFullName(String code) {
        return getCarrierInfo(code)
                .map(CarrierInfo::getFullName)
                .orElse(code);
    }

    /**
     * Checks if a carrier code is known
     */
    public boolean hasCarrier(String code) {
        return carrierMap.containsKey(code);
    }

    /**
     * Gets the number of carriers in the mapper
     */
    public int size() {
        return carrierMap.size();
    }

    /**
     * Gets all carrier codes
     */
    public java.util.Set<String> getAllCodes() {
        return new java.util.HashSet<>(carrierMap.keySet());
    }

    /**
     * Gets all carrier info
     */
    public java.util.Collection<CarrierInfo> getAllCarriers() {
        return new java.util.ArrayList<>(carrierMap.values());
    }
}
