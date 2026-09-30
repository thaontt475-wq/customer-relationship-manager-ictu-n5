package com.crm.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordUtilTest {

    @Test
    void acceptsSprintOnePasswordPolicyWithoutExtraComplexityRules() {
        assertTrue(PasswordUtil.isValidPassword("password1"));
        assertTrue(PasswordUtil.isValidPassword("MATKHAU1"));
        assertTrue(PasswordUtil.isValidPassword("abc12345"));
    }

    @Test
    void rejectsShortPasswordsOrPasswordsWithoutLetterAndNumber() {
        assertFalse(PasswordUtil.isValidPassword("abc1234"));
        assertFalse(PasswordUtil.isValidPassword("abcdefgh"));
        assertFalse(PasswordUtil.isValidPassword("12345678"));
        assertFalse(PasswordUtil.isValidPassword(null));
    }
}
