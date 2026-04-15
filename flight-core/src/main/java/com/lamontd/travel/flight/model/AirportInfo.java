package com.lamontd.travel.flight.model;

import java.util.Objects;
import java.util.Optional;

public class AirportInfo {
    private final String code;          // IATA code (3-letter)
    private final String name;          // Airport name
    private final String city;          // City served
    private final String country;       // Country
    private final String icao;          // ICAO code (4-letter)
    private final Double latitude;      // Decimal degrees
    private final Double longitude;     // Decimal degrees
    private final Integer altitude;     // Feet
    private final Double timezone;      // Hours offset from UTC
    private final String dst;           // DST: E/A/S/O/Z/N/U
    private final String tzDatabase;    // Timezone in tz format
    private final String type;          // airport, station, etc.

    private AirportInfo(Builder builder) {
        this.code = builder.code;
        this.name = builder.name;
        this.city = builder.city;
        this.country = builder.country;
        this.icao = builder.icao;
        this.latitude = builder.latitude;
        this.longitude = builder.longitude;
        this.altitude = builder.altitude;
        this.timezone = builder.timezone;
        this.dst = builder.dst;
        this.tzDatabase = builder.tzDatabase;
        this.type = builder.type;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getCity() {
        return city;
    }

    public Optional<String> getCountry() {
        return Optional.ofNullable(country);
    }

    public Optional<String> getIcao() {
        return Optional.ofNullable(icao);
    }

    public Optional<Double> getLatitude() {
        return Optional.ofNullable(latitude);
    }

    public Optional<Double> getLongitude() {
        return Optional.ofNullable(longitude);
    }

    public Optional<Integer> getAltitude() {
        return Optional.ofNullable(altitude);
    }

    public Optional<Double> getTimezone() {
        return Optional.ofNullable(timezone);
    }

    public Optional<String> getDst() {
        return Optional.ofNullable(dst);
    }

    public Optional<String> getTzDatabase() {
        return Optional.ofNullable(tzDatabase);
    }

    public Optional<String> getType() {
        return Optional.ofNullable(type);
    }

    /**
     * Returns a full display name in the format: "Name (City, Country)"
     * Example: "Hartsfield Jackson Atlanta International Airport (Atlanta, United States)"
     */
    public String getFullDisplayName() {
        if (city != null && country != null) {
            return String.format("%s (%s, %s)", name, city, country);
        } else if (city != null) {
            return String.format("%s (%s)", name, city);
        }
        return name;
    }

    /**
     * Returns a short display name in the format: "City Name (CODE)"
     * Example: "Atlanta (ATL)"
     */
    public String getShortDisplayName() {
        return String.format("%s (%s)", city != null ? city : name, code);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AirportInfo that = (AirportInfo) o;
        return Objects.equals(code, that.code) &&
                Objects.equals(name, that.name) &&
                Objects.equals(city, that.city) &&
                Objects.equals(country, that.country) &&
                Objects.equals(icao, that.icao);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, name, city, country, icao);
    }

    @Override
    public String toString() {
        return "AirportInfo{" +
                "code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", city='" + city + '\'' +
                ", country='" + country + '\'' +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String code;
        private String name;
        private String city;
        private String country;
        private String icao;
        private Double latitude;
        private Double longitude;
        private Integer altitude;
        private Double timezone;
        private String dst;
        private String tzDatabase;
        private String type;

        public Builder code(String code) {
            this.code = code;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder city(String city) {
            this.city = city;
            return this;
        }

        public Builder country(String country) {
            this.country = country;
            return this;
        }

        public Builder icao(String icao) {
            this.icao = icao;
            return this;
        }

        public Builder latitude(Double latitude) {
            this.latitude = latitude;
            return this;
        }

        public Builder longitude(Double longitude) {
            this.longitude = longitude;
            return this;
        }

        public Builder altitude(Integer altitude) {
            this.altitude = altitude;
            return this;
        }

        public Builder timezone(Double timezone) {
            this.timezone = timezone;
            return this;
        }

        public Builder dst(String dst) {
            this.dst = dst;
            return this;
        }

        public Builder tzDatabase(String tzDatabase) {
            this.tzDatabase = tzDatabase;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public AirportInfo build() {
            Objects.requireNonNull(code, "code is required");
            Objects.requireNonNull(name, "name is required");
            Objects.requireNonNull(city, "city is required");
            return new AirportInfo(this);
        }
    }
}
