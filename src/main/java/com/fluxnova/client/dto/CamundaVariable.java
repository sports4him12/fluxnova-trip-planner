package com.fluxnova.client.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Camunda REST API variable format: { "value": ..., "type": "String" }
 */
@Getter
@Builder
public class CamundaVariable {
    private Object value;
    private String type;

    public static CamundaVariable of(Object value) {
        String type = inferType(value);
        return CamundaVariable.builder().value(value).type(type).build();
    }

    private static String inferType(Object value) {
        if (value instanceof String)  return "String";
        if (value instanceof Long)    return "Long";
        if (value instanceof Integer) return "Integer";
        if (value instanceof Boolean) return "Boolean";
        if (value instanceof Double)  return "Double";
        return "String";
    }
}
