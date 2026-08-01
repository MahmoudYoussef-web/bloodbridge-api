package com.bloodbridge.bloodbridge.converter;

import com.bloodbridge.bloodbridge.enumtype.BloodType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class BloodTypeConverter implements AttributeConverter<BloodType, String> {

    @Override
    public String convertToDatabaseColumn(BloodType attribute) {
        if (attribute == null) return null;
        return Integer.toString(attribute.getValue());
    }

    @Override
    public BloodType convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return null;
        try {
            return BloodType.fromValue(Integer.parseInt(dbData));
        } catch (NumberFormatException e) {
            return BloodType.valueOf(dbData);
        }
    }
}
