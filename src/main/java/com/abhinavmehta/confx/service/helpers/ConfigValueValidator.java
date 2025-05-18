package com.abhinavmehta.confx.service.helpers;

import com.abhinavmehta.confx.model.enums.ConfigDataType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class ConfigValueValidator {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public boolean isValid(String value, ConfigDataType dataType) {
        if (value == null) { // Depending on requirements, null might be a valid representation for some types or mean "unset"
            return true; // For now, assume null is permissible if the DB column allows it.
        }
        switch (dataType) {
            case BOOLEAN:
                return "true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value);
            case INTEGER:
                try {
                    Integer.parseInt(value);
                    return true;
                } catch (NumberFormatException e) {
                    return false;
                }
            case DOUBLE:
                try {
                    Double.parseDouble(value);
                    return true;
                } catch (NumberFormatException e) {
                    return false;
                }
            case STRING:
                return true; // Any string is valid by default, specific constraints could be added.
            case JSON:
                try {
                    objectMapper.readTree(value);
                    return true;
                } catch (JsonProcessingException e) {
                    return false;
                }
            default:
                return false;
        }
    }
} 