package com.chaula.dental.service;

import com.chaula.dental.util.PhoneUtils;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PhoneUtilsTest {
    @Test
    void normalizesLocalAndCountryPrefixedIndianNumbers() {
        assertThat(PhoneUtils.normalizeIndianPhone("98107 64715")).isEqualTo("919810764715");
        assertThat(PhoneUtils.normalizeIndianPhone("098107-64715")).isEqualTo("919810764715");
        assertThat(PhoneUtils.normalizeIndianPhone("+91 (98107) 64715")).isEqualTo("919810764715");
    }

    @Test
    void rejectsNumbersThatCannotBeNormalizedToAnIndianMobile() {
        assertThatThrownBy(() -> PhoneUtils.normalizeIndianPhone("12345"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PhoneUtils.normalizeIndianPhone(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
