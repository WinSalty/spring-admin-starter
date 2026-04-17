package com.salty.admin.auth.service;

import com.salty.admin.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PasswordPolicyValidatorTest {

    private final PasswordPolicyValidator validator = new PasswordPolicyValidator();

    @Test
    void acceptsStrongPassword() {
        assertDoesNotThrow(() -> validator.validate("Admin@123456", "Admin@123456"));
    }

    @Test
    void rejectsWeakPassword() {
        assertThrows(BusinessException.class, () -> validator.validate("password", "password"));
    }

    @Test
    void rejectsMismatchedConfirmation() {
        assertThrows(BusinessException.class, () -> validator.validate("Admin@123456", "Admin@654321"));
    }
}
