package com.bloodbridge.bloodbridge.converter;

import com.bloodbridge.bloodbridge.enumtype.UrgencyLevel;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class UrgencyLevelConverter implements AttributeConverter<UrgencyLevel, String> {

    @Override
    public String convertToDatabaseColumn(UrgencyLevel attribute) {
        if (attribute == null) return null;
        return Integer.toString(attribute.getValue());
    }

    @Override
    public UrgencyLevel convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return null;
        try {
            return UrgencyLevel.fromValue(Integer.parseInt(dbData));
        } catch (NumberFormatException e) {
            return UrgencyLevel.valueOf(dbData);
        }
    }
}
