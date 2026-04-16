package com.lamontd.travel.flight.asqp.service;

import com.lamontd.travel.flight.mapper.AirportCodeMapper;
import com.lamontd.travel.flight.asqp.model.ASQPFlightRecord;
import com.lamontd.travel.flight.asqp.reader.CsvFlightRecordReader;
import com.lamontd.travel.flight.util.PerformanceTimer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Service for loading flight data from files
 */
public class FlightDataLoader {
    private static final Logger logger = LoggerFactory.getLogger(FlightDataLoader.class);

    /**
     * Loads flight records from multiple files in parallel
     * @param filePaths Array of file paths to load
     * @return Combined list of all flight records
     */
    public List<ASQPFlightRecord> loadFiles(String[] filePaths) {
        try (var timer = new PerformanceTimer("Load " + filePaths.length + " file(s)")) {
            logger.info("Loading {} file(s){}", filePaths.length,
                    filePaths.length > 1 ? " in parallel" : "");

            // Use parallel streams for multiple files, sequential for single file
            List<ASQPFlightRecord> allRecords = (filePaths.length > 1
                    ? Arrays.stream(filePaths).parallel()
                    : Arrays.stream(filePaths))
                    .map(this::processFile)
                    .filter(Objects::nonNull)
                    .flatMap(List::stream)
                    .collect(Collectors.toList());

            // Show summary
            if (allRecords.isEmpty()) {
                logger.error("No valid records found in any file");
            } else {
                logger.info("Successfully loaded {} total records from {} file(s)",
                        allRecords.size(), filePaths.length);

                if (filePaths.length > 1) {
                    logger.debug("Average: {} records per file",
                            (allRecords.size() / filePaths.length));
                }

                // Show cancelled vs operated summary
                long cancelled = allRecords.stream().filter(ASQPFlightRecord::isCancelled).count();
                long operated = allRecords.size() - cancelled;
                logger.info("Operated: {} ({} %), Cancelled: {} ({} %)",
                        operated, String.format("%.1f", operated * 100.0 / allRecords.size()),
                        cancelled, String.format("%.1f", cancelled * 100.0 / allRecords.size()));
            }

            return allRecords;
        }
    }

    /**
     * Processes a single flight data file
     * @param filePath Path to the file to process
     * @return List of flight records, or null if error occurred
     */
    private List<ASQPFlightRecord> processFile(String filePath) {
        AirportCodeMapper airportMapper = AirportCodeMapper.getDefault();
        CsvFlightRecordReader reader = new CsvFlightRecordReader(airportMapper);
        Path path = Paths.get(filePath);

        try {
            List<ASQPFlightRecord> records = reader.readFromFile(path);

            if (records.isEmpty()) {
                logger.warn("No records found in {}", filePath);
            } else {
                logger.info("Loaded {} records from {}", records.size(), path.getFileName());
            }

            return records;

        } catch (IOException e) {
            logger.error("Error reading {}: {}", filePath, e.getMessage());
            return null;
        } catch (Exception e) {
            logger.error("Unexpected error processing {}: {}", filePath, e.getMessage());
            return null;
        }
    }
}
