package com.bloodbridge.bloodbridge.converter;

import com.bloodbridge.bloodbridge.enumtype.BloodRequestStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class BloodRequestStatusConverter implements AttributeConverter<BloodRequestStatus, String> {

    @Override
    public String convertToDatabaseColumn(BloodRequestStatus attribute) {
        if (attribute == null) return null;
        return Integer.toString(attribute.getValue());
    }

    @Override
    public BloodRequestStatus convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return null;
        try {
            return BloodRequestStatus.fromValue(Integer.parseInt(dbData));
        } catch (NumberFormatException e) {
            return BloodRequestStatus.valueOf(dbData);
        }
    }
}
