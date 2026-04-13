# Building and Deploying ASQP Reader

## Building the Executable JAR

The project is configured to build an executable JAR file with all dependencies included using the Maven Shade Plugin.

### Build Command

```bash
mvn clean package
```

This will:
1. Clean previous builds
2. Compile the code
3. Run all tests
4. Create an executable JAR with all dependencies

### Skip Tests (Faster Build)

If you want to skip tests during the build:

```bash
mvn clean package -DskipTests
```

### Build Output

The build creates the executable JAR at:
```
target/asqp-reader.jar
```

**Size:** ~1.7 MB (includes all dependencies)

**Contents:**
- Your application code
- Apache Commons CSV library
- Google Gson library
- All resource files (airports.dat, airlines.dat, countries.json)

## Running the JAR

### Basic Usage

```bash
java -jar target/asqp-reader.jar <csv-file-path>
```

### Examples

**1. Process a specific CSV file:**
```bash
java -jar target/asqp-reader.jar /path/to/flight-data.csv
```

**2. Run with sample data (no arguments):**
```bash
java -jar target/asqp-reader.jar
```

**3. Run from any directory:**
```bash
java -jar /path/to/asqp-reader.jar data.csv
```

## Deployment

### Option 1: Copy JAR Only

The JAR is completely self-contained. Simply copy it to your target location:

```bash
# Copy to deployment directory
cp target/asqp-reader.jar /opt/asqp-reader/

# Run from deployment directory
cd /opt/asqp-reader
java -jar asqp-reader.jar flight-data.csv
```

### Option 2: Create a Distribution Package

Create a distribution with the JAR and documentation:

```bash
mkdir asqp-reader-dist
cp target/asqp-reader.jar asqp-reader-dist/
cp README.md asqp-reader-dist/
cp DATA_QUALITY_VALIDATION.md asqp-reader-dist/
cp AIRPORT_INTEGRATION.md asqp-reader-dist/
cp COUNTRY_INTEGRATION.md asqp-reader-dist/
zip -r asqp-reader-dist.zip asqp-reader-dist/
```

### Option 3: Create a Wrapper Script

**Linux/Mac (asqp-reader.sh):**
```bash
#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
java -jar "$DIR/asqp-reader.jar" "$@"
```

Make executable:
```bash
chmod +x asqp-reader.sh
./asqp-reader.sh data.csv
```

**Windows (asqp-reader.bat):**
```batch
@echo off
java -jar "%~dp0asqp-reader.jar" %*
```

Usage:
```cmd
asqp-reader.bat data.csv
```

## Java Version Requirement

**Required:** Java 23 or higher

### Check Java Version

```bash
java -version
```

Should show: `java version "23"` or higher

### If Java 23 is not available

You can modify the project to target an earlier Java version by editing `pom.xml`:

```xml
<properties>
  <maven.compiler.target>17</maven.compiler.target>
  <maven.compiler.source>17</maven.compiler.source>
</properties>
```

Then rebuild:
```bash
mvn clean package
```

## Resource Files

The JAR includes all necessary resource files:
- `airlines.dat` - 992 carriers (OpenFlights)
- `airports.dat` - 6,033 airports (OpenFlights)
- `countries.json` - 193 countries (ISO 3166-1)

These are bundled inside the JAR and loaded automatically.

## Updating Reference Data

If you need to update the reference data (airlines, airports, countries):

1. **Update source files:**
   ```bash
   # Update airports
   curl -o src/main/resources/data/airports.dat \
     https://raw.githubusercontent.com/jpatokal/openflights/master/data/airports.dat
   
   # Update airlines
   curl -o src/main/resources/data/airlines.dat \
     https://raw.githubusercontent.com/jpatokal/openflights/master/data/airlines.dat
   ```

2. **Rebuild the JAR:**
   ```bash
   mvn clean package
   ```

3. **Deploy the new JAR:**
   ```bash
   cp target/asqp-reader.jar /deployment/path/
   ```

## Troubleshooting

### "No main manifest attribute"

If you see this error, the JAR wasn't built correctly. Rebuild:
```bash
mvn clean package
```

Ensure you're running the shaded JAR:
```bash
java -jar target/asqp-reader.jar
```

### "UnsupportedClassVersionError"

Your Java version is too old. The JAR requires Java 23+.

Check version:
```bash
java -version
```

### "ClassNotFoundException"

This shouldn't happen with the shaded JAR. If it does:
1. Verify you're using the correct JAR file
2. Rebuild with `mvn clean package`
3. Check that the build completed successfully

### Resource Files Not Found

If you see "Resource not found" errors:
1. Verify the JAR was built with `mvn package` (not just `mvn compile`)
2. Check that resource files exist in `src/main/resources/data/`
3. Rebuild with `mvn clean package`

## Build Artifacts

After running `mvn package`, you'll find:

```
target/
├── asqp-reader.jar                      # Executable JAR with dependencies (USE THIS)
├── asqp-reader-1.0-SNAPSHOT.jar         # Original JAR without dependencies
├── classes/                              # Compiled .class files
├── generated-sources/                    # Generated code
├── maven-archiver/                       # Maven metadata
├── maven-status/                         # Build status
└── surefire-reports/                     # Test reports
```

**Always use:** `asqp-reader.jar` (the larger file, ~1.7 MB)

## Continuous Integration

### GitHub Actions Example

```yaml
name: Build JAR

on: [push]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 23
        uses: actions/setup-java@v3
        with:
          java-version: '23'
          distribution: 'temurin'
      - name: Build with Maven
        run: mvn clean package
      - name: Upload JAR
        uses: actions/upload-artifact@v3
        with:
          name: asqp-reader
          path: target/asqp-reader.jar
```

### Jenkins Pipeline Example

```groovy
pipeline {
    agent any
    stages {
        stage('Build') {
            steps {
                sh 'mvn clean package'
            }
        }
        stage('Archive') {
            steps {
                archiveArtifacts artifacts: 'target/asqp-reader.jar'
            }
        }
    }
}
```

## Performance

**Build times:**
- Full build with tests: ~10-15 seconds
- Build without tests: ~5-8 seconds
- Clean build: ~10-20 seconds

**JAR file:**
- Size: ~1.7 MB
- Startup time: ~0.5-1.0 seconds
- Memory usage: ~100-200 MB

## Development vs Production

### Development

During development, use Maven exec:
```bash
mvn exec:java -Dexec.mainClass="com.lamontd.asqp.App" -Dexec.args="data.csv"
```

### Production

In production, use the JAR:
```bash
java -jar asqp-reader.jar data.csv
```

## Version Updates

To update the version number:

1. Edit `pom.xml`:
   ```xml
   <version>1.1-SNAPSHOT</version>
   ```

2. Rebuild:
   ```bash
   mvn clean package
   ```

The JAR will be named according to the version.

## Summary

**Build:** `mvn clean package`

**Output:** `target/asqp-reader.jar`

**Run:** `java -jar target/asqp-reader.jar <file.csv>`

**Deploy:** Copy the JAR file anywhere - it's self-contained!

## Quick Start

```bash
# Build
mvn clean package

# Test
java -jar target/asqp-reader.jar

# Deploy
cp target/asqp-reader.jar /opt/apps/
```

That's it! The JAR is completely self-contained and ready to deploy. 🚀
