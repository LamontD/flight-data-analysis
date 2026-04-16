# Logging and Performance Instrumentation Plan

## Context

Add structured logging with SLF4J and performance metrics to track startup performance, particularly:
- File loading times in FlightDataLoader
- Index building times in FlightDataIndex
- Mapper loading times (CarrierCodeMapper, AirportCodeMapper, CountryCodeMapper)

**Current state:**
- 289 System.out/err.println calls in asqp-reader
- 8 System.out/err.println calls in flight-core
- Manual timing with System.currentTimeMillis() in 3 locations
- No structured logging framework

**Why SLF4J:**
- Industry standard logging facade
- Allows runtime logger selection (Logback, Log4j2)
- Zero performance overhead when logging disabled
- MDC support for contextual logging
- Better than java.util.logging (limited features)

## Scope of Changes

### Files Requiring Changes: 10-12 files

**flight-core module:**
1. `flight-core/pom.xml` - Add SLF4J dependency
2. `CarrierCodeMapper.java` - Replace System.err, add load timing
3. `AirportCodeMapper.java` - Replace System.err, add load timing  
4. `CountryCodeMapper.java` - Replace System.err, add load timing

**asqp-reader module:**
5. `asqp-reader/pom.xml` - Add Logback dependency
6. `FlightDataLoader.java` - Replace prints, enhance timing
7. `FlightDataIndex.java` - Replace prints, enhance timing
8. `CsvFlightRecordReader.java` - Replace System.err for validation
9. `App.java` - Replace startup prints
10. `src/main/resources/logback.xml` - NEW: Logging configuration

**Optional (views have UI output, may keep System.out):**
- View classes (8 files) - Decision: Keep System.out for user interaction

### Performance Metrics Pattern

**Create utility class:**
- `flight-core/src/main/java/com/lamontd/travel/flight/util/PerformanceTimer.java`

```java
public class PerformanceTimer implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(PerformanceTimer.class);
    private final String operationName;
    private final long startNanos;
    
    public PerformanceTimer(String operationName) {
        this.operationName = operationName;
        this.startNanos = System.nanoTime();
        logger.debug("Starting: {}", operationName);
    }
    
    @Override
    public void close() {
        long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
        logger.info("{} completed in {} ms", operationName, durationMs);
    }
}
```

**Usage with try-with-resources:**
```java
try (var timer = new PerformanceTimer("Load flight data")) {
    // operation
}
```

## Implementation Plan

### 1. Add Dependencies

**parent pom.xml:**
```xml
<properties>
  <slf4j.version>2.0.9</slf4j.version>
  <logback.version>1.4.11</logback.version>
</properties>

<dependencyManagement>
  <dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-api</artifactId>
    <version>${slf4j.version}</version>
  </dependency>
  <dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-classic</artifactId>
    <version>${logback.version}</version>
  </dependency>
</dependencyManagement>
```

**flight-core/pom.xml:**
```xml
<dependency>
  <groupId>org.slf4j</groupId>
  <artifactId>slf4j-api</artifactId>
</dependency>
```

**asqp-reader/pom.xml:**
```xml
<dependency>
  <groupId>ch.qos.logback</groupId>
  <artifactId>logback-classic</artifactId>
</dependency>
```

### 2. Create PerformanceTimer Utility

**New file: flight-core/src/main/java/com/lamontd/travel/flight/util/PerformanceTimer.java**

### 3. Create Logback Configuration

**New file: asqp-reader/src/main/resources/logback.xml:**
```xml
<configuration>
  <appender name="FILE" class="ch.qos.logback.core.FileAppender">
    <file>flight-analysis-performance.log</file>
    <encoder>
      <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
    </encoder>
  </appender>
  
  <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
    <encoder>
      <pattern>%d{HH:mm:ss.SSS} %-5level %logger{20} - %msg%n</pattern>
    </encoder>
  </appender>

  <logger name="com.lamontd.travel.flight" level="INFO"/>
  <logger name="com.lamontd.travel.flight.util.PerformanceTimer" level="INFO"/>
  
  <root level="WARN">
    <appender-ref ref="FILE"/>
    <appender-ref ref="CONSOLE"/>
  </root>
</configuration>
```

### 4. Migrate FlightDataLoader

**Before:**
```java
System.out.println("Loading file...");
long startTime = System.currentTimeMillis();
// ... load
long loadTime = System.currentTimeMillis() - startTime;
System.out.printf("✓ Successfully loaded %,d records in %,d ms%n", size, loadTime);
```

**After:**
```java
private static final Logger logger = LoggerFactory.getLogger(FlightDataLoader.class);

public List<ASQPFlightRecord> loadFiles(String[] filePaths) {
    try (var timer = new PerformanceTimer("Load " + filePaths.length + " file(s)")) {
        logger.info("Loading {} file(s)", filePaths.length);
        // ... load logic
        logger.info("Successfully loaded {} records from {} file(s)", 
                    allRecords.size(), filePaths.length);
        return allRecords;
    }
}
```

### 5. Migrate FlightDataIndex

**Key changes:**
- Add logger field
- Wrap index building in PerformanceTimer
- Log reference data stats at INFO
- Log individual index sizes at DEBUG

**Before (lines 53-57):**
```java
System.out.printf("\nReference data: %d carriers, %d airports, %d countries%n",
    carrierMapper.size(), airportMapper.size(), countryMapper.size());
System.out.println("Building data indices for efficient querying...");
long startTime = System.currentTimeMillis();
```

**After:**
```java
private static final Logger logger = LoggerFactory.getLogger(FlightDataIndex.class);

logger.info("Reference data loaded: {} carriers, {} airports, {} countries",
    carrierMapper.size(), airportMapper.size(), countryMapper.size());
    
try (var timer = new PerformanceTimer("Build flight data indices")) {
    // build indices
}

logger.info("Indices: {} carriers, {} airports, {} tail numbers, {} flight numbers, {} dates, {} routes",
    byCarrier.size(), airports.size(), byTailNumber.size(), 
    byFlightNumber.size(), byDate.size(), routeDistances.size());
```

### 6. Migrate Mapper Classes

**Pattern for all three mappers (Carrier, Airport, Country):**

```java
private static final Logger logger = LoggerFactory.getLogger(CarrierCodeMapper.class);

public static synchronized CarrierCodeMapper getDefault() {
    if (defaultInstance == null) {
        try (var timer = new PerformanceTimer("Load carrier data")) {
            defaultInstance = new CarrierCodeMapper();
            try {
                defaultInstance.loadFromOpenFlightsResource("/data/airlines.dat");
                logger.info("Loaded {} carriers from OpenFlights data", defaultInstance.size());
            } catch (IOException e) {
                logger.warn("Could not load OpenFlights data: {}", e.getMessage());
            }
        }
    }
    return defaultInstance;
}
```

### 7. Migrate CsvFlightRecordReader

Replace validation error prints:

```java
private static final Logger logger = LoggerFactory.getLogger(CsvFlightRecordReader.class);

// Line 70:
catch (FlightRecordValidationException e) {
    logger.warn("Skipping invalid record {}: {}", recordNumber, e.getMessage());
}
```

### 8. Update CLAUDE.md

Add section after "Eclipse Collections Preference":

```markdown
## Logging and Performance Instrumentation

### Logging Framework

This project uses **SLF4J** with **Logback** for structured logging.

**Guidelines:**
- Use SLF4J for all logging (no System.out/err except in views for user interaction)
- Log performance metrics for startup operations
- Use appropriate log levels: ERROR (failures), WARN (recoverable issues), INFO (key events), DEBUG (detailed flow)

### Logger Declaration

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

private static final Logger logger = LoggerFactory.getLogger(YourClass.class);
```

### Performance Timing

Use `PerformanceTimer` for critical operations:

```java
try (var timer = new PerformanceTimer("Operation description")) {
    // your operation
} // Automatically logs duration on close
```

**Use for:**
- File loading (FlightDataLoader)
- Index building (FlightDataIndex)
- Mapper initialization (CarrierCodeMapper, etc.)
- Route graph construction (RouteGraphService)

### Log Levels

- **ERROR**: Operation failures that prevent functionality
- **WARN**: Recoverable issues (missing reference data, skipped records)
- **INFO**: Key events (files loaded, indices built, performance metrics)
- **DEBUG**: Detailed flow, individual index sizes

### Configuration

Logging configured in `asqp-reader/src/main/resources/logback.xml`:
- Console output: Brief format for user feedback
- File output: `flight-analysis-performance.log` with detailed timestamps
- Performance metrics always logged to file for analysis
```

## Testing Strategy

### Build Verification
```bash
mvn clean install
```

### Run and Check Logs
```bash
java -jar asqp-reader/target/asqp-reader.jar
# Check console output - should see SLF4J messages
# Check flight-analysis-performance.log exists and contains timing data
```

### Verify Performance Metrics
Log file should contain:
```
2026-04-16 10:23:45.123 INFO  PerformanceTimer - Load carrier data completed in 45 ms
2026-04-16 10:23:45.234 INFO  PerformanceTimer - Load 1 file(s) completed in 156 ms
2026-04-16 10:23:45.456 INFO  PerformanceTimer - Build flight data indices completed in 98 ms
```

### Test Suite
All 89 tests should pass - logging doesn't affect functionality.

## Post-Implementation Tasks

### Copy Plan to Project Documentation

After completing the implementation, copy this plan to the project's documentation directory:

```bash
# Ensure docs/plans directory exists
mkdir -p docs/plans

# Copy the plan file
cp ~/.claude/plans/fuzzy-puzzling-pond.md docs/plans/logging-and-performance-instrumentation.md

# Add to git if not already tracked
git add docs/plans/logging-and-performance-instrumentation.md
```

**Note:** Future plans will automatically save to `docs/plans/` due to the `plansDirectory` setting in `.claude/settings.json`.

## Critical Files

- `pom.xml` - Add SLF4J version property
- `flight-core/pom.xml` - Add slf4j-api
- `asqp-reader/pom.xml` - Add logback-classic
- `flight-core/src/main/java/com/lamontd/travel/flight/util/PerformanceTimer.java` - NEW
- `asqp-reader/src/main/resources/logback.xml` - NEW
- `asqp-reader/src/main/java/com/lamontd/travel/flight/asqp/service/FlightDataLoader.java`
- `asqp-reader/src/main/java/com/lamontd/travel/flight/asqp/index/FlightDataIndex.java`
- `flight-core/src/main/java/com/lamontd/travel/flight/mapper/CarrierCodeMapper.java`
- `flight-core/src/main/java/com/lamontd/travel/flight/mapper/AirportCodeMapper.java`
- `flight-core/src/main/java/com/lamontd/travel/flight/mapper/CountryCodeMapper.java`
- `asqp-reader/src/main/java/com/lamontd/travel/flight/asqp/reader/CsvFlightRecordReader.java`
- `CLAUDE.md`
