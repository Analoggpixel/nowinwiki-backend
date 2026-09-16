package com.analoggpixel.nowinwiki;

import com.analoggpixel.nowinwiki.common.util.RegexUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegexUtilsTest {

    @Test
    void phoneValidation() {
        assertFalse(RegexUtils.isPhoneInvalid("13800138000"));
        assertTrue(RegexUtils.isPhoneInvalid("12345"));
    }

    @Test
    void codeValidation() {
        assertFalse(RegexUtils.isCodeInvalid("123456"));
        assertTrue(RegexUtils.isCodeInvalid("12ab56"));
    }
}
