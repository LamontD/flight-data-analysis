package com.lamontd.travel.flight.validation;

public class FlightRecordValidationException extends Exception {
    private final int recordNumber;
    private final String fieldName;

    public FlightRecordValidationException(String message, int recordNumber, String fieldName) {
        super(String.format("Validation error at record %d, field '%s': %s", recordNumber, fieldName, message));
        this.recordNumber = recordNumber;
        this.fieldName = fieldName;
    }

    public FlightRecordValidationException(String message, int recordNumber, String fieldName, Throwable cause) {
        super(String.format("Validation error at record %d, field '%s': %s", recordNumber, fieldName, message), cause);
        this.recordNumber = recordNumber;
        this.fieldName = fieldName;
    }

    public int getRecordNumber() {
        return recordNumber;
    }

    public String getFieldName() {
        return fieldName;
    }
}
