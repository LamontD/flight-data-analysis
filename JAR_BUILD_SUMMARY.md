# Executable JAR Build - Summary

## ✅ Configuration Complete

The ASQP Reader is now configured to build as an executable JAR file with all dependencies included.

## 🎯 What Was Configured

### Maven Shade Plugin

Added to `pom.xml`:
- **Plugin:** `maven-shade-plugin` version 3.6.0
- **Main Class:** `com.lamontd.asqp.App`
- **Output:** `target/asqp-reader.jar`
- **Features:**
  - Bundles all dependencies (Apache Commons CSV, Gson)
  - Includes all resource files (airports, airlines, countries)
  - Creates executable JAR with manifest
  - Filters out signature files to avoid security exceptions

## 📦 Build Results

### Build Command
```bash
mvn clean package
```

### Output Files
```
target/
├── asqp-reader.jar                    # ⭐ Executable JAR (1.7 MB) - USE THIS
├── asqp-reader-1.0-SNAPSHOT.jar       # Original without dependencies (516 KB)
└── ... other build artifacts
```

### What's Included in the JAR

**Application Code:**
- All Java classes
- Flight record reader
- Carrier, airport, country mappers
- Data quality validation

**Dependencies (automatically bundled):**
- Apache Commons CSV 1.12.0
- Apache Commons IO 2.17.0
- Apache Commons Codec 1.17.1
- Google Gson 2.11.0

**Resource Files (embedded):**
- `airlines.dat` - 992 carriers
- `airports.dat` - 6,033 airports
- `countries.json` - 193 countries
- `sample-data.asc.groomed` - Sample flight data

**Total Size:** ~1.7 MB (fully self-contained)

## 🚀 Usage

### Building

**Full build with tests:**
```bash
mvn clean package
```

**Skip tests (faster):**
```bash
mvn clean package -DskipTests
```

**Build time:** ~5-15 seconds depending on system

### Running

**Basic usage:**
```bash
java -jar target/asqp-reader.jar <csv-file-path>
```

**With sample data:**
```bash
java -jar target/asqp-reader.jar
```

**From any location:**
```bash
java -jar /path/to/asqp-reader.jar data.csv
```

### Example Session

```bash
# Build the JAR
$ mvn clean package
[INFO] BUILD SUCCESS
[INFO] Total time:  8.311 s

# Check the file
$ ls -lh target/asqp-reader.jar
-rw-r--r-- 1 user group 1.7M Apr 12 20:50 target/asqp-reader.jar

# Run it
$ java -jar target/asqp-reader.jar
Loaded 6033 airports from 7698 records
Loaded 193 countries
Loaded 992 carriers, 6033 airports, and 193 countries

Successfully loaded 500 flight records
...
```

## 🎨 Deployment

### Option 1: Simple Copy

The JAR is completely self-contained:

```bash
# Copy to production server
scp target/asqp-reader.jar user@server:/opt/apps/

# Run on server
ssh user@server
cd /opt/apps
java -jar asqp-reader.jar flight-data.csv
```

### Option 2: Create Wrapper Script

**Linux/Mac:**
```bash
#!/bin/bash
java -jar /opt/apps/asqp-reader.jar "$@"
```

**Windows:**
```batch
@echo off
java -jar C:\Apps\asqp-reader.jar %*
```

### Option 3: Create Distribution Package

```bash
mkdir asqp-reader-dist
cp target/asqp-reader.jar asqp-reader-dist/
cp README.md asqp-reader-dist/
cp BUILD_AND_DEPLOY.md asqp-reader-dist/
zip -r asqp-reader-dist.zip asqp-reader-dist/
```

## 📊 Verification

### All Tests Pass

```
Tests run: 49, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### JAR Functionality Verified

```bash
$ java -jar target/asqp-reader.jar

✓ Loads successfully
✓ All resources found (airlines, airports, countries)
✓ Data quality validation works
✓ Processes sample data correctly
✓ Outputs complete statistics
```

## 🔧 Configuration Details

### pom.xml Changes

**Added `<build>` section with:**

```xml
<build>
  <plugins>
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-shade-plugin</artifactId>
      <version>3.6.0</version>
      <executions>
        <execution>
          <phase>package</phase>
          <goals>
            <goal>shade</goal>
          </goals>
          <configuration>
            <transformers>
              <transformer implementation="...ManifestResourceTransformer">
                <mainClass>com.lamontd.asqp.App</mainClass>
              </transformer>
            </transformers>
            <filters>
              <!-- Exclude signature files -->
              <filter>
                <artifact>*:*</artifact>
                <excludes>
                  <exclude>META-INF/*.SF</exclude>
                  <exclude>META-INF/*.DSA</exclude>
                  <exclude>META-INF/*.RSA</exclude>
                </excludes>
              </filter>
            </filters>
            <finalName>asqp-reader</finalName>
          </configuration>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
```

### Why Maven Shade Plugin?

**Benefits:**
- ✅ Creates "uber-jar" with all dependencies
- ✅ Handles classpath conflicts
- ✅ Creates proper MANIFEST.MF
- ✅ Filters out signature files
- ✅ Single file deployment
- ✅ No dependency installation needed

**Alternatives (not used):**
- `maven-assembly-plugin` - Similar but less flexible
- `maven-jar-plugin` - Doesn't bundle dependencies
- `spring-boot-maven-plugin` - Overkill for non-Spring apps

## ⚙️ Requirements

### Build Requirements
- Java 23 JDK
- Maven 3.6+
- Internet connection (first build, to download plugins)

### Runtime Requirements
- Java 23 JRE or JDK
- No other dependencies!

### Verify Java Version

```bash
$ java -version
java version "23" 2024-09-17
```

## 📝 Build Warnings (Normal)

You may see these warnings during build - they're **normal and can be ignored**:

```
[WARNING] commons-codec-1.17.1.jar, commons-csv-1.12.0.jar define 2 overlapping resources:
[WARNING]   - META-INF/LICENSE.txt
[WARNING]   - META-INF/NOTICE.txt
```

**Explanation:** Multiple libraries include the same license files. Maven Shade handles this by keeping one copy.

## 🎯 Use Cases

### 1. Development

During development, continue using Maven:
```bash
mvn exec:java -Dexec.mainClass="com.lamontd.asqp.App"
```

### 2. Testing

Build and test the JAR:
```bash
mvn clean package
java -jar target/asqp-reader.jar test-data.csv
```

### 3. Production Deployment

Deploy the JAR to production:
```bash
# Build release
mvn clean package

# Copy to server
scp target/asqp-reader.jar prod-server:/opt/asqp/

# Run on server
ssh prod-server "java -jar /opt/asqp/asqp-reader.jar /data/flights.csv"
```

### 4. Scheduled Jobs

Use in cron jobs or scheduled tasks:
```bash
# Linux cron
0 2 * * * java -jar /opt/asqp-reader.jar /data/daily/flights-$(date +\%Y\%m\%d).csv
```

```batch
REM Windows Task Scheduler
java -jar C:\Apps\asqp-reader.jar C:\Data\flights.csv
```

## 🔄 Updating Reference Data

If you update the reference data files:

1. **Update source files:**
   ```bash
   curl -o src/main/resources/data/airports.dat ...
   ```

2. **Rebuild JAR:**
   ```bash
   mvn clean package
   ```

3. **Redeploy:**
   ```bash
   cp target/asqp-reader.jar /deployment/location/
   ```

The new data will be embedded in the JAR automatically.

## 📊 Performance

| Metric | Value |
|--------|-------|
| JAR Size | 1.7 MB |
| Build Time (with tests) | ~8-15 seconds |
| Build Time (skip tests) | ~5-8 seconds |
| Startup Time | ~0.5-1.0 seconds |
| Memory Usage | ~100-200 MB |

## 🎉 Summary

The ASQP Reader is now a **fully deployable, self-contained executable JAR**!

**What you get:**
- ✅ Single JAR file (~1.7 MB)
- ✅ All dependencies included
- ✅ All resource data embedded
- ✅ Executable with `java -jar`
- ✅ No installation required
- ✅ Easy deployment
- ✅ Platform independent

**Build it:**
```bash
mvn clean package
```

**Deploy it:**
```bash
cp target/asqp-reader.jar /anywhere/
```

**Run it:**
```bash
java -jar asqp-reader.jar data.csv
```

**That's it!** 🚀

## 📚 Documentation

Complete build and deployment documentation:
- **[BUILD_AND_DEPLOY.md](BUILD_AND_DEPLOY.md)** - Complete guide
- **[README.md](README.md)** - Updated with JAR instructions
- **[JAR_BUILD_SUMMARY.md](JAR_BUILD_SUMMARY.md)** - This document

Everything is ready for production deployment!
