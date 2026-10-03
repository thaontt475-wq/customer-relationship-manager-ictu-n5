package com.crm.controller.users;

import com.crm.model.Role;
import com.crm.model.User;
import com.crm.service.users.ProfileService;
import com.crm.util.SessionKey;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Controller integration tests for CRM-35 ProfileServlet:
 * 1. Authentication requirements (GET/POST)
 * 2. CSRF verification on form submissions
 * 3. Identity binding to session (anti-IDOR)
 * 4. Immutable fields (email, team, role, data_scope)
 * 5. Session synchronization on successful update
 * 6. Forwarding on validation error
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProfileServletTest {

    private static final long ACTOR_USER_ID = 42L;

    @Mock
    private ProfileService profileService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private HttpSession session;

    @Mock
    private RequestDispatcher requestDispatcher;

    private ProfileServlet servlet;

    @BeforeEach
    void setUp() {
        servlet = new ProfileServlet(profileService);
    }

    @Nested
    @DisplayName("GET /profile Tests")
    class GetProfileTests {

        @Test
        @DisplayName("Unauthenticated GET /profile redirects to /login?expired=1")
        void unauthenticatedGet_redirectsToLogin() throws Exception {
            when(request.getSession(false)).thenReturn(null);
            when(request.getContextPath()).thenReturn("/crm");
            when(request.getServletPath()).thenReturn("/profile");

            servlet.doGet(request, response);

            verify(response).sendRedirect("/crm/login?expired=1");
            verifyNoInteractions(profileService);
        }

        @Test
        @DisplayName("Authenticated GET /profile forwards to profile.jsp with user attributes")
        void authenticatedGet_forwardsToJsp() throws Exception {
            User user = new User();
            user.setId(ACTOR_USER_ID);
            user.setFullName("Nguyen Van A");
            user.setEmail("a@crm.vn");
            user.setSignature("Signature A");
            user.setRoles(List.of(new Role(1L, "Sales Rep")));

            when(request.getSession(false)).thenReturn(session);
            when(session.getAttribute(SessionKey.CURRENT_USER)).thenReturn(user);
            when(request.getServletPath()).thenReturn("/profile");
            when(profileService.getUserProfile(ACTOR_USER_ID)).thenReturn(user);
            when(request.getRequestDispatcher("/jsp/users/profile.jsp")).thenReturn(requestDispatcher);

            servlet.doGet(request, response);

            verify(request).setAttribute(eq("profileUser"), eq(user));
            verify(request).setAttribute(eq("signature"), eq("Signature A"));
            verify(requestDispatcher).forward(request, response);
        }
    }

    @Nested
    @DisplayName("POST /profile Form Tests")
    class PostProfileFormTests {

        private static final String CSRF_SESSION_TOKEN = "valid-csrf-token-1234";

        @BeforeEach
        void setupSession() {
            when(request.getSession(false)).thenReturn(session);
            User actor = new User();
            actor.setId(ACTOR_USER_ID);
            actor.setEmail("actor@crm.vn");
            actor.setFullName("Original Name");
            when(session.getAttribute(SessionKey.CURRENT_USER)).thenReturn(actor);
        }

        @Test
        @DisplayName("POST /profile with valid CSRF updates profile and synchronizes session")
        void validCsrf_updatesProfile_andSyncsSession() throws Exception {
            when(session.getAttribute("htmlFormToken")).thenReturn(CSRF_SESSION_TOKEN);
            when(request.getParameter("csrfToken")).thenReturn(CSRF_SESSION_TOKEN);
            when(request.getParameter("fullName")).thenReturn("Updated Full Name");
            when(request.getParameter("phone")).thenReturn("0912345678");
            when(request.getParameter("signature")).thenReturn("New Signature");
            when(request.getContextPath()).thenReturn("/crm");
            when(request.getServletPath()).thenReturn("/profile");

            User updated = new User();
            updated.setId(ACTOR_USER_ID);
            updated.setFullName("Updated Full Name");
            updated.setPhone("0912345678");
            updated.setSignature("New Signature");

            when(profileService.updateProfile(ACTOR_USER_ID, "Updated Full Name", "0912345678", "New Signature"))
                    .thenReturn(updated);

            servlet.doPost(request, response);

            verify(profileService).updateProfile(ACTOR_USER_ID, "Updated Full Name", "0912345678", "New Signature");
            verify(session).setAttribute(eq(SessionKey.CURRENT_USER), eq(updated));
            verify(session).setAttribute(eq("displayName"), eq("Updated Full Name"));
            verify(response).sendRedirect("/crm/profile?updated=1");
        }

        @Test
        @DisplayName("POST /profile with missing/invalid CSRF sends 403 Forbidden")
        void invalidCsrf_sends403Forbidden() throws Exception {
            when(session.getAttribute("htmlFormToken")).thenReturn(CSRF_SESSION_TOKEN);
            when(request.getParameter("csrfToken")).thenReturn("wrong-or-missing-csrf");
            when(request.getServletPath()).thenReturn("/profile");

            servlet.doPost(request, response);

            verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
            verifyNoInteractions(profileService);
        }

        @Test
        @DisplayName("POST /profile with empty fullName forwards back to JSP with error message")
        void emptyFullName_forwardsWithError() throws Exception {
            when(session.getAttribute("htmlFormToken")).thenReturn(CSRF_SESSION_TOKEN);
            when(request.getParameter("csrfToken")).thenReturn(CSRF_SESSION_TOKEN);
            when(request.getParameter("fullName")).thenReturn("");
            when(request.getParameter("phone")).thenReturn("0912345678");
            when(request.getParameter("signature")).thenReturn("Sig");
            when(request.getServletPath()).thenReturn("/profile");

            when(profileService.updateProfile(eq(ACTOR_USER_ID), eq(""), anyString(), anyString()))
                    .thenThrow(new IllegalArgumentException("Họ và tên không được để trống."));
            when(request.getRequestDispatcher("/jsp/users/profile.jsp")).thenReturn(requestDispatcher);

            servlet.doPost(request, response);

            verify(request).setAttribute(eq("error"), eq("Họ và tên không được để trống."));
            verify(requestDispatcher).forward(request, response);
        }

        @Test
        @DisplayName("POST /profile ignores any submitted userId, email, team_id, and role parameters (anti-IDOR / anti-MassAssignment)")
        void parameterTampering_ignoredByBackend() throws Exception {
            when(session.getAttribute("htmlFormToken")).thenReturn(CSRF_SESSION_TOKEN);
            when(request.getParameter("csrfToken")).thenReturn(CSRF_SESSION_TOKEN);

            // Attacker attempts to modify another user's profile and elevate privileges
            when(request.getParameter("userId")).thenReturn("9999");
            when(request.getParameter("email")).thenReturn("hacked@evil.com");
            when(request.getParameter("team_id")).thenReturn("555");
            when(request.getParameter("role")).thenReturn("Admin");
            when(request.getParameter("fullName")).thenReturn("Tamper Name");
            when(request.getParameter("phone")).thenReturn("0912345678");
            when(request.getParameter("signature")).thenReturn("Sig");
            when(request.getContextPath()).thenReturn("/crm");
            when(request.getServletPath()).thenReturn("/profile");

            User updated = new User();
            updated.setId(ACTOR_USER_ID);
            updated.setFullName("Tamper Name");

            when(profileService.updateProfile(ACTOR_USER_ID, "Tamper Name", "0912345678", "Sig"))
                    .thenReturn(updated);

            servlet.doPost(request, response);

            // Verify updateProfile was called STRICTLY with actorUserId from session, not 9999
            verify(profileService).updateProfile(eq(ACTOR_USER_ID), eq("Tamper Name"), eq("0912345678"), eq("Sig"));
            verify(profileService, never()).updateProfile(eq(9999L), anyString(), anyString(), anyString());
        }
    }
}
