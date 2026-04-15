package com.lamontd.travel.flight.mapper;

import com.lamontd.travel.flight.model.CarrierInfo;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CarrierCodeMapperTest {

    @Test
    void testLoadFromReader() throws IOException {
        String csv = """
                code,name,full_name
                DL,Delta,Delta Air Lines Inc.
                AA,American,American Airlines Inc.
                """;

        CarrierCodeMapper mapper = new CarrierCodeMapper();
        mapper.loadFromReader(new StringReader(csv));

        assertEquals(2, mapper.size());
        assertTrue(mapper.hasCarrier("DL"));
        assertTrue(mapper.hasCarrier("AA"));
        assertFalse(mapper.hasCarrier("UA"));
    }

    @Test
    void testGetCarrierInfo() throws IOException {
        String csv = """
                code,name,full_name
                DL,Delta,Delta Air Lines Inc.
                """;

        CarrierCodeMapper mapper = new CarrierCodeMapper();
        mapper.loadFromReader(new StringReader(csv));

        Optional<CarrierInfo> info = mapper.getCarrierInfo("DL");
        assertTrue(info.isPresent());
        assertEquals("DL", info.get().getCode());
        assertEquals("Delta", info.get().getName());
        assertEquals("Delta Air Lines Inc.", info.get().getFullName());
    }

    @Test
    void testGetCarrierName() throws IOException {
        String csv = """
                code,name,full_name
                DL,Delta,Delta Air Lines Inc.
                """;

        CarrierCodeMapper mapper = new CarrierCodeMapper();
        mapper.loadFromReader(new StringReader(csv));

        assertEquals("Delta", mapper.getCarrierName("DL"));
        assertEquals("UNKNOWN", mapper.getCarrierName("UNKNOWN"));
    }

    @Test
    void testGetCarrierFullName() throws IOException {
        String csv = """
                code,name,full_name
                DL,Delta,Delta Air Lines Inc.
                """;

        CarrierCodeMapper mapper = new CarrierCodeMapper();
        mapper.loadFromReader(new StringReader(csv));

        assertEquals("Delta Air Lines Inc.", mapper.getCarrierFullName("DL"));
        assertEquals("UNKNOWN", mapper.getCarrierFullName("UNKNOWN"));
    }

    @Test
    void testAddCarrier() {
        CarrierCodeMapper mapper = new CarrierCodeMapper();
        mapper.addCarrier("DL", "Delta", "Delta Air Lines Inc.");

        assertTrue(mapper.hasCarrier("DL"));
        assertEquals("Delta", mapper.getCarrierName("DL"));
        assertEquals("Delta Air Lines Inc.", mapper.getCarrierFullName("DL"));
    }

    @Test
    void testAddCarrierShortForm() {
        CarrierCodeMapper mapper = new CarrierCodeMapper();
        mapper.addCarrier("DL", "Delta");

        assertTrue(mapper.hasCarrier("DL"));
        assertEquals("Delta", mapper.getCarrierName("DL"));
        assertEquals("Delta", mapper.getCarrierFullName("DL"));
    }

    @Test
    void testLoadWithComments() throws IOException {
        String csv = """
                # This is a comment
                code,name,full_name
                # Another comment
                DL,Delta,Delta Air Lines Inc.

                AA,American,American Airlines Inc.
                """;

        CarrierCodeMapper mapper = new CarrierCodeMapper();
        mapper.loadFromReader(new StringReader(csv));

        assertEquals(2, mapper.size());
    }

    @Test
    void testLoadWithMissingFullName() throws IOException {
        String csv = """
                code,name,full_name
                DL,Delta,
                AA,American,American Airlines Inc.
                """;

        CarrierCodeMapper mapper = new CarrierCodeMapper();
        mapper.loadFromReader(new StringReader(csv));

        assertEquals("Delta", mapper.getCarrierFullName("DL"));
        assertEquals("American Airlines Inc.", mapper.getCarrierFullName("AA"));
    }

    @Test
    void testLoadFromDefaultResource() {
        CarrierCodeMapper mapper = CarrierCodeMapper.getDefault();

        assertTrue(mapper.size() > 0, "Should have loaded carriers from default resource");
        assertTrue(mapper.hasCarrier("DL"), "Should have Delta in default data");
        // OpenFlights data has full name "Delta Air Lines"
        assertEquals("Delta Air Lines", mapper.getCarrierName("DL"));

        // Verify we loaded OpenFlights data (should have many carriers)
        assertTrue(mapper.size() > 100, "Should have loaded many carriers from OpenFlights");

        // Test some other major carriers
        assertTrue(mapper.hasCarrier("AA"), "Should have American Airlines");
        assertTrue(mapper.hasCarrier("UA"), "Should have United Airlines");
        assertTrue(mapper.hasCarrier("WN"), "Should have Southwest Airlines");
    }

    @Test
    void testGetAllCodes() throws IOException {
        String csv = """
                code,name,full_name
                DL,Delta,Delta Air Lines Inc.
                AA,American,American Airlines Inc.
                UA,United,United Air Lines Inc.
                """;

        CarrierCodeMapper mapper = new CarrierCodeMapper();
        mapper.loadFromReader(new StringReader(csv));

        var codes = mapper.getAllCodes();
        assertEquals(3, codes.size());
        assertTrue(codes.contains("DL"));
        assertTrue(codes.contains("AA"));
        assertTrue(codes.contains("UA"));
    }

    @Test
    void testGetAllCarriers() throws IOException {
        String csv = """
                code,name,full_name
                DL,Delta,Delta Air Lines Inc.
                AA,American,American Airlines Inc.
                """;

        CarrierCodeMapper mapper = new CarrierCodeMapper();
        mapper.loadFromReader(new StringReader(csv));

        var carriers = mapper.getAllCarriers();
        assertEquals(2, carriers.size());
    }

    @Test
    void testSingletonBehavior() {
        CarrierCodeMapper mapper1 = CarrierCodeMapper.getDefault();
        CarrierCodeMapper mapper2 = CarrierCodeMapper.getDefault();

        assertSame(mapper1, mapper2, "getDefault() should return the same instance");
    }

    @Test
    void testLoadOpenFlightsFormat() throws IOException {
        // OpenFlights format: id,Name,Alias,IATA,ICAO,Callsign,Country,Active
        String openFlightsCsv = """
                2009,"Delta Air Lines",\\N,"DL","DAL","DELTA","United States","Y"
                24,"American Airlines",\\N,"AA","AAL","AMERICAN","United States","Y"
                4547,"Southwest Airlines",\\N,"WN","SWA","SOUTHWEST","United States","Y"
                5209,"United Airlines",\\N,"UA","UAL","UNITED","United States","Y"
                -1,"Unknown",\\N,"-","N/A",\\N,\\N,"Y"
                100,"Inactive Airline",\\N,"IA","IAL","INACTIVE","United States","N"
                101,"No IATA Code",\\N,"","XYZ","NOCODE","United States","Y"
                """;

        CarrierCodeMapper mapper = new CarrierCodeMapper();
        mapper.loadFromOpenFlightsReader(new StringReader(openFlightsCsv));

        // Should have loaded 4 active carriers with valid IATA codes
        assertEquals(4, mapper.size());

        // Check Delta
        assertTrue(mapper.hasCarrier("DL"));
        CarrierInfo delta = mapper.getCarrierInfo("DL").get();
        assertEquals("DL", delta.getCode());
        assertEquals("Delta Air Lines", delta.getName());
        assertEquals("DAL", delta.getIcao().get());
        assertEquals("DELTA", delta.getCallsign().get());
        assertEquals("United States", delta.getCountry().get());
        assertTrue(delta.isActive());

        // Should not load inactive airline
        assertFalse(mapper.hasCarrier("IA"));

        // Should not load airlines without IATA code
        assertFalse(mapper.hasCarrier(""));

        // Should not load airlines with "-" as IATA code
        assertFalse(mapper.hasCarrier("-"));
    }

    @Test
    void testOpenFlightsWithNullFields() throws IOException {
        // Some airlines don't have all fields
        String csv = """
                100,"Test Airline",\\N,"TA",\\N,\\N,\\N,"Y"
                """;

        CarrierCodeMapper mapper = new CarrierCodeMapper();
        mapper.loadFromOpenFlightsReader(new StringReader(csv));

        assertEquals(1, mapper.size());
        CarrierInfo info = mapper.getCarrierInfo("TA").get();
        assertEquals("TA", info.getCode());
        assertEquals("Test Airline", info.getName());
        assertFalse(info.getIcao().isPresent());
        assertFalse(info.getCallsign().isPresent());
        assertFalse(info.getCountry().isPresent());
        assertTrue(info.isActive());
    }
}
