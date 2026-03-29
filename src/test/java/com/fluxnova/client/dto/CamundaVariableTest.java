package com.fluxnova.client.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CamundaVariableTest {

    @Test
    void of_string_setsTypeString() {
        CamundaVariable v = CamundaVariable.of("hello");
        assertThat(v.getValue()).isEqualTo("hello");
        assertThat(v.getType()).isEqualTo("String");
    }

    @Test
    void of_long_setsTypeLong() {
        CamundaVariable v = CamundaVariable.of(42L);
        assertThat(v.getType()).isEqualTo("Long");
    }

    @Test
    void of_integer_setsTypeInteger() {
        CamundaVariable v = CamundaVariable.of(7);
        assertThat(v.getType()).isEqualTo("Integer");
    }

    @Test
    void of_boolean_setsTypeBoolean() {
        CamundaVariable v = CamundaVariable.of(true);
        assertThat(v.getType()).isEqualTo("Boolean");
    }

    @Test
    void of_double_setsTypeDouble() {
        CamundaVariable v = CamundaVariable.of(3.14);
        assertThat(v.getType()).isEqualTo("Double");
    }

    @Test
    void of_unknownType_defaultsToString() {
        CamundaVariable v = CamundaVariable.of(new Object());
        assertThat(v.getType()).isEqualTo("String");
    }
}
