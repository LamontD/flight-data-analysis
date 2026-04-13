package com.lamontd.asqp.mapper;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.lamontd.asqp.model.CountryInfo;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Maps ISO 3166-1 country codes to country information
 */
public class CountryCodeMapper {
    private final Map<String, CountryInfo> byAlpha2;  // Lowercase alpha-2 codes
    private final Map<String, CountryInfo> byAlpha3;  // Lowercase alpha-3 codes
    private final Map<String, CountryInfo> byName;    // Lowercase names
    private final Map<Integer, CountryInfo> byId;     // Numeric IDs
    private final Gson gson;
    private static CountryCodeMapper defaultInstance;

    public CountryCodeMapper() {
        this.byAlpha2 = new HashMap<>();
        this.byAlpha3 = new HashMap<>();
        this.byName = new HashMap<>();
        this.byId = new HashMap<>();
        this.gson = new Gson();
    }

    /**
     * Gets the default singleton instance loaded from the bundled country data
     */
    public static synchronized CountryCodeMapper getDefault() {
        if (defaultInstance == null) {
            defaultInstance = new CountryCodeMapper();
            try {
                defaultInstance.loadFromResource("/data/countries.json");
            } catch (IOException e) {
                System.err.println("Warning: Could not load default country data: " + e.getMessage());
            }
        }
        return defaultInstance;
    }

    /**
     * Creates a new mapper loaded from a JSON file
     */
    public static CountryCodeMapper fromFile(Path filePath) throws IOException {
        CountryCodeMapper mapper = new CountryCodeMapper();
        mapper.loadFromFile(filePath);
        return mapper;
    }

    /**
     * Loads country data from a JSON resource
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
     * Loads country data from a JSON file
     */
    public void loadFromFile(Path filePath) throws IOException {
        try (Reader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            loadFromReader(reader);
        }
    }

    /**
     * Loads country data from a JSON reader
     * Expected format: [{id, alpha2, alpha3, name}, ...]
     */
    public void loadFromReader(Reader reader) throws IOException {
        Type listType = new TypeToken<List<CountryInfo>>(){}.getType();
        List<CountryInfo> countries = gson.fromJson(reader, listType);

        if (countries == null) {
            throw new IOException("Failed to parse countries JSON");
        }

        for (CountryInfo country : countries) {
            byAlpha2.put(country.getAlpha2().toLowerCase(), country);
            byAlpha3.put(country.getAlpha3().toLowerCase(), country);
            byName.put(country.getName().toLowerCase(), country);
            byId.put(country.getId(), country);
        }

        System.out.println("Loaded " + countries.size() + " countries");
    }

    /**
     * Gets country info by alpha-2 code (case-insensitive)
     * Example: getByAlpha2("US") or getByAlpha2("us")
     */
    public Optional<CountryInfo> getByAlpha2(String alpha2) {
        if (alpha2 == null) return Optional.empty();
        return Optional.ofNullable(byAlpha2.get(alpha2.toLowerCase()));
    }

    /**
     * Gets country info by alpha-3 code (case-insensitive)
     * Example: getByAlpha3("USA") or getByAlpha3("usa")
     */
    public Optional<CountryInfo> getByAlpha3(String alpha3) {
        if (alpha3 == null) return Optional.empty();
        return Optional.ofNullable(byAlpha3.get(alpha3.toLowerCase()));
    }

    /**
     * Gets country info by name (case-insensitive, exact match)
     * Example: getByName("United States")
     */
    public Optional<CountryInfo> getByName(String name) {
        if (name == null) return Optional.empty();
        return Optional.ofNullable(byName.get(name.toLowerCase()));
    }

    /**
     * Gets country info by numeric ID
     */
    public Optional<CountryInfo> getById(int id) {
        return Optional.ofNullable(byId.get(id));
    }

    /**
     * Gets country name by alpha-2 code, or returns the code if not found
     */
    public String getCountryName(String alpha2) {
        return getByAlpha2(alpha2)
                .map(CountryInfo::getName)
                .orElse(alpha2);
    }

    /**
     * Converts alpha-2 code to alpha-3 code
     */
    public Optional<String> alpha2ToAlpha3(String alpha2) {
        return getByAlpha2(alpha2)
                .map(CountryInfo::getAlpha3Upper);
    }

    /**
     * Converts alpha-3 code to alpha-2 code
     */
    public Optional<String> alpha3ToAlpha2(String alpha3) {
        return getByAlpha3(alpha3)
                .map(CountryInfo::getAlpha2Upper);
    }

    /**
     * Searches countries by name (case-insensitive, partial match)
     */
    public List<CountryInfo> searchByName(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String search = searchTerm.toLowerCase();
        return byName.values().stream()
                .distinct()
                .filter(c -> c.getName().toLowerCase().contains(search))
                .sorted(Comparator.comparing(CountryInfo::getName))
                .collect(Collectors.toList());
    }

    /**
     * Checks if a country code (alpha-2 or alpha-3) exists
     */
    public boolean hasCountry(String code) {
        if (code == null) return false;
        String lower = code.toLowerCase();
        return byAlpha2.containsKey(lower) || byAlpha3.containsKey(lower);
    }

    /**
     * Checks if an alpha-2 code exists
     */
    public boolean hasAlpha2(String alpha2) {
        if (alpha2 == null) return false;
        return byAlpha2.containsKey(alpha2.toLowerCase());
    }

    /**
     * Checks if an alpha-3 code exists
     */
    public boolean hasAlpha3(String alpha3) {
        if (alpha3 == null) return false;
        return byAlpha3.containsKey(alpha3.toLowerCase());
    }

    /**
     * Gets the number of countries in the mapper
     */
    public int size() {
        return byId.size();
    }

    /**
     * Gets all country codes (alpha-2)
     */
    public Set<String> getAllAlpha2Codes() {
        return byAlpha2.values().stream()
                .map(CountryInfo::getAlpha2Upper)
                .collect(Collectors.toSet());
    }

    /**
     * Gets all country codes (alpha-3)
     */
    public Set<String> getAllAlpha3Codes() {
        return byAlpha3.values().stream()
                .map(CountryInfo::getAlpha3Upper)
                .collect(Collectors.toSet());
    }

    /**
     * Gets all countries
     */
    public Collection<CountryInfo> getAllCountries() {
        return new ArrayList<>(byId.values());
    }

    /**
     * Adds a country to the mapper
     */
    public void addCountry(CountryInfo country) {
        byAlpha2.put(country.getAlpha2().toLowerCase(), country);
        byAlpha3.put(country.getAlpha3().toLowerCase(), country);
        byName.put(country.getName().toLowerCase(), country);
        byId.put(country.getId(), country);
    }
}
