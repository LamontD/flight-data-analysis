package com.lamontd.asqp.mapper;

import com.lamontd.asqp.model.AirportInfo;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AirportCodeMapperTest {

    @Test
    void testLoadOpenFlightsFormat() throws IOException {
        // OpenFlights format: AirportID,Name,City,Country,IATA,ICAO,Lat,Lon,Alt,TZ,DST,Tz,Type,Source
        String openFlightsCsv = """
                3682,"Hartsfield Jackson Atlanta International Airport","Atlanta","United States","ATL","KATL",33.6367,-84.428101,1026,-5,"A","America/New_York","airport","OurAirports"
                3797,"John F Kennedy International Airport","New York","United States","JFK","KJFK",40.63980103,-73.77890015,13,-5,"A","America/New_York","airport","OurAirports"
                3484,"Los Angeles International Airport","Los Angeles","United States","LAX","KLAX",33.94250107,-118.4079971,125,-8,"A","America/Los_Angeles","airport","OurAirports"
                1,"Airport Without IATA","City","Country","","XXXX",0,0,0,0,"N","Tz","airport","Source"
                2,"Airport With Invalid IATA","City","Country","ABCD","XXXX",0,0,0,0,"N","Tz","airport","Source"
                """;

        AirportCodeMapper mapper = new AirportCodeMapper();
        mapper.loadFromOpenFlightsReader(new StringReader(openFlightsCsv));

        // Should have loaded 3 airports with valid IATA codes
        assertEquals(3, mapper.size());
        assertTrue(mapper.hasAirport("ATL"));
        assertTrue(mapper.hasAirport("JFK"));
        assertTrue(mapper.hasAirport("LAX"));
        assertFalse(mapper.hasAirport(""));
        assertFalse(mapper.hasAirport("ABCD"));
    }

    @Test
    void testGetAirportInfo() throws IOException {
        String csv = """
                3682,"Hartsfield Jackson Atlanta International Airport","Atlanta","United States","ATL","KATL",33.6367,-84.428101,1026,-5,"A","America/New_York","airport","OurAirports"
                """;

        AirportCodeMapper mapper = new AirportCodeMapper();
        mapper.loadFromOpenFlightsReader(new StringReader(csv));

        Optional<AirportInfo> info = mapper.getAirportInfo("ATL");
        assertTrue(info.isPresent());

        AirportInfo atl = info.get();
        assertEquals("ATL", atl.getCode());
        assertEquals("Hartsfield Jackson Atlanta International Airport", atl.getName());
        assertEquals("Atlanta", atl.getCity());
        assertEquals("United States", atl.getCountry().get());
        assertEquals("KATL", atl.getIcao().get());
        assertEquals(33.6367, atl.getLatitude().get(), 0.0001);
        assertEquals(-84.428101, atl.getLongitude().get(), 0.0001);
        assertEquals(1026, atl.getAltitude().get());
        assertEquals(-5.0, atl.getTimezone().get(), 0.001);
        assertEquals("A", atl.getDst().get());
        assertEquals("America/New_York", atl.getTzDatabase().get());
        assertEquals("airport", atl.getType().get());
    }

    @Test
    void testGetAirportName() throws IOException {
        String csv = """
                3682,"Hartsfield Jackson Atlanta International Airport","Atlanta","United States","ATL","KATL",33.6367,-84.428101,1026,-5,"A","America/New_York","airport","OurAirports"
                """;

        AirportCodeMapper mapper = new AirportCodeMapper();
        mapper.loadFromOpenFlightsReader(new StringReader(csv));

        assertEquals("Hartsfield Jackson Atlanta International Airport", mapper.getAirportName("ATL"));
        assertEquals("UNKNOWN", mapper.getAirportName("UNKNOWN"));
    }

    @Test
    void testGetAirportCity() throws IOException {
        String csv = """
                3682,"Hartsfield Jackson Atlanta International Airport","Atlanta","United States","ATL","KATL",33.6367,-84.428101,1026,-5,"A","America/New_York","airport","OurAirports"
                """;

        AirportCodeMapper mapper = new AirportCodeMapper();
        mapper.loadFromOpenFlightsReader(new StringReader(csv));

        assertEquals("Atlanta", mapper.getAirportCity("ATL"));
        assertEquals("UNKNOWN", mapper.getAirportCity("UNKNOWN"));
    }

    @Test
    void testDisplayNames() throws IOException {
        String csv = """
                3682,"Hartsfield Jackson Atlanta International Airport","Atlanta","United States","ATL","KATL",33.6367,-84.428101,1026,-5,"A","America/New_York","airport","OurAirports"
                """;

        AirportCodeMapper mapper = new AirportCodeMapper();
        mapper.loadFromOpenFlightsReader(new StringReader(csv));

        AirportInfo atl = mapper.getAirportInfo("ATL").get();
        assertEquals("Atlanta (ATL)", atl.getShortDisplayName());
        assertEquals("Hartsfield Jackson Atlanta International Airport (Atlanta, United States)",
                    atl.getFullDisplayName());

        assertEquals("Atlanta (ATL)", mapper.getShortDisplayName("ATL"));
        assertEquals("Hartsfield Jackson Atlanta International Airport (Atlanta, United States)",
                    mapper.getFullDisplayName("ATL"));
    }

    @Test
    void testGetAirportsByCountry() throws IOException {
        String csv = """
                3682,"Hartsfield Jackson Atlanta International Airport","Atlanta","United States","ATL","KATL",33.6367,-84.428101,1026,-5,"A","America/New_York","airport","OurAirports"
                3797,"John F Kennedy International Airport","New York","United States","JFK","KJFK",40.63980103,-73.77890015,13,-5,"A","America/New_York","airport","OurAirports"
                1382,"London Heathrow Airport","London","United Kingdom","LHR","EGLL",51.4706,-0.461941,83,0,"E","Europe/London","airport","OurAirports"
                """;

        AirportCodeMapper mapper = new AirportCodeMapper();
        mapper.loadFromOpenFlightsReader(new StringReader(csv));

        List<AirportInfo> usAirports = mapper.getAirportsByCountry("United States");
        assertEquals(2, usAirports.size());
        assertTrue(usAirports.stream().anyMatch(a -> a.getCode().equals("ATL")));
        assertTrue(usAirports.stream().anyMatch(a -> a.getCode().equals("JFK")));

        List<AirportInfo> ukAirports = mapper.getAirportsByCountry("United Kingdom");
        assertEquals(1, ukAirports.size());
        assertEquals("LHR", ukAirports.get(0).getCode());
    }

    @Test
    void testGetAirportsByCity() throws IOException {
        String csv = """
                3682,"Hartsfield Jackson Atlanta International Airport","Atlanta","United States","ATL","KATL",33.6367,-84.428101,1026,-5,"A","America/New_York","airport","OurAirports"
                3697,"La Guardia Airport","New York","United States","LGA","KLGA",40.77719879,-73.87259674,21,-5,"A","America/New_York","airport","OurAirports"
                3797,"John F Kennedy International Airport","New York","United States","JFK","KJFK",40.63980103,-73.77890015,13,-5,"A","America/New_York","airport","OurAirports"
                """;

        AirportCodeMapper mapper = new AirportCodeMapper();
        mapper.loadFromOpenFlightsReader(new StringReader(csv));

        List<AirportInfo> newYorkAirports = mapper.getAirportsByCity("New York");
        assertEquals(2, newYorkAirports.size());
        assertTrue(newYorkAirports.stream().anyMatch(a -> a.getCode().equals("LGA")));
        assertTrue(newYorkAirports.stream().anyMatch(a -> a.getCode().equals("JFK")));
    }

    @Test
    void testSearchByName() throws IOException {
        String csv = """
                3682,"Hartsfield Jackson Atlanta International Airport","Atlanta","United States","ATL","KATL",33.6367,-84.428101,1026,-5,"A","America/New_York","airport","OurAirports"
                3797,"John F Kennedy International Airport","New York","United States","JFK","KJFK",40.63980103,-73.77890015,13,-5,"A","America/New_York","airport","OurAirports"
                3484,"Los Angeles International Airport","Los Angeles","United States","LAX","KLAX",33.94250107,-118.4079971,125,-8,"A","America/Los_Angeles","airport","OurAirports"
                """;

        AirportCodeMapper mapper = new AirportCodeMapper();
        mapper.loadFromOpenFlightsReader(new StringReader(csv));

        // Search by airport name
        List<AirportInfo> kennedyAirports = mapper.searchByName("Kennedy");
        assertEquals(1, kennedyAirports.size());
        assertEquals("JFK", kennedyAirports.get(0).getCode());

        // Search by city
        List<AirportInfo> atlantaAirports = mapper.searchByName("Atlanta");
        assertEquals(1, atlantaAirports.size());
        assertEquals("ATL", atlantaAirports.get(0).getCode());

        // Search partial match
        List<AirportInfo> internationalAirports = mapper.searchByName("International");
        assertEquals(3, internationalAirports.size());
    }

    @Test
    void testLoadFromDefaultResource() {
        AirportCodeMapper mapper = AirportCodeMapper.getDefault();

        assertTrue(mapper.size() > 0, "Should have loaded airports from default resource");

        // Test some known airports from sample data
        assertTrue(mapper.hasAirport("ATL"), "Should have Atlanta");
        assertTrue(mapper.hasAirport("CVG"), "Should have Cincinnati");
        assertTrue(mapper.hasAirport("LGA"), "Should have La Guardia");
        assertTrue(mapper.hasAirport("JFK"), "Should have JFK");

        // Verify data quality
        AirportInfo cvg = mapper.getAirportInfo("CVG").get();
        assertEquals("Cincinnati", cvg.getCity());
        assertTrue(cvg.getCountry().isPresent());
        assertEquals("United States", cvg.getCountry().get());
    }

    @Test
    void testHandleNullFields() throws IOException {
        String csv = """
                100,"Test Airport","Test City","Test Country","TST",\\N,0,0,0,0,\\N,\\N,"airport","Source"
                """;

        AirportCodeMapper mapper = new AirportCodeMapper();
        mapper.loadFromOpenFlightsReader(new StringReader(csv));

        assertEquals(1, mapper.size());
        AirportInfo info = mapper.getAirportInfo("TST").get();
        assertEquals("TST", info.getCode());
        assertEquals("Test Airport", info.getName());
        assertFalse(info.getIcao().isPresent());
        assertFalse(info.getDst().isPresent());
        assertFalse(info.getTzDatabase().isPresent());
    }

    @Test
    void testSingletonBehavior() {
        AirportCodeMapper mapper1 = AirportCodeMapper.getDefault();
        AirportCodeMapper mapper2 = AirportCodeMapper.getDefault();

        assertSame(mapper1, mapper2, "getDefault() should return the same instance");
    }

    @Test
    void testAddAirport() {
        AirportCodeMapper mapper = new AirportCodeMapper();

        AirportInfo custom = AirportInfo.builder()
                .code("TST")
                .name("Test Airport")
                .city("Test City")
                .country("Test Country")
                .build();

        mapper.addAirport(custom);

        assertTrue(mapper.hasAirport("TST"));
        assertEquals("Test Airport", mapper.getAirportName("TST"));
    }
}
