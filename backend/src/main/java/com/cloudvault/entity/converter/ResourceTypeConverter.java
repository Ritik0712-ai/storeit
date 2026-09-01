package com.cloudvault.entity.converter;

import com.cloudvault.entity.Share;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

// DB check constraints require lowercase values ('file'/'folder'),
// but the Java enum constants are uppercase (FILE/FOLDER). Store/read lowercase.
@Converter
public class ResourceTypeConverter implements AttributeConverter<Share.ResourceType, String> {
    @Override
    public String convertToDatabaseColumn(Share.ResourceType attribute) {
        return attribute == null ? null : attribute.name().toLowerCase();
    }

    @Override
    public Share.ResourceType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : Share.ResourceType.valueOf(dbData.toUpperCase());
    }
}
