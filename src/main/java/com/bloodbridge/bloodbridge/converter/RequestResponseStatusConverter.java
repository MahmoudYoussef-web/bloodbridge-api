package com.bloodbridge.bloodbridge.converter;

import com.bloodbridge.bloodbridge.enumtype.RequestResponseStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class RequestResponseStatusConverter implements AttributeConverter<RequestResponseStatus, String> {

    @Override
    public String convertToDatabaseColumn(RequestResponseStatus attribute) {
        if (attribute == null) return null;
        return Integer.toString(attribute.getValue());
    }

    @Override
    public RequestResponseStatus convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return null;
        try {
            return RequestResponseStatus.fromValue(Integer.parseInt(dbData));
        } catch (NumberFormatException e) {
            return RequestResponseStatus.valueOf(dbData);
        }
    }
}
