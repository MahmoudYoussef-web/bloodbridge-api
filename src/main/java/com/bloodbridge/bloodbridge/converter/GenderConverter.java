package com.bloodbridge.bloodbridge.converter;

import com.bloodbridge.bloodbridge.enumtype.Gender;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class GenderConverter implements AttributeConverter<Gender, String> {

    @Override
    public String convertToDatabaseColumn(Gender attribute) {
        if (attribute == null) return null;
        return Integer.toString(attribute.getValue());
    }

    @Override
    public Gender convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return null;
        try {
            return Gender.fromValue(Integer.parseInt(dbData));
        } catch (NumberFormatException e) {
            return Gender.valueOf(dbData);
        }
    }
}
