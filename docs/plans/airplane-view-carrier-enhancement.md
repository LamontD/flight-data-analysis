# Airplane View Enhancements & Route Graph Bug Fix

## Context

After implementing the multi-level menu restructure, several issues and improvements have been identified:

1. **Missing Carrier Information**: Airplane views don't show which carrier(s) the aircraft operates for
2. **CRITICAL BUG - Route Graph Completeness**: RouteGraphService creates edges between ALL airports regardless of whether actual flights exist between them

### Priority: Route Graph Bug Fix (CRITICAL)

**Current Behavior**: The RouteGraphService creates a COMPLETE GRAPH where every airport is connected to every other airport. This causes:
- "Reachable Airports" feature shows ALL airports as directly accessible from any airport
- Shortest path calculations don't reflect actual flight routes
- No way to determine which flights connect two airports

**Root Cause**: In `RouteGraphService.buildGraph()`, the code creates edges between all origin and destination airports:

```java
for (String origin : index.getOriginAirports()) {
    for (String dest : index.getDestinationAirports()) {
        if (!origin.equals(dest)) {
            double distance = index.getRouteDistance(origin, dest);
            if (distance > 0 && routeGraph.containsVertex(origin) && routeGraph.containsVertex(dest)) {
                DefaultWeightedEdge edge = routeGraph.addEdge(origin, dest);
                // This creates edges even when NO flights exist!
            }
        }
    }
}
```

**Expected Behavior**: Only create edges for routes that have actual flights. For example, in `sample-data.asqpflightrecord.csv`:
- ✓ LGA → CVG should have an edge (flights exist)
- ✓ LGA → CLT should have an edge (flights exist)  
- ✗ LGA → SHV should NOT have an edge (no flights exist)

**Impact**: This bug affects:
- Route Network Analysis → Find Reachable Airports (shows incorrect results)
- Route Network Analysis → Find Shortest Path (may suggest non-existent routes)
- Future feature: Finding actual flights between airports

---

### Enhancement: Airplane View Carrier Information

Airplane Report views (Overview, Routes, Flight Log) don't display which carrier(s) the aircraft operates for. This is valuable information for understanding aircraft operations and ownership.

## Proposed Changes

### PRIORITY 1: Fix Route Graph to Only Include Actual Flight Routes

#### Problem Analysis

The current implementation creates edges between all airport pairs, resulting in a complete graph. Instead, we need to build the graph from actual flight records.

#### Solution

Modify `RouteGraphService.buildGraph()` to iterate through actual flight records and only add edges for routes that have flights:

**Current (INCORRECT) Implementation:**
```java
private void buildGraph() {
    // Get all unique airports
    Set<String> airports = new HashSet<>();
    airports.addAll(index.getOriginAirports());
    airports.addAll(index.getDestinationAirports());
    
    // Add vertices
    airports.forEach(routeGraph::addVertex);
    
    // Add edges between ALL airports (WRONG!)
    for (String origin : index.getOriginAirports()) {
        for (String dest : index.getDestinationAirports()) {
            if (!origin.equals(dest)) {
                double distance = index.getRouteDistance(origin, dest);
                if (distance > 0 && routeGraph.containsVertex(origin) && routeGraph.containsVertex(dest)) {
                    DefaultWeightedEdge edge = routeGraph.addEdge(origin, dest);
                    if (edge != null) {
                        routeGraph.setEdgeWeight(edge, distance);
                    }
                }
            }
        }
    }
}
```

**Corrected Implementation:**
```java
private void buildGraph() {
    // Get all unique airports from the index
    Set<String> airports = new HashSet<>();
    airports.addAll(index.getOriginAirports());
    airports.addAll(index.getDestinationAirports());
    
    // Add all airports as vertices
    airports.forEach(routeGraph::addVertex);
    
    // Build set of actual routes from flight records
    // Only add edges for routes that have actual flights
    Set<String> actualRoutes = new HashSet<>();
    
    for (String origin : index.getOriginAirports()) {
        // Get all flights from this origin
        // Note: FlightDataIndex (which implements RouteIndex) should provide
        // a way to iterate actual routes. We can use the pre-computed routeDistances
        // OR iterate through origin airports and their destination airports.
        
        // Option: Use the fact that FlightDataIndex has byOriginAirport map
        // and for each origin, we can get the flights and extract unique destinations
    }
    
    // Add edges only for routes that actually exist
    for (String routeKey : index.routeDistances.keySet()) {
        String[] parts = routeKey.split("-");
        if (parts.length == 2) {
            String origin = parts[0];
            String dest = parts[1];
            
            // Verify both airports exist as vertices
            if (routeGraph.containsVertex(origin) && routeGraph.containsVertex(dest)) {
                double distance = index.getRouteDistance(origin, dest);
                if (distance > 0) {
                    DefaultWeightedEdge edge = routeGraph.addEdge(origin, dest);
                    if (edge != null) {
                        routeGraph.setEdgeWeight(edge, distance);
                    }
                }
            }
        }
    }
}
```

**Alternative (Better) Approach**: 
Since `FlightDataIndex` already has `routeDistances` map which contains pre-computed distances for ALL UNIQUE ROUTES from actual flight records, we should use that map's keys to determine which routes exist:

```java
private void buildGraph() {
    // Get all unique airports
    Set<String> airports = new HashSet<>();
    airports.addAll(index.getOriginAirports());
    airports.addAll(index.getDestinationAirports());
    
    // Add all airports as vertices
    airports.forEach(routeGraph::addVertex);
    
    // Add edges ONLY for routes that exist in the flight data
    // routeDistances keys are in format "ORIGIN-DEST"
    for (String routeKey : index.routeDistances.keySet()) {
        String[] parts = routeKey.split("-");
        if (parts.length == 2) {
            String origin = parts[0];
            String dest = parts[1];
            
            if (routeGraph.containsVertex(origin) && routeGraph.containsVertex(dest)) {
                double distance = index.routeDistances.get(routeKey);
                if (distance > 0) {
                    DefaultWeightedEdge edge = routeGraph.addEdge(origin, dest);
                    if (edge != null) {
                        routeGraph.setEdgeWeight(edge, distance);
                    }
                }
            }
        }
    }
}
```

#### Verification with Sample Data

Using `sample-data.asqpflightrecord.csv`, verify:
1. **LGA → CVG**: Should exist (direct flight route)
2. **LGA → CLT**: Should exist (direct flight route)
3. **LGA → SHV**: Should NOT exist (no direct flights)

After fix:
- Reachable airports from LGA should only show airports with actual flight connections
- Multi-hop paths should only use routes with actual flights

### PRIORITY 2: Add Carrier Information to All Airplane Views

#### **Airplane Overview View**
Add carrier information in the header section, showing:
- Primary carrier (most flights)
- If multi-carrier operations exist, show all carriers with flight counts

**Current Output:**
```
Tail Number: N12345
--------------------------------------------------
Operational Summary:
  Total Flights: 145
  Days Active: 30 days
  ...
```

**Proposed Output:**
```
Tail Number: N12345
--------------------------------------------------
Carrier: WN (Southwest Airlines) - 145 flights

Operational Summary:
  Total Flights: 145
  Days Active: 30 days
  ...
```

**For multi-carrier aircraft:**
```
Tail Number: N12345
--------------------------------------------------
Carriers:
  Primary: WN (Southwest Airlines) - 120 flights (82.8%)
  Secondary: DL (Delta Air Lines) - 25 flights (17.2%)

Operational Summary:
  Total Flights: 145
  Days Active: 30 days
  ...
```

#### **Airplane Routes View**
Add carrier information in the header section.

**Current Output:**
```
Tail Number: N12345
Total Active Days: 15
--------------------------------------------------
Daily Route History:
  2025-01-15: ATL -> ORD -> DEN -> ATL (1,234 miles)
  ...
```

**Proposed Output:**
```
Tail Number: N12345
Carrier: WN (Southwest Airlines)
Total Active Days: 15
--------------------------------------------------
Daily Route History:
  2025-01-15: ATL -> ORD -> DEN -> ATL (1,234 miles)
  ...
```

#### **Airplane Flight Log (existing AirplaneView)**
This view already shows carrier codes in the leg details (e.g., `WN5114`), but the header should also display the primary carrier.

**Current Output:**
```
Tail Number: N12345
Total Operated Flights: 145
--------------------------------------------------
```

**Proposed Output:**
```
Tail Number: N12345
Carrier: WN (Southwest Airlines)
Total Operated Flights: 145
--------------------------------------------------
```

### 2. Handle Multi-Carrier Aircraft

Some aircraft may operate for multiple carriers during the dataset period (leasing, transfers, etc.). The implementation should:

1. **Identify primary carrier**: The carrier with the most flights
2. **Display all carriers**: If aircraft flew for multiple carriers, show breakdown
3. **Set threshold**: Only show "Secondary" carriers if they have >5% of total flights

**Example:**
```
Carriers:
  Primary: WN (Southwest Airlines) - 120 flights (82.8%)
  Secondary: DL (Delta Air Lines) - 25 flights (17.2%)
```

### 3. Display Carrier Name

Use `CarrierCodeMapper.getDefault()` to resolve carrier codes to full airline names, following the pattern used in other views:
- Format: `CODE (Full Name)` 
- Example: `WN (Southwest Airlines)`

## Implementation Details

### Files to Modify

#### Priority 1: Route Graph Bug Fix

1. **[RouteGraphService.java](flight-core/src/main/java/com/lamontd/travel/flight/service/RouteGraphService.java)** - CRITICAL
   - Modify `buildGraph()` method
   - Change from creating complete graph to only adding edges for actual routes
   - Use `index.routeDistances.keySet()` to get actual routes

#### Priority 2: Carrier Information

1. **[AirplaneOverviewView.java](asqp-reader/src/main/java/com/lamontd/travel/flight/asqp/view/AirplaneOverviewView.java)**
   - Add carrier analysis after filtering operated flights
   - Display carrier information in header section
   - Handle multi-carrier scenario

2. **[AirplaneRoutesView.java](asqp-reader/src/main/java/com/lamontd/travel/flight/asqp/view/AirplaneRoutesView.java)**
   - Add carrier determination (primary carrier)
   - Display in header after tail number

3. **[AirplaneView.java](asqp-reader/src/main/java/com/lamontd/travel/flight/asqp/view/AirplaneView.java)**
   - Add carrier display in header section
   - Keep existing leg detail format (already shows carrier codes)

### Code Pattern

For each view, add carrier analysis:

```java
// Determine carrier(s) for this tail number
Map<String, Long> carrierCounts = operatedFlights.stream()
    .collect(Collectors.groupingBy(
        ASQPFlightRecord::getCarrierCode,
        Collectors.counting()
    ));

// Display carrier information
if (carrierCounts.size() == 1) {
    // Single carrier - simple display
    String carrierCode = carrierCounts.keySet().iterator().next();
    String carrierName = carrierMapper.getCarrierName(carrierCode);
    System.out.printf("Carrier: %s (%s)%n", carrierCode, carrierName);
} else if (carrierCounts.size() > 1) {
    // Multi-carrier - show breakdown
    System.out.println("Carriers:");
    carrierCounts.entrySet().stream()
        .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
        .forEach(entry -> {
            String code = entry.getKey();
            long count = entry.getValue();
            double percentage = (count * 100.0) / operatedFlights.size();
            String name = carrierMapper.getCarrierName(code);
            String label = (percentage > 50) ? "Primary" : "Secondary";
            System.out.printf("  %s: %s (%s) - %d flight%s (%.1f%%)%n",
                label, code, name, count, count == 1 ? "" : "s", percentage);
        });
}
```

## Edge Cases

1. **No operated flights**: Already handled (views return early)
2. **Single carrier**: Most common case - simple display
3. **Multi-carrier**: Display breakdown with percentages
4. **Unknown carrier**: Use carrier code only if mapper returns null
5. **Small secondary carrier** (<5% of flights): Could optionally suppress

## Testing Checklist

### CRITICAL: Route Graph Bug Fix (Test First!)

1. **Verify graph only contains actual routes**:
   - Load sample-data.asqpflightrecord.csv
   - Check graph edge count matches unique routes in data
   - Should NOT be a complete graph (N * (N-1) edges)

2. **Test Reachable Airports from LGA**:
   - Should show CVG and CLT as reachable (direct or 0 layovers)
   - Should NOT show SHV as direct/0-layover (no direct flight exists)
   - Verify layover counts are realistic (not all airports at 0 layovers)

3. **Test Shortest Path**:
   - LGA → CVG: Should find direct route
   - LGA → SHV: Should find multi-hop route (if reachable) or "no route"
   - Verify all segments in shortest path have actual flights

4. **Verify Network Statistics**:
   - Edge count should match actual unique routes in dataset
   - Top hubs should reflect actual connectivity (not all airports equal)

### Carrier Information Tests

1. **Single-carrier aircraft**: Verify carrier displays correctly in all 3 views
2. **Multi-carrier aircraft**: 
   - Find tail number with multiple carriers in dataset
   - Verify primary/secondary labeling
   - Verify percentages add to 100%
3. **Carrier name resolution**: 
   - Test with known carriers (WN, DL, AA, UA)
   - Test with unknown/invalid carrier code
4. **Consistency**: Verify same carrier info across all 3 airplane views

## Additional Considerations

### Future Enhancements (Not in Scope)
- Carrier transitions over time (show when aircraft switched carriers)
- Carrier-specific statistics per aircraft
- Filter airplane views by carrier

### Related Features
This enhancement improves consistency with:
- Flight View - already shows carrier in header
- Carrier View - shows per-carrier statistics
- Schedule View - shows carrier information

## Implementation Order

1. **FIRST**: Fix RouteGraphService bug (flight-core module)
   - This is a critical bug affecting graph analysis features
   - Must be fixed before carrier enhancements
   - Rebuild flight-core and install to local Maven repo

2. **SECOND**: Add carrier information to airplane views (asqp-reader module)
   - Requires flight-core to be built first
   - Less critical, can be done after graph fix

## Dependencies

- **CarrierCodeMapper**: Already available via `CarrierCodeMapper.getDefault()`
- **FlightDataIndex**: Already provides `getByTailNumber()` with carrier codes
- **RouteIndex interface**: FlightDataIndex implements this, provides `routeDistances` map
- No new dependencies required

## Backwards Compatibility

This is an **additive change only**:
- No breaking changes to existing functionality
- Only adds display fields to existing views
- Data structures unchanged
- API unchanged

## Estimated Impact

- **Lines of code**: ~30-40 lines per view (~100 total)
- **Complexity**: Low - straightforward grouping and display logic
- **Testing**: ~15 minutes manual testing across 3 views
- **Build time**: <5 minutes (compilation only)
