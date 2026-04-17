# Multi-Level Menu System Restructure

## Context

The ASQP Flight Data Analysis application currently has a flat menu structure with 9 options in the main MenuController. As the application grows, this flat structure is becoming unwieldy and difficult to navigate. Users need to scan through all options each time, and related functionality is not grouped together logically.

This plan restructures the menu into a hierarchical, multi-level navigation system that groups related functionality together and adds new features for comprehensive data exploration.

## Proposed Menu Structure

```
Main Menu:
├── 1. Data View (submenu)
│   ├── 1. Carrier View
│   └── 2. Airport View
├── 2. Airplane Report (submenu)
│   ├── 1. Airplane Overview (NEW)
│   ├── 2. Airplane Routes (NEW)
│   └── 3. Airplane Flight Log (existing AirplaneView)
├── 3. Flight Report (submenu)
│   ├── 1. Flight Overview (existing FlightView - Route Overview + Performance Summary)
│   └── 2. Flight Details (existing FlightView - Daily Flight Log)
├── 4. Route Network Analysis (submenu)
│   ├── 1. View Network Statistics
│   ├── 2. Find Shortest Path
│   └── 3. Find Reachable Airports (ENHANCED)
├── 5. Schedule Report (submenu)
│   └── 1. Schedule Analysis (existing FlightScheduleView)
├── 6. Filter By Date Range
└── 7. Exit
```

## Critical Files to Modify

### Existing Files to Modify
1. **[MenuController.java](asqp-reader/src/main/java/com/lamontd/travel/flight/asqp/controller/MenuController.java)** - Main controller, restructure to support multi-level menus
2. **[FlightView.java](asqp-reader/src/main/java/com/lamontd/travel/flight/asqp/view/FlightView.java)** - Split into two display modes (overview vs detailed)
3. **[RouteAnalysisView.java](asqp-reader/src/main/java/com/lamontd/travel/flight/asqp/view/RouteAnalysisView.java)** - Already has submenu pattern, enhance reachable airports feature
4. **[RouteGraphService.java](flight-core/src/main/java/com/lamontd/travel/flight/service/RouteGraphService.java)** - Add BFS-based method for finding reachable airports with minimum layover counts

### New Files to Create
5. **DataViewSubmenu.java** - Submenu controller for Data View (Carrier/Airport)
6. **AirplaneReportSubmenu.java** - Submenu controller for Airplane reports
7. **FlightReportSubmenu.java** - Submenu controller for Flight reports
8. **ScheduleReportSubmenu.java** - Submenu controller for Schedule analysis
9. **AirplaneOverviewView.java** - New view: tail number → days active, total miles
10. **AirplaneRoutesView.java** - New view: tail number → daily routes without leg details

## Implementation Details

### 1. MenuController Restructure

The main MenuController will be simplified to show 7 top-level options. Each submenu option will invoke a dedicated submenu controller class that follows a consistent pattern:
- Display submenu header with context
- Show submenu options
- Handle user selection
- Return to main menu

### 2. Submenu Pattern

All submenu controllers will implement a common interface or follow a standard pattern:

```java
public interface SubmenuController {
    void display(FlightDataIndex index, Scanner scanner);
}
```

Each submenu:
- Accepts FlightDataIndex and Scanner
- Displays options with numbered choices
- Includes "Return to Main Menu" option
- Loops until user returns to main menu
- Uses consistent formatting (50-character separator lines)

### 3. New View: AirplaneOverviewView

**Purpose**: High-level summary for a tail number without detailed flight logs

**Output**:
- Tail Number
- Number of days the plane was active (count of unique departure dates)
- Total miles traveled (sum of all route distances for operated flights)
- Date range of operations (first and last flight dates)

**Data Sources**:
- `FlightDataIndex.getByTailNumber(tailNumber)` for flight records
- `FlightDataIndex.getDistance(origin, destination)` for each leg
- Filter out cancelled flights
- Group by departure date to count active days

### 4. New View: AirplaneRoutesView

**Purpose**: Show daily route patterns without detailed leg information

**Output Format**:
```
Tail Number: N12345
Total Active Days: 15

Daily Route History:
  2025-01-15: ATL -> ORD -> DEN -> ATL (1,234 miles)
  2025-01-16: ATL -> LAX -> PHX -> ATL (2,345 miles)
  ...
```

**Differences from AirplaneView**:
- No detailed leg information (flight numbers, times, cities)
- Just date, route chain, and total distance
- More concise for quick route pattern analysis

### 5. FlightView Split

The existing FlightView displays:
1. Route Overview (all unique routes flown)
2. Performance Summary (operational stats, on-time performance)
3. Daily Flight Report (detailed day-by-day breakdown)

**Split into two modes**:
- **Flight Overview Mode**: Shows only Route Overview + Performance Summary
- **Flight Details Mode**: Shows only Daily Flight Report (or all three sections)

Implementation options:
- Option A: Add a `renderMode` parameter to FlightView (`OVERVIEW` or `DETAILS`)
- Option B: Extract methods and call different combinations from submenu
- **Recommended**: Option A for cleaner separation

### 6. Enhanced Reachable Airports Feature

**Current Implementation** (RouteGraphService.getReachableAirports):
- Uses Dijkstra's shortest path to find all reachable airports
- No layover limit
- Doesn't calculate minimum layovers needed

**Enhancement Required**:
Add new method to RouteGraphService:
```java
public Map<String, Integer> getReachableAirportsWithLayoverCount(String origin, int maxLayovers)
```

**Algorithm**: Breadth-First Search (BFS)
- Start from origin airport (0 layovers)
- Explore all directly connected airports (0 layovers to reach)
- Then explore airports reachable from those (1 layover)
- Continue up to maxLayovers
- Track minimum layovers needed for each airport
- Return Map<Airport, MinLayovers>

**Output Format**:
```
AIRPORTS REACHABLE FROM ATL (Atlanta) WITH MAX 2 LAYOVERS

Direct (0 layovers): 150 airports
  ORD (Chicago), DFW (Dallas), LAX (Los Angeles), ...

1 Layover: 75 airports
  BOS (Boston), SEA (Seattle), ...

2 Layovers: 45 airports
  ANC (Anchorage), HNL (Honolulu), ...

Total: 270 airports reachable within 2 layovers
```

### 7. Menu Navigation Flow

**Main Menu** → **Submenu** → **View/Action** → **Back to Submenu** → **Back to Main Menu**

Each level should:
- Show clear breadcrumb or context (e.g., "Data View > Carrier View")
- Have consistent "Return" option (last numbered option)
- Maintain filter state (if date filtering is active)
- Handle invalid input gracefully

### 8. Date Filtering Consistency

Date filtering (option 6) should remain accessible from the main menu and apply globally to all views, maintaining the current behavior where:
- `activeIndex` is filtered
- `originalIndex` is preserved
- Filter status is displayed in menu header

## Testing Strategy

### Unit Tests
1. Test new BFS method in RouteGraphService with known graph topology
2. Test AirplaneOverviewView calculations (days active, total miles)
3. Test FlightView rendering modes

### Integration Tests
1. Navigate through all menu levels
2. Verify "Return to Main Menu" works from all submenus
3. Verify date filtering persists across submenu navigation
4. Test reachable airports with various layover limits (0, 1, 2, 3)

### Manual Testing Checklist
1. **Main Menu Navigation**:
   - Select each top-level option
   - Verify submenu displays correctly
   - Return to main menu from each submenu
   
2. **Airplane Report**:
   - Test Airplane Overview with valid/invalid tail numbers
   - Verify miles calculation matches manual calculation
   - Test Airplane Routes for multi-day operations
   - Compare Flight Log with original AirplaneView
   
3. **Flight Report**:
   - Test Flight Overview shows Route Overview + Performance Summary
   - Test Flight Details shows Daily Flight Log
   - Verify same data as original FlightView
   
4. **Route Network Analysis**:
   - Test Reachable Airports with maxLayovers=0 (direct only)
   - Test with maxLayovers=1 and verify counts
   - Test with maxLayovers=2 for major hub (e.g., ATL)
   - Verify layover counts are accurate
   
5. **Date Filtering Integration**:
   - Apply date filter from main menu
   - Navigate through submenus
   - Verify filtered data is used throughout
   - Clear filter and verify full data restored

## Architecture Considerations

### Reusable Components
- Create `SubmenuController` interface for consistency
- Extract common menu rendering logic to utility methods
- Consider `MenuOption` class for cleaner menu definitions

### Performance
- All new features use existing indexed data structures (no performance impact)
- BFS for reachable airports is O(V + E) where V=airports, E=routes
- Pre-compute graph once in RouteAnalysisView (already done)

### Extensibility
- Submenu pattern makes it easy to add new top-level categories
- Each submenu controller is independent and can be extended
- New views can be added to existing submenus without modifying others

## Open Questions / Decisions Made

1. ✅ **Data View navigation**: Direct to submenu (no intermediate overview)
2. ✅ **Reachable airports algorithm**: BFS with minimum layover counts shown
3. ✅ **FlightView split**: Use rendering mode parameter for overview vs details
4. ⚠️ **DataOverviewView**: Currently option 1, should it be removed or moved to Data View submenu? **Decision**: Remove from main menu (data overview info is less critical with organized submenus)

## Dependencies

- **External**: JGraphT (already in use for graph operations)
- **Internal**: All existing views and mappers
- **Build**: No changes to Maven configuration needed

## Backwards Compatibility

This is a breaking change to the user interface:
- Menu option numbers will change
- Navigation patterns will change
- Command-line arguments unchanged
- Data processing unchanged
- All existing views remain functional

Users will need to adapt to new menu structure, but functionality is preserved and enhanced.
