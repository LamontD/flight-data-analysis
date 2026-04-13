package com.lamontd.asqp.model;

import java.util.Objects;
import java.util.Optional;

public class CarrierInfo {
    private final String code;          // IATA code (2-letter)
    private final String name;          // Airline name
    private final String fullName;      // Full name (for backward compatibility)
    private final String icao;          // ICAO code (3-letter)
    private final String callsign;      // Airline callsign
    private final String country;       // Country/territory
    private final boolean active;       // Whether airline is currently active

    private CarrierInfo(Builder builder) {
        this.code = builder.code;
        this.name = builder.name;
        this.fullName = builder.fullName != null ? builder.fullName : builder.name;
        this.icao = builder.icao;
        this.callsign = builder.callsign;
        this.country = builder.country;
        this.active = builder.active;
    }

    public CarrierInfo(String code, String name, String fullName) {
        this.code = code;
        this.name = name;
        this.fullName = fullName;
        this.icao = null;
        this.callsign = null;
        this.country = null;
        this.active = true;
    }

    public CarrierInfo(String code, String name) {
        this(code, name, name);
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getFullName() {
        return fullName;
    }

    public Optional<String> getIcao() {
        return Optional.ofNullable(icao);
    }

    public Optional<String> getCallsign() {
        return Optional.ofNullable(callsign);
    }

    public Optional<String> getCountry() {
        return Optional.ofNullable(country);
    }

    public boolean isActive() {
        return active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CarrierInfo that = (CarrierInfo) o;
        return active == that.active &&
                Objects.equals(code, that.code) &&
                Objects.equals(name, that.name) &&
                Objects.equals(fullName, that.fullName) &&
                Objects.equals(icao, that.icao) &&
                Objects.equals(callsign, that.callsign) &&
                Objects.equals(country, that.country);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, name, fullName, icao, callsign, country, active);
    }

    @Override
    public String toString() {
        return "CarrierInfo{" +
                "code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", country='" + country + '\'' +
                ", active=" + active +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String code;
        private String name;
        private String fullName;
        private String icao;
        private String callsign;
        private String country;
        private boolean active = true;

        public Builder code(String code) {
            this.code = code;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder fullName(String fullName) {
            this.fullName = fullName;
            return this;
        }

        public Builder icao(String icao) {
            this.icao = icao;
            return this;
        }

        public Builder callsign(String callsign) {
            this.callsign = callsign;
            return this;
        }

        public Builder country(String country) {
            this.country = country;
            return this;
        }

        public Builder active(boolean active) {
            this.active = active;
            return this;
        }

        public CarrierInfo build() {
            Objects.requireNonNull(code, "code is required");
            Objects.requireNonNull(name, "name is required");
            return new CarrierInfo(this);
        }
    }
}
