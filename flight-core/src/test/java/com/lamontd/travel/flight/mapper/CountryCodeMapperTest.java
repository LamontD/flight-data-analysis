package com.lamontd.travel.flight.mapper;

import com.lamontd.travel.flight.model.CountryInfo;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CountryCodeMapperTest {

    @Test
    void testLoadFromJson() throws IOException {
        String json = """
                [
                  {"id":840,"alpha2":"us","alpha3":"usa","name":"United States"},
                  {"id":124,"alpha2":"ca","alpha3":"can","name":"Canada"},
                  {"id":826,"alpha2":"gb","alpha3":"gbr","name":"United Kingdom"}
                ]
                """;

        CountryCodeMapper mapper = new CountryCodeMapper();
        mapper.loadFromReader(new StringReader(json));

        assertEquals(3, mapper.size());
        assertTrue(mapper.hasAlpha2("us"));
        assertTrue(mapper.hasAlpha2("US"));
        assertTrue(mapper.hasAlpha3("usa"));
        assertTrue(mapper.hasAlpha3("USA"));
    }

    @Test
    void testGetByAlpha2() throws IOException {
        String json = """
                [{"id":840,"alpha2":"us","alpha3":"usa","name":"United States"}]
                """;

        CountryCodeMapper mapper = new CountryCodeMapper();
        mapper.loadFromReader(new StringReader(json));

        // Case-insensitive
        Optional<CountryInfo> us1 = mapper.getByAlpha2("us");
        Optional<CountryInfo> us2 = mapper.getByAlpha2("US");
        Optional<CountryInfo> us3 = mapper.getByAlpha2("Us");

        assertTrue(us1.isPresent());
        assertTrue(us2.isPresent());
        assertTrue(us3.isPresent());

        CountryInfo country = us1.get();
        assertEquals(840, country.getId());
        assertEquals("us", country.getAlpha2());
        assertEquals("US", country.getAlpha2Upper());
        assertEquals("usa", country.getAlpha3());
        assertEquals("USA", country.getAlpha3Upper());
        assertEquals("United States", country.getName());
    }

    @Test
    void testGetByAlpha3() throws IOException {
        String json = """
                [{"id":840,"alpha2":"us","alpha3":"usa","name":"United States"}]
                """;

        CountryCodeMapper mapper = new CountryCodeMapper();
        mapper.loadFromReader(new StringReader(json));

        // Case-insensitive
        Optional<CountryInfo> us1 = mapper.getByAlpha3("usa");
        Optional<CountryInfo> us2 = mapper.getByAlpha3("USA");
        Optional<CountryInfo> us3 = mapper.getByAlpha3("Usa");

        assertTrue(us1.isPresent());
        assertTrue(us2.isPresent());
        assertTrue(us3.isPresent());
        assertEquals("United States", us1.get().getName());
    }

    @Test
    void testGetByName() throws IOException {
        String json = """
                [{"id":840,"alpha2":"us","alpha3":"usa","name":"United States"}]
                """;

        CountryCodeMapper mapper = new CountryCodeMapper();
        mapper.loadFromReader(new StringReader(json));

        // Case-insensitive
        Optional<CountryInfo> us1 = mapper.getByName("United States");
        Optional<CountryInfo> us2 = mapper.getByName("united states");
        Optional<CountryInfo> us3 = mapper.getByName("UNITED STATES");

        assertTrue(us1.isPresent());
        assertTrue(us2.isPresent());
        assertTrue(us3.isPresent());
        assertEquals("us", us1.get().getAlpha2());
    }

    @Test
    void testGetById() throws IOException {
        String json = """
                [{"id":840,"alpha2":"us","alpha3":"usa","name":"United States"}]
                """;

        CountryCodeMapper mapper = new CountryCodeMapper();
        mapper.loadFromReader(new StringReader(json));

        Optional<CountryInfo> us = mapper.getById(840);
        assertTrue(us.isPresent());
        assertEquals("United States", us.get().getName());

        Optional<CountryInfo> notFound = mapper.getById(999);
        assertFalse(notFound.isPresent());
    }

    @Test
    void testGetCountryName() throws IOException {
        String json = """
                [{"id":840,"alpha2":"us","alpha3":"usa","name":"United States"}]
                """;

        CountryCodeMapper mapper = new CountryCodeMapper();
        mapper.loadFromReader(new StringReader(json));

        assertEquals("United States", mapper.getCountryName("us"));
        assertEquals("United States", mapper.getCountryName("US"));
        assertEquals("UNKNOWN", mapper.getCountryName("UNKNOWN"));
    }

    @Test
    void testAlpha2ToAlpha3() throws IOException {
        String json = """
                [{"id":840,"alpha2":"us","alpha3":"usa","name":"United States"}]
                """;

        CountryCodeMapper mapper = new CountryCodeMapper();
        mapper.loadFromReader(new StringReader(json));

        Optional<String> alpha3 = mapper.alpha2ToAlpha3("us");
        assertTrue(alpha3.isPresent());
        assertEquals("USA", alpha3.get());

        Optional<String> notFound = mapper.alpha2ToAlpha3("zz");
        assertFalse(notFound.isPresent());
    }

    @Test
    void testAlpha3ToAlpha2() throws IOException {
        String json = """
                [{"id":840,"alpha2":"us","alpha3":"usa","name":"United States"}]
                """;

        CountryCodeMapper mapper = new CountryCodeMapper();
        mapper.loadFromReader(new StringReader(json));

        Optional<String> alpha2 = mapper.alpha3ToAlpha2("usa");
        assertTrue(alpha2.isPresent());
        assertEquals("US", alpha2.get());

        Optional<String> notFound = mapper.alpha3ToAlpha2("zzz");
        assertFalse(notFound.isPresent());
    }

    @Test
    void testSearchByName() throws IOException {
        String json = """
                [
                  {"id":840,"alpha2":"us","alpha3":"usa","name":"United States"},
                  {"id":826,"alpha2":"gb","alpha3":"gbr","name":"United Kingdom"},
                  {"id":784,"alpha2":"ae","alpha3":"are","name":"United Arab Emirates"}
                ]
                """;

        CountryCodeMapper mapper = new CountryCodeMapper();
        mapper.loadFromReader(new StringReader(json));

        List<CountryInfo> united = mapper.searchByName("united");
        assertEquals(3, united.size());

        List<CountryInfo> kingdom = mapper.searchByName("Kingdom");
        assertEquals(1, kingdom.size());
        assertEquals("United Kingdom", kingdom.get(0).getName());

        List<CountryInfo> states = mapper.searchByName("States");
        assertEquals(1, states.size());
        assertEquals("United States", states.get(0).getName());
    }

    @Test
    void testHasCountry() throws IOException {
        String json = """
                [{"id":840,"alpha2":"us","alpha3":"usa","name":"United States"}]
                """;

        CountryCodeMapper mapper = new CountryCodeMapper();
        mapper.loadFromReader(new StringReader(json));

        assertTrue(mapper.hasCountry("us"));
        assertTrue(mapper.hasCountry("US"));
        assertTrue(mapper.hasCountry("usa"));
        assertTrue(mapper.hasCountry("USA"));
        assertFalse(mapper.hasCountry("zz"));
        assertFalse(mapper.hasCountry(null));
    }

    @Test
    void testLoadFromDefaultResource() {
        CountryCodeMapper mapper = CountryCodeMapper.getDefault();

        assertTrue(mapper.size() > 0, "Should have loaded countries from default resource");

        // Test some known countries
        assertTrue(mapper.hasAlpha2("US"), "Should have United States");
        assertTrue(mapper.hasAlpha2("CA"), "Should have Canada");
        assertTrue(mapper.hasAlpha2("GB"), "Should have United Kingdom");
        assertTrue(mapper.hasAlpha2("FR"), "Should have France");
        assertTrue(mapper.hasAlpha2("DE"), "Should have Germany");

        // Verify data quality
        CountryInfo us = mapper.getByAlpha2("US").get();
        assertEquals("United States of America", us.getName());
        assertEquals("USA", us.getAlpha3Upper());

        // Test conversions
        assertEquals("USA", mapper.alpha2ToAlpha3("US").get());
        assertEquals("US", mapper.alpha3ToAlpha2("USA").get());
    }

    @Test
    void testGetAllCodes() throws IOException {
        String json = """
                [
                  {"id":840,"alpha2":"us","alpha3":"usa","name":"United States"},
                  {"id":124,"alpha2":"ca","alpha3":"can","name":"Canada"}
                ]
                """;

        CountryCodeMapper mapper = new CountryCodeMapper();
        mapper.loadFromReader(new StringReader(json));

        var alpha2Codes = mapper.getAllAlpha2Codes();
        assertEquals(2, alpha2Codes.size());
        assertTrue(alpha2Codes.contains("US"));
        assertTrue(alpha2Codes.contains("CA"));

        var alpha3Codes = mapper.getAllAlpha3Codes();
        assertEquals(2, alpha3Codes.size());
        assertTrue(alpha3Codes.contains("USA"));
        assertTrue(alpha3Codes.contains("CAN"));
    }

    @Test
    void testAddCountry() {
        CountryCodeMapper mapper = new CountryCodeMapper();

        CountryInfo custom = new CountryInfo(999, "zz", "zzz", "Test Country");
        mapper.addCountry(custom);

        assertTrue(mapper.hasAlpha2("zz"));
        assertTrue(mapper.hasAlpha3("zzz"));
        assertEquals("Test Country", mapper.getCountryName("zz"));
    }

    @Test
    void testSingletonBehavior() {
        CountryCodeMapper mapper1 = CountryCodeMapper.getDefault();
        CountryCodeMapper mapper2 = CountryCodeMapper.getDefault();

        assertSame(mapper1, mapper2, "getDefault() should return the same instance");
    }
}
