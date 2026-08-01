package com.bloodbridge.bloodbridge.converter;

import com.bloodbridge.bloodbridge.enumtype.UserRole;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class UserRoleConverter implements AttributeConverter<UserRole, String> {

    @Override
    public String convertToDatabaseColumn(UserRole attribute) {
        if (attribute == null) return null;
        return Integer.toString(attribute.getValue());
    }

    @Override
    public UserRole convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return null;
        try {
            return UserRole.fromValue(Integer.parseInt(dbData));
        } catch (NumberFormatException e) {
            return UserRole.valueOf(dbData);
        }
    }
}
