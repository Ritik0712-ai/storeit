package com.cloudvault.entity.converter;

import com.cloudvault.entity.Share;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

// DB check constraint requires lowercase values ('viewer'/'editor'),
// but the Java enum constants are uppercase (VIEWER/EDITOR). Store/read lowercase.
@Converter
public class RoleConverter implements AttributeConverter<Share.Role, String> {
    @Override
    public String convertToDatabaseColumn(Share.Role attribute) {
        return attribute == null ? null : attribute.name().toLowerCase();
    }

    @Override
    public Share.Role convertToEntityAttribute(String dbData) {
        return dbData == null ? null : Share.Role.valueOf(dbData.toUpperCase());
    }
}
