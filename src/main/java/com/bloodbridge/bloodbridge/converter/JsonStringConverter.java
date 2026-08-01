package com.bloodbridge.bloodbridge.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class JsonStringConverter implements AttributeConverter<String, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return attribute;
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty() || dbData.charAt(0) != '"') {
            return dbData;
        }
        try {
            String unwrapped = MAPPER.readValue(dbData, String.class);
            if (unwrapped.startsWith("{") || unwrapped.startsWith("[")) {
                return unwrapped;
            }
        } catch (Exception ignored) {
        }
        return dbData;
    }
}
