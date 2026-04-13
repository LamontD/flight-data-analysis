package com.lamontd.asqp.examples;

import com.lamontd.asqp.mapper.CarrierCodeMapper;
import com.lamontd.asqp.model.CarrierInfo;

import java.io.IOException;
import java.util.Optional;

/**
 * Examples of using the CarrierCodeMapper
 */
public class CarrierMapperExample {

    public static void main(String[] args) throws IOException {
        // Example 1: Using the default mapper (loads from resources/data/carriers.csv)
        System.out.println("=== Example 1: Default Mapper ===");
        CarrierCodeMapper defaultMapper = CarrierCodeMapper.getDefault();
        System.out.println("Loaded " + defaultMapper.size() + " carriers");
        System.out.println("DL = " + defaultMapper.getCarrierName("DL"));
        System.out.println("AA = " + defaultMapper.getCarrierFullName("AA"));
        System.out.println();

        // Example 2: Getting detailed carrier info
        System.out.println("=== Example 2: Detailed Carrier Info ===");
        Optional<CarrierInfo> deltaInfo = defaultMapper.getCarrierInfo("DL");
        if (deltaInfo.isPresent()) {
            CarrierInfo info = deltaInfo.get();
            System.out.println("Code: " + info.getCode());
            System.out.println("Name: " + info.getName());
            System.out.println("Full Name: " + info.getFullName());
        }
        System.out.println();

        // Example 3: Checking if a carrier exists
        System.out.println("=== Example 3: Checking Carrier Existence ===");
        System.out.println("Has DL? " + defaultMapper.hasCarrier("DL"));
        System.out.println("Has XX? " + defaultMapper.hasCarrier("XX"));
        System.out.println();

        // Example 4: Getting all carriers
        System.out.println("=== Example 4: All Carriers ===");
        System.out.println("All carrier codes: " + defaultMapper.getAllCodes());
        System.out.println();

        // Example 5: Creating a custom mapper
        System.out.println("=== Example 5: Custom Mapper ===");
        CarrierCodeMapper customMapper = new CarrierCodeMapper();
        customMapper.addCarrier("XX", "Custom Airlines", "Custom Airlines Corporation");
        customMapper.addCarrier("YY", "Test Airways");
        System.out.println("Custom carriers: " + customMapper.getAllCodes());
        System.out.println("XX = " + customMapper.getCarrierFullName("XX"));
        System.out.println();

        // Example 6: Handling unknown carriers gracefully
        System.out.println("=== Example 6: Unknown Carrier Handling ===");
        String unknownCode = "ZZ";
        String displayName = defaultMapper.getCarrierName(unknownCode);
        System.out.println("Unknown carrier '" + unknownCode + "' displays as: " + displayName);
    }
}
