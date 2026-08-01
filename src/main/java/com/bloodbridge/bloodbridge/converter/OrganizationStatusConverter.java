package com.bloodbridge.bloodbridge.converter;

import com.bloodbridge.bloodbridge.enumtype.OrganizationStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class OrganizationStatusConverter implements AttributeConverter<OrganizationStatus, String> {

    @Override
    public String convertToDatabaseColumn(OrganizationStatus attribute) {
        if (attribute == null) return null;
        return Integer.toString(attribute.getValue());
    }

    @Override
    public OrganizationStatus convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return null;
        try {
            return OrganizationStatus.fromValue(Integer.parseInt(dbData));
        } catch (NumberFormatException e) {
            return OrganizationStatus.valueOf(dbData);
        }
    }
}
