package com.lamontd.travel.flight.model;

import java.util.Objects;

/**
 * Represents ISO 3166-1 country information
 */
public class CountryInfo {
    private final int id;              // Numeric country code
    private final String alpha2;       // 2-letter code (ISO 3166-1 alpha-2)
    private final String alpha3;       // 3-letter code (ISO 3166-1 alpha-3)
    private final String name;         // Country name

    public CountryInfo(int id, String alpha2, String alpha3, String name) {
        this.id = id;
        this.alpha2 = alpha2;
        this.alpha3 = alpha3;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getAlpha2() {
        return alpha2;
    }

    public String getAlpha2Upper() {
        return alpha2.toUpperCase();
    }

    public String getAlpha3() {
        return alpha3;
    }

    public String getAlpha3Upper() {
        return alpha3.toUpperCase();
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CountryInfo that = (CountryInfo) o;
        return id == that.id &&
                Objects.equals(alpha2, that.alpha2) &&
                Objects.equals(alpha3, that.alpha3) &&
                Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, alpha2, alpha3, name);
    }

    @Override
    public String toString() {
        return "CountryInfo{" +
                "alpha2='" + alpha2 + '\'' +
                ", alpha3='" + alpha3 + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
