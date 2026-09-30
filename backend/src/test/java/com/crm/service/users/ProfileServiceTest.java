package com.crm.service.users;

import com.crm.dao.users.UserDAO;
import com.crm.model.Role;
import com.crm.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CRM-35 ProfileService:
 * 1. Viewing profile with roles
 * 2. Updating allowed fields (full_name, phone, signature)
 * 3. Blocking email, team, role updates
 * 4. Vietnamese phone number validation regex
 */
@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    private static final long USER_ID = 100L;

    @Mock
    private UserDAO userDAO;

    private ProfileService profileService;

    @BeforeEach
    void setUp() {
        profileService = new ProfileService(userDAO);
    }

    @Nested
    @DisplayName("Phone Validation Tests (Vietnamese format)")
    class PhoneValidationTests {

        @ParameterizedTest
        @ValueSource(strings = {
                "0912345678",
                "0387654321",
                "0791122334",
                "0855667788",
                "0566778899",
                "+84912345678",
                "+84387654321",
                "091 234 5678",
                "091-234-5678",
                "091.234.5678"
        })
        @DisplayName("Valid Vietnamese phone numbers are accepted and normalized")
        void validPhones_accepted(String phone) {
            String normalized = profileService.normalizeAndValidatePhone(phone);
            assertNotNull(normalized);
            assertTrue(normalized.startsWith("0"));
            assertEquals(10, normalized.length());
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "1234567890",       // doesn't start with 0
                "0123456789",       // 01 prefix is old/invalid
                "0212345678",       // landline prefix, not mobile
                "091234567",        // 9 digits (too short)
                "09123456789",      // 11 digits (too long)
                "0912abc678",       // non-numeric
                "abcdefghij"        // letters
        })
        @DisplayName("Invalid phone numbers throw IllegalArgumentException")
        void invalidPhones_throwException(String phone) {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> profileService.normalizeAndValidatePhone(phone));
            assertTrue(ex.getMessage().contains("Số điện thoại không hợp lệ"));
        }

        @Test
        @DisplayName("Empty or null phone is allowed (returns empty string)")
        void emptyPhone_allowed() {
            assertEquals("", profileService.normalizeAndValidatePhone(""));
            assertEquals("", profileService.normalizeAndValidatePhone(null));
            assertEquals("", profileService.normalizeAndValidatePhone("   "));
        }
    }

    @Nested
    @DisplayName("Profile Update Tests (CRM-35 Business Rules)")
    class ProfileUpdateTests {

        @Test
        @DisplayName("Updating full_name, phone, and signature succeeds")
        void updateProfile_success() throws SQLException {
            User existing = new User();
            existing.setId(USER_ID);
            existing.setEmail("user@example.com");
            existing.setFullName("Old Name");
            existing.setPhone("0911111111");
            existing.setSignature("Old Signature");
            existing.setTeamName("Sales Team");
            existing.setRoles(List.of(new Role(1L, "Sales Rep")));

            User updatedUser = new User();
            updatedUser.setId(USER_ID);
            updatedUser.setEmail("user@example.com"); // Email unchanged
            updatedUser.setFullName("Nguyen Van A");
            updatedUser.setPhone("0912345678");
            updatedUser.setSignature("Trân trọng,\nNguyen Van A");
            updatedUser.setTeamName("Sales Team"); // Team unchanged
            updatedUser.setRoles(List.of(new Role(1L, "Sales Rep"))); // Role unchanged

            when(userDAO.findByIdForUpdate(any(Connection.class), eq(USER_ID))).thenReturn(existing);
            when(userDAO.updateUserSelfProfile(any(Connection.class), eq(USER_ID), eq("Nguyen Van A"), eq("0912345678"), anyString()))
                    .thenReturn(1);
            when(userDAO.findUserProfileWithRoles(any(Connection.class), eq(USER_ID))).thenReturn(updatedUser);

            User result = profileService.updateProfile(USER_ID, "Nguyen Van A", "0912345678", "Trân trọng,\nNguyen Van A");

            assertNotNull(result);
            assertEquals("Nguyen Van A", result.getFullName());
            assertEquals("0912345678", result.getPhone());
            assertEquals("Trân trọng,\nNguyen Van A", result.getSignature());
            assertEquals("user@example.com", result.getEmail());
            assertEquals("Sales Team", result.getTeamName());

            // Verify updateUserSelfProfile was called strictly with name, phone, signature
            verify(userDAO).updateUserSelfProfile(any(Connection.class), eq(USER_ID), eq("Nguyen Van A"), eq("0912345678"), eq("Trân trọng,\nNguyen Van A"));
        }

        @Test
        @DisplayName("Empty fullName throws IllegalArgumentException")
        void emptyFullName_throws() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> profileService.updateProfile(USER_ID, "", "0912345678", "Sig"));
            assertTrue(ex.getMessage().contains("Họ và tên"));
        }

        @Test
        @DisplayName("Invalid phone throws IllegalArgumentException during update")
        void invalidPhone_throws() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> profileService.updateProfile(USER_ID, "Nguyen Van A", "123456", "Sig"));
            assertTrue(ex.getMessage().contains("Số điện thoại không hợp lệ"));
        }

        @Test
        @DisplayName("User not found throws IllegalArgumentException")
        void userNotFound_throws() throws SQLException {
            when(userDAO.findByIdForUpdate(any(Connection.class), eq(USER_ID))).thenReturn(null);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> profileService.updateProfile(USER_ID, "Nguyen Van A", "0912345678", "Sig"));
            assertTrue(ex.getMessage().contains("Không tìm thấy"));
        }
    }

    @Nested
    @DisplayName("View Profile Tests")
    class ViewProfileTests {

        @Test
        @DisplayName("getUserProfile returns user with signature and roles")
        void getUserProfile_returnsCompleteProfile() throws SQLException {
            User user = new User();
            user.setId(USER_ID);
            user.setFullName("Nguyen Van A");
            user.setEmail("user@example.com");
            user.setPhone("0912345678");
            user.setSignature("Signature");
            user.setRoles(List.of(new Role(1L, "Sales Rep")));

            when(userDAO.findUserProfileWithRoles(any(Connection.class), eq(USER_ID))).thenReturn(user);

            User result = profileService.getUserProfile(USER_ID);
            assertNotNull(result);
            assertEquals("Nguyen Van A", result.getFullName());
            assertEquals("Signature", result.getSignature());
            assertEquals(1, result.getRoles().size());
        }

        @Test
        @DisplayName("getUserProfile with invalid ID returns null")
        void getUserProfile_invalidId_returnsNull() throws SQLException {
            assertNull(profileService.getUserProfile(0L));
            assertNull(profileService.getUserProfile(-1L));
        }
    }
}
