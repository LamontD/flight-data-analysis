# Data Quality Validation

## Overview

The ASQP Reader now includes automatic data quality validation that checks for common data issues when processing flight records.

## Validation Checks

### 1. Missing Carrier Codes

**What it checks:** Verifies that all carrier codes in the flight data exist in the carrier database.

**Purpose:** Identifies unknown airlines that may need to be added to the carrier database.

**Example Output:**
```
✓ All carrier codes found in database
```

Or if issues are found:
```
WARNING: Found 2 unknown carrier code(s):
  - XX (not in carrier database)
  - YY (not in carrier database)
```

**How to fix:**
- Add missing carriers to the carrier database
- Or verify the carrier codes are correct in the source data

### 2. Missing Airport Codes

**What it checks:** Verifies that all airport codes (origin and destination) exist in the airport database.

**Purpose:** Identifies unknown airports that may need to be added to the airport database.

**Example Output:**
```
✓ All airport codes found in database
```

Or if issues are found:
```
WARNING: Found 3 unknown airport code(s):
  - ABC (not in airport database)
  - DEF (not in airport database)
  - GHI (not in airport database)
```

**How to fix:**
- Add missing airports to the airport database
- Or verify the airport codes are correct in the source data
- Check for typos in airport codes

### 3. Invalid Flight Times (Arrival Before Departure)

**What it checks:** Detects flights where the gate arrival time is before the gate departure time.

**Purpose:** Identifies potential data quality issues or red-eye flights that cross midnight.

**Example Output:**
```
✓ All flight times appear valid
```

Or if issues are found:
```
WARNING: Found 2 flight(s) with arrival before departure:
  - DL5035 on 2025-01-11: Departed 23:33, Arrived 01:31
  - DL5036 on 2025-01-09: Departed 23:17, Arrived 01:10
  (Note: These may be flights crossing midnight)
```

**Interpretation:**
- **Red-eye flights:** Flights departing late at night and arriving early the next morning (crossing midnight) will trigger this warning. This is normal for overnight flights.
- **Data errors:** If the flight is not an overnight flight, this may indicate incorrect time data.

**Examples:**
- `Departed 23:33, Arrived 01:31` - **Normal:** Red-eye flight, crossed midnight
- `Departed 14:30, Arrived 12:15` - **Error:** Likely a data entry mistake

### 4. Invalid Wheels Times (Wheels Down Before Wheels Up)

**What it checks:** Detects flights where wheels down time is before wheels up time.

**Purpose:** Identifies potential data quality issues or flights crossing midnight.

**Example Output:**
```
✓ No wheel time issues detected
```

Or if issues are found:
```
WARNING: Found 2 flight(s) with wheels down before wheels up:
  - DL5035 on 2025-01-11: Wheels up 23:51, Wheels down 01:28
  - DL5036 on 2025-01-09: Wheels up 23:34, Wheels down 01:01
```

**Interpretation:**
- Similar to arrival times, this typically indicates flights crossing midnight
- Could also indicate data entry errors

## Full Example Output

```
=== Data Quality Checks ===
✓ All carrier codes found in database
✓ All airport codes found in database
WARNING: Found 2 flight(s) with arrival before departure:
  - DL5035 on 2025-01-11: Departed 23:33, Arrived 01:31
  - DL5036 on 2025-01-09: Departed 23:17, Arrived 01:10
  (Note: These may be flights crossing midnight)
WARNING: Found 2 flight(s) with wheels down before wheels up:
  - DL5035 on 2025-01-11: Wheels up 23:51, Wheels down 01:28
  - DL5036 on 2025-01-09: Wheels up 23:34, Wheels down 01:01
```

## Implementation Details

### Check Execution

Validation checks run automatically after flight records are loaded:

```java
// Validation runs here
List<FlightRecord> records = reader.readFromFile(path);

// Data Quality Checks execute automatically
// Results printed to console
```

### Performance

- **Impact:** Minimal (~10-50ms for 500 records)
- **Memory:** O(n) where n = number of records
- **When:** After loading, before statistics

### Cancelled Flights

The validation system automatically:
- **Skips** cancelled flights when checking times
- **Includes** cancelled flights when checking carrier/airport codes

## Understanding Time Issues

### Why Times May Appear Invalid

**1. Midnight Crossing (Red-eye Flights)**

Most common reason. Example:
```
Flight departs: 11:30 PM (23:30)
Flight arrives: 1:15 AM (01:15) next day
```

The system sees `01:15 < 23:30` and flags it, but this is normal for overnight flights.

**How to identify:**
- Departure time is late evening (after 20:00)
- Arrival time is early morning (before 06:00)
- Flight duration makes sense when accounting for midnight crossing

**2. Timezone Changes**

Flights crossing time zones may have confusing times:
```
Depart LAX: 23:00 PST
Arrive JFK: 07:30 EST (next day)
```

Local times may appear to have issues, but are correct when timezones are considered.

**3. Data Entry Errors**

Actual errors to investigate:
```
Flight departs: 14:30 (2:30 PM)
Flight arrives: 12:15 (12:15 PM)
```

This suggests a data quality issue unless the flight crosses the International Date Line.

### Validation Strategy

**For red-eye flights (likely valid):**
- Departure between 20:00-23:59
- Arrival between 00:00-06:00
- These are usually overnight flights crossing midnight

**For suspicious entries (investigate):**
- Departure during daytime (08:00-19:59)
- Arrival before departure (same time window)
- These may be data errors

## Programmatic Access

### Custom Validation

You can implement your own validation:

```java
List<FlightRecord> records = reader.readFromFile(path);

// Check for missing carriers
Set<String> unknownCarriers = records.stream()
    .map(FlightRecord::getCarrierCode)
    .filter(code -> !carrierMapper.hasCarrier(code))
    .collect(Collectors.toSet());

if (!unknownCarriers.isEmpty()) {
    System.out.println("Unknown carriers: " + unknownCarriers);
}

// Check for invalid times
List<FlightRecord> suspiciousFlights = records.stream()
    .filter(r -> !r.isCancelled())
    .filter(r -> r.getGateDeparture().isPresent() && 
                 r.getGateArrival().isPresent())
    .filter(r -> {
        LocalTime dep = r.getGateDeparture().get();
        LocalTime arr = r.getGateArrival().get();
        // Filter out likely red-eyes
        boolean isLateEvening = dep.isAfter(LocalTime.of(20, 0));
        boolean isEarlyMorning = arr.isBefore(LocalTime.of(6, 0));
        boolean crossesMidnight = arr.isBefore(dep);
        
        // Flag as suspicious if crosses midnight but NOT a red-eye pattern
        return crossesMidnight && !(isLateEvening && isEarlyMorning);
    })
    .toList();
```

## Configuration

### Disable Validation (if needed)

Currently, validation always runs. To disable, you would need to modify `App.java` and comment out the validation section.

### Customize Output

Modify the validation section in `App.java`:
- Change warning format
- Adjust the number of examples shown (currently limited to 5)
- Add or remove specific checks
- Change output verbosity

## Use Cases

### 1. Data Quality Monitoring

Run validation on each data load to monitor data quality over time:
```bash
java -jar asqp-reader.jar data.csv > quality-report.txt
```

Review `quality-report.txt` for warnings.

### 2. Pre-Processing Verification

Validate data before loading into a database:
```bash
java -jar asqp-reader.jar new-data.csv
# Check for warnings before proceeding
```

### 3. Database Maintenance

Identify missing carriers/airports that need to be added:
```
WARNING: Found 3 unknown airport code(s):
  - XYZ (not in airport database)
```

Action: Research XYZ airport and add to `airports.dat`.

### 4. Data Cleaning

Identify records that need correction:
```
WARNING: Flight departs 14:30, arrives 12:15
```

Action: Investigate source data for this specific flight.

## Best Practices

### 1. Review Warnings Regularly

Not all warnings are errors:
- Red-eye flights are normal
- New airports may appear in data before database updates

### 2. Investigate Daytime Anomalies

Focus on flights where:
- Departure is NOT late evening
- Arrival is NOT early morning
- But arrival < departure

These are more likely to be actual errors.

### 3. Update Reference Data

When new carriers or airports appear:
1. Verify they're legitimate
2. Add to appropriate database
3. Re-run validation to confirm

### 4. Document Known Issues

If certain warnings are expected (e.g., specific red-eye routes), document them so they're not repeatedly investigated.

## Troubleshooting

### Too Many Time Warnings

If you're getting many red-eye flight warnings, you can:
1. Accept them as normal (they're informational)
2. Modify the validation to filter out likely red-eyes
3. Enhance validation to check flight duration or timezone data

### False Positives

Time warnings may be false positives for:
- Trans-Pacific flights crossing date line
- Flights in polar regions
- Daylight saving time transitions

Consider the route and date when investigating.

### Missing Data

Some fields may legitimately be missing:
- Cancelled flights have no actual times
- Some flights may not have wheels up/down data
- These are handled automatically by the validation system

## Future Enhancements

Potential improvements to validation:
- **Timezone-aware time checking**
- **Flight duration validation** (compare scheduled vs actual)
- **Route validation** (verify origin/destination pairs make sense)
- **Date validation** (check for reasonable date ranges)
- **Tail number validation** (check aircraft registration format)
- **Statistical outlier detection** (find unusually long/short flights)
- **Historical comparison** (compare against typical patterns)

## Summary

The data quality validation system provides:
- ✅ **Automatic checks** on every data load
- ✅ **Clear warnings** for potential issues
- ✅ **Contextual hints** (e.g., "may be midnight crossing")
- ✅ **Non-intrusive** (doesn't block processing)
- ✅ **Actionable** (identifies specific records)

Use these checks to maintain high data quality and catch issues early!
