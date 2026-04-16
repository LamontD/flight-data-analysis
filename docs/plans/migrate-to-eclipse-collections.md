# Eclipse Collections Migration Plan

## ⚠️ MIGRATION FAILED - DO NOT RETRY ⚠️

**Date:** 2026-04-16  
**Status:** COMPLETED AND ROLLED BACK  
**Result:** **35% PERFORMANCE DEGRADATION** - Migration made the code slower, not faster.

---

## Executive Summary

This migration was fully implemented and tested with a 7.7M record dataset. Eclipse Collections resulted in significantly **worse** performance compared to the original Java Collections implementation. The changes were completely reverted.

**Performance Results:**
- **Baseline (Java Collections):** 4,350ms to build indices
- **After EC Migration:** 5,895ms to build indices
- **Performance Change:** +35% slower (1,545ms regression)

**Conclusion:** The original Java Streams-based implementation using `Collectors.groupingBy()` was already well-optimized for this use case. Eclipse Collections added conversion overhead without providing the expected benefits.

---

## Original Context (Why We Tried)

The migration was attempted to improve performance in this flight-data-analysis project (multi-module Maven Java 23 project analyzing ASQP flight records).

**Expected Benefits:**
- Better performance for bulk operations (groupBy, select, collect)
- Memory-efficient primitive collections (ObjectLongMap vs Map<String, Long>)
- Optimized for common patterns like grouping and filtering
- Lazy evaluation with views reduces intermediate allocations
- Immutable collections with structural sharing

**Identified Performance Bottlenecks:**
- FlightDataIndex builds multiple Map<String, List<>> indices using Collectors.groupingBy()
- 153 stream operations across codebase (134 in asqp-reader, 19 in flight-core)
- Defensive copying in mapper methods (getAllCodes(), getAllCarriers())
- Multiple grouping operations in services and views

## Recommended Approach: Targeted Migration

**Migrate performance-critical areas first:**
1. FlightDataIndex (main bottleneck - index building)
2. Mapper classes (defensive copying overhead)
3. FlightScheduleService (heavy grouping operations)
4. Keep views as-is initially (can migrate later)

**Why not full migration:**
- JGraphT requires java.util.Set - conversion needed anyway
- Views do minimal collection work (mostly display)
- Incremental approach reduces risk
- Can measure performance gains per component

## Implementation Plan

### 1. Add Eclipse Collections Dependencies

**File: pom.xml (parent)**
```xml
<properties>
  <eclipse-collections.version>11.1.0</eclipse-collections.version>
</properties>

<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.eclipse.collections</groupId>
      <artifactId>eclipse-collections-api</artifactId>
      <version>${eclipse-collections.version}</version>
    </dependency>
    <dependency>
      <groupId>org.eclipse.collections</groupId>
      <artifactId>eclipse-collections</artifactId>
      <version>${eclipse-collections.version}</version>
    </dependency>
  </dependencies>
</dependencyManagement>
```

**File: flight-core/pom.xml**
Add after existing dependencies:
```xml
<dependency>
  <groupId>org.eclipse.collections</groupId>
  <artifactId>eclipse-collections-api</artifactId>
</dependency>
<dependency>
  <groupId>org.eclipse.collections</groupId>
  <artifactId>eclipse-collections</artifactId>
</dependency>
```

### 2. Migrate FlightDataIndex

**File: asqp-reader/src/main/java/com/lamontd/travel/flight/asqp/index/FlightDataIndex.java**

**Changes:**
- Replace `Map<String, List<ASQPFlightRecord>>` with `ImmutableListMultimap<String, ASQPFlightRecord>`
- Replace `Map<String, Long>` with `ImmutableObjectLongMap<String>`
- Replace `Map<String, Double>` with `ImmutableObjectDoubleMap<String>`
- Use `Lists.immutable.withAll(records).groupBy()` instead of `stream().collect(Collectors.groupingBy())`
- Use `aggregateBy()` for counting operations
- Getters return EC types but convert to java.util at RouteIndex interface boundary

**Key conversions:**
```java
// Before:
this.byCarrier = records.stream()
    .collect(Collectors.groupingBy(ASQPFlightRecord::getCarrierCode));

// After:
this.byCarrier = Lists.immutable.withAll(records)
    .groupBy(ASQPFlightRecord::getCarrierCode);

// Before:
this.carrierCounts = byCarrier.entrySet().stream()
    .collect(Collectors.toMap(Map.Entry::getKey, e -> (long) e.getValue().size()));

// After:
this.carrierCounts = Lists.immutable.withAll(records)
    .aggregateBy(
        ASQPFlightRecord::getCarrierCode,
        () -> 0L,
        (count, r) -> count + 1
    );
```

**RouteIndex interface methods (keep java.util.Set):**
```java
@Override
public Set<String> getOriginAirports() {
    return byOriginAirport.keysView().toSet(); // Convert to java.util.Set
}
```

### 3. Migrate CarrierCodeMapper

**File: flight-core/src/main/java/com/lamontd/travel/flight/mapper/CarrierCodeMapper.java**

**Changes:**
- Change `Map<String, CarrierInfo>` to `MutableMap<String, CarrierInfo>`
- Initialize as `Maps.mutable.empty()`
- Replace defensive copies with views:

```java
// Before:
public Set<String> getAllCodes() {
    return new HashSet<>(carrierMap.keySet());
}

// After:
public Set<String> getAllCodes() {
    return carrierMap.keysView().toSet();
}

// Before:
public Collection<CarrierInfo> getAllCarriers() {
    return new ArrayList<>(carrierMap.values());
}

// After:
public Collection<CarrierInfo> getAllCarriers() {
    return carrierMap.valuesView().toList();
}
```

### 4. Migrate AirportCodeMapper

**File: flight-core/src/main/java/com/lamontd/travel/flight/mapper/AirportCodeMapper.java**

Same pattern as CarrierCodeMapper - use MutableMap and views for defensive copies.

### 5. Migrate FlightScheduleService (Optional but Recommended)

**File: asqp-reader/src/main/java/com/lamontd/travel/flight/asqp/service/FlightScheduleService.java**

Replace stream().collect() patterns with EC equivalents:
- `groupingBy` → `groupBy`
- `Collectors.counting()` → `aggregateBy`
- `filter().toList()` → `select().toList()`

### 6. Update CLAUDE.md

**File: CLAUDE.md**

Add new section after "Code Conventions":

```markdown
## Eclipse Collections Preference

This project uses **Eclipse Collections** instead of standard Java collections for performance-critical code. Eclipse Collections provides better performance for bulk operations, memory-efficient primitive collections, and lazy evaluation.

### When to Use Eclipse Collections

**Always use EC for:**
- Index building (FlightDataIndex)
- Mapper internal storage (CarrierCodeMapper, AirportCodeMapper)
- Bulk operations: grouping, filtering, mapping
- Primitive key/value maps (ObjectLongMap, ObjectDoubleMap)

**Use java.util for:**
- Public API boundaries (return types for external consumption)
- Interoperability with third-party libraries (JGraphT)
- Simple temporary collections with minimal operations

### Common Patterns

#### Creating Collections
```java
// Mutable
MutableList<String> list = Lists.mutable.empty();
MutableSet<String> set = Sets.mutable.with("A", "B", "C");
MutableMap<String, Integer> map = Maps.mutable.empty();

// Immutable
ImmutableList<String> list = Lists.immutable.withAll(javaList);
ImmutableSet<String> set = Sets.immutable.with("A", "B", "C");
```

#### Grouping
```java
// Instead of: Collectors.groupingBy()
ImmutableListMultimap<String, Record> byCarrier = 
    Lists.immutable.withAll(records)
        .groupBy(Record::getCarrierCode);
```

#### Filtering
```java
// Instead of: stream().filter()
ImmutableList<Record> operated = 
    allRecords.select(r -> !r.isCancelled());
```

#### Primitive Maps
```java
// Instead of: Map<String, Long>
ImmutableObjectLongMap<String> counts = 
    Lists.immutable.withAll(records)
        .aggregateBy(
            Record::getCarrierCode,
            () -> 0L,
            (count, r) -> count + 1
        );
```

#### Converting to java.util
```java
// When JGraphT or other libraries need java.util.Set:
Set<String> javaSet = ecSet.castToSet();
// Or: ecSet.toSet()
```

### Eclipse Collections vs Java Streams

| Java Streams | Eclipse Collections |
|--------------|-------------------|
| `filter()` | `select()` / `reject()` |
| `map()` | `collect()` |
| `Collectors.groupingBy()` | `groupBy()` |
| `findFirst()` | `detect()` |
| `allMatch()` | `allSatisfy()` |
| `Collectors.counting()` | `aggregateBy()` |

### Dependencies

Eclipse Collections is managed in parent pom.xml (version 11.1.0) and included in flight-core.
```

Also update the "Code Conventions" section to mention:
```markdown
- **Collections**: Use Eclipse Collections for performance-critical code; use java.util at API boundaries
```

## Testing Strategy

### Unit Tests
- All existing tests should pass without modification (EC collections implement standard interfaces)
- If any tests check specific implementation types (ArrayList, HashMap), update to check interfaces (List, Map)
- Run after each migration step: `cd flight-core && mvn test`

### Integration Test
After all changes:
```bash
cd flight-data-analysis
mvn clean install
java -jar asqp-reader/target/asqp-reader.jar
```

Verify:
- Application builds successfully
- Index building shows same or better performance
- Interactive menu works correctly
- All 89 tests pass

### Performance Validation
Compare index building time before/after:
- Look for "Indices built in X ms" console output
- Expect 10-30% improvement for 500-record dataset
- Larger datasets should show more significant gains

## Critical Files

- `pom.xml` - Add EC dependency management
- `flight-core/pom.xml` - Add EC dependencies
- `asqp-reader/src/main/java/com/lamontd/travel/flight/asqp/index/FlightDataIndex.java` - Main performance bottleneck
- `flight-core/src/main/java/com/lamontd/travel/flight/mapper/CarrierCodeMapper.java` - Remove defensive copying
- `flight-core/src/main/java/com/lamontd/travel/flight/mapper/AirportCodeMapper.java` - Remove defensive copying
- `asqp-reader/src/main/java/com/lamontd/travel/flight/asqp/service/FlightScheduleService.java` - Optimize grouping
- `CLAUDE.md` - Document preference for future work

## What Was Implemented

All planned changes were successfully implemented:

✅ Added Eclipse Collections 11.1.0 dependencies to parent pom.xml  
✅ Added EC dependencies to flight-core and asqp-reader modules  
✅ Migrated FlightDataIndex to use:
   - `ImmutableList<ASQPFlightRecord>`
   - `ImmutableListMultimap<String, ASQPFlightRecord>` for all group-by indices
   - `ImmutableMap<LocalDate, ImmutableList<ASQPFlightRecord>>` for byDate
   - `ImmutableObjectLongMap<String>` for carrierCounts
   - `ImmutableObjectDoubleMap<String>` for routeDistances

✅ Migrated CarrierCodeMapper to use `MutableMap<String, CarrierInfo>`  
✅ Migrated AirportCodeMapper to use `MutableMap<String, AirportInfo>`  
✅ Updated view classes (CarrierView, DataOverviewView) to use EC APIs:
   - `keyValuesView()` for iteration
   - `collectInt()` for numeric operations
   - `sizeDistinct()` for multimap key counts

✅ All 89 tests passing after migration

## Why It Failed

1. **Conversion Overhead:**
   - Converting between EC's multimap groups and immutable collections added overhead
   - The complex `byDate` construction (multimap → map with immutable lists) was slow
   - `ObjectLongMaps.immutable.from()` with lambda functions was slower than direct grouping

2. **Java Streams Already Optimized:**
   - Java 8+ Streams API with `Collectors.groupingBy()` is highly optimized
   - JVM JIT compiler optimizes stream pipelines very effectively
   - The code was already using parallel streams for file loading

3. **Dataset Characteristics:**
   - Large dataset (7.7M records) highlighted conversion costs
   - Multiple grouping operations magnified the overhead
   - Primitive maps didn't provide enough benefit to offset conversion costs

4. **Implementation Issues:**
   - Initial bug: Used `.size()` instead of `.sizeDistinct()` on multimaps (showed 7.7M carriers instead of 11)
   - Carrier counting used inefficient pattern that scanned all records for each carrier
   - Complex byDate conversion required extra steps vs. direct stream collection

## What Was Rolled Back

All Eclipse Collections changes were reverted while preserving the logging infrastructure:

✅ Removed EC dependencies from all pom.xml files  
✅ Reverted FlightDataIndex back to Java Collections (HashMap, ArrayList, Collectors)  
✅ Reverted mapper classes back to HashMap  
✅ Reverted view classes to standard Java Collections API  
✅ Removed Eclipse Collections section from CLAUDE.md  
✅ Verified all 89 tests still passing  
✅ Confirmed baseline performance restored (4,384ms vs 4,350ms original)

**Kept (from concurrent logging work):**
- SLF4J and Logback dependencies
- PerformanceTimer utility class
- Logger instances and timing code
- logback.xml configuration
- Logging section in CLAUDE.md

## Lessons Learned

1. **Don't assume library claims without testing:**
   - Eclipse Collections markets itself as "faster than Java Collections"
   - Reality depends heavily on use case and data characteristics
   - Measure first, optimize second

2. **Java Streams are already very good:**
   - For bulk operations on large datasets, Java Streams + parallel processing works well
   - Modern JVM optimizations make streams competitive
   - Simple, idiomatic Java code is often fast enough

3. **Conversion costs matter:**
   - Any library requiring conversion at boundaries adds overhead
   - In this case: EC → java.util.Set for RouteIndex interface
   - Multiple conversions in index building added up

4. **Performance testing is essential:**
   - Small datasets (500 records) showed acceptable performance
   - Large dataset (7.7M records) revealed the true cost
   - Always test with production-scale data before committing to a migration

## Recommendation for Future

**DO NOT attempt this migration again.** The current Java Collections implementation is:
- Well-tested (89 tests passing)
- Fast enough (4.3 seconds for 7.7M records)
- Simple and maintainable
- Using standard Java APIs

**If performance becomes an issue in the future:**
1. Profile first to identify actual bottlenecks
2. Consider algorithmic improvements before library swaps
3. Look at parallelization (already implemented for file loading)
4. Consider specialized data structures for specific operations
5. Measure any changes against baseline with production data
