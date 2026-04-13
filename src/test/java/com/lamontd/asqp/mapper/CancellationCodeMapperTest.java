package com.lamontd.asqp.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CancellationCodeMapperTest {
    private CancellationCodeMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = CancellationCodeMapper.getDefault();
    }

    @Test
    void testGetDescription_ValidCodes() {
        assertEquals("Carrier Caused", mapper.getDescription("A"));
        assertEquals("Weather", mapper.getDescription("B"));
        assertEquals("National Aviation System", mapper.getDescription("C"));
        assertEquals("Security", mapper.getDescription("D"));
    }

    @Test
    void testGetDescription_CaseInsensitive() {
        assertEquals("Carrier Caused", mapper.getDescription("a"));
        assertEquals("Weather", mapper.getDescription("b"));
        assertEquals("National Aviation System", mapper.getDescription("c"));
        assertEquals("Security", mapper.getDescription("d"));
    }

    @Test
    void testGetDescription_WithWhitespace() {
        assertEquals("Carrier Caused", mapper.getDescription(" A "));
        assertEquals("Weather", mapper.getDescription(" B "));
    }

    @Test
    void testGetDescription_InvalidCode() {
        assertEquals("X", mapper.getDescription("X"));
        assertEquals("Z", mapper.getDescription("Z"));
    }

    @Test
    void testGetDescription_NullOrEmpty() {
        assertEquals("Unknown", mapper.getDescription(null));
        assertEquals("Unknown", mapper.getDescription(""));
        assertEquals("Unknown", mapper.getDescription("   "));
    }

    @Test
    void testGetFullDescription() {
        assertEquals("A - Carrier Caused", mapper.getFullDescription("A"));
        assertEquals("B - Weather", mapper.getFullDescription("B"));
        assertEquals("C - National Aviation System", mapper.getFullDescription("C"));
        assertEquals("D - Security", mapper.getFullDescription("D"));
    }

    @Test
    void testGetFullDescription_InvalidCode() {
        assertEquals("X", mapper.getFullDescription("X"));
    }

    @Test
    void testIsValidCode() {
        assertTrue(mapper.isValidCode("A"));
        assertTrue(mapper.isValidCode("B"));
        assertTrue(mapper.isValidCode("C"));
        assertTrue(mapper.isValidCode("D"));
        assertTrue(mapper.isValidCode("a"));
        assertTrue(mapper.isValidCode("b"));
        assertTrue(mapper.isValidCode(" A "));

        assertFalse(mapper.isValidCode("X"));
        assertFalse(mapper.isValidCode(""));
        assertFalse(mapper.isValidCode(null));
    }

    @Test
    void testGetAllCodes() {
        Map<String, String> allCodes = mapper.getAllCodes();

        assertEquals(4, allCodes.size());
        assertEquals("Carrier Caused", allCodes.get("A"));
        assertEquals("Weather", allCodes.get("B"));
        assertEquals("National Aviation System", allCodes.get("C"));
        assertEquals("Security", allCodes.get("D"));
    }

    @Test
    void testSingletonPattern() {
        CancellationCodeMapper mapper1 = CancellationCodeMapper.getDefault();
        CancellationCodeMapper mapper2 = CancellationCodeMapper.getDefault();

        assertSame(mapper1, mapper2, "Should return the same instance");
    }
}
