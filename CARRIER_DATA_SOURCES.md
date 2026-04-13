# Airline Carrier Code Data Sources

## Current Data Source: OpenFlights ⭐

**The application now uses OpenFlights as the default carrier data source.**

- **Status:** ✅ **IN USE** (airlines.dat in resources)
- **Coverage:** 992+ active airlines worldwide
- **Last updated:** Check the airlines.dat file date
- **Update frequency:** Download latest as needed

To update to the latest OpenFlights data:
```bash
curl -o src/main/resources/data/airlines.dat \
  https://raw.githubusercontent.com/jpatokal/openflights/master/data/airlines.dat
```

---

## Alternative Sources

### 1. Bureau of Transportation Statistics (BTS) - **RECOMMENDED**
The most authoritative source for US airline data used in ASQP reporting.

**Website:** https://www.transtats.bts.gov

**Direct Links:**
- **Carrier Lookup Table:** https://www.transtats.bts.gov/Data_Elements.aspx?Data=2
- **Download Data:** https://www.transtats.bts.gov/DL_SelectFields.aspx?gnoyr_VQ=FGJ&QO_fu146_anzr=b0-gvzr
  - Select "Carrier" as the data element
  - Export as CSV

**File Format:**
The BTS provides a "Carriers" lookup table with:
- `Code` - 2-letter carrier code
- `Description` - Full carrier name

### 2. IATA Airline Codes
International Air Transport Association maintains official airline codes.

**Website:** https://www.iata.org/en/publications/directories/code-search/

**Note:** IATA data requires a subscription for bulk access, but individual lookups are free.

### 3. OpenFlights Data - **FREE ALTERNATIVE**
Community-maintained airline database.

**GitHub:** https://github.com/jpatokal/openflights
**Direct CSV:** https://raw.githubusercontent.com/jpatokal/openflights/master/data/airlines.dat

**Format:** 
```
Airline ID,Name,Alias,IATA,ICAO,Callsign,Country,Active
```

## How to Update Carrier Data

### Option 1: Manual Download from BTS
1. Visit https://www.transtats.bts.gov/Data_Elements.aspx?Data=2
2. Find "Unique Carrier Code" section
3. Download the lookup table
4. Convert to CSV format: `code,name,full_name`
5. Save to `src/main/resources/data/carriers.csv`

### Option 2: Use OpenFlights Data
```bash
# Download OpenFlights data
curl -o airlines.dat https://raw.githubusercontent.com/jpatokal/openflights/master/data/airlines.dat

# Convert to our format (requires processing)
# Filter for IATA codes and active airlines
```

### Option 3: Build Your Own
Create a CSV file with the format:
```csv
code,name,full_name
DL,Delta,Delta Air Lines Inc.
AA,American,American Airlines Inc.
```

## Current Data

The included `carriers.csv` contains a sample dataset with major US carriers. For production use, you should:

1. Download official BTS carrier data
2. Merge with your own carrier information if needed
3. Update regularly (quarterly is recommended)

## Using the Data in Code

```java
// Load default carriers from resources
CarrierCodeMapper mapper = CarrierCodeMapper.getDefault();

// Or load from a custom file
CarrierCodeMapper mapper = CarrierCodeMapper.fromFile(Paths.get("my-carriers.csv"));

// Or load from a custom resource
CarrierCodeMapper mapper = CarrierCodeMapper.fromResource("/data/custom-carriers.csv");

// Use the mapper
String name = mapper.getCarrierName("DL"); // "Delta"
String fullName = mapper.getCarrierFullName("DL"); // "Delta Air Lines Inc."
```

## CSV File Format

The carriers.csv file should have three columns:

```csv
code,name,full_name
AA,American,American Airlines Inc.
```

- **code**: 2-letter IATA carrier code (required)
- **name**: Short name for display (required)
- **full_name**: Full legal name (optional, defaults to name)
- Lines starting with `#` are treated as comments
- Empty lines are ignored

## Validation

After updating carrier data, run the tests to ensure proper loading:

```bash
mvn test -Dtest=CarrierCodeMapperTest
```
