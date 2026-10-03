package com.crm.controller.users;

import com.crm.model.User;
import com.crm.model.UserAvatar;
import com.crm.service.users.AvatarException;
import com.crm.service.users.AvatarService;
import com.crm.util.SessionKey;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AvatarServletTest {

    private static final long ACTOR_USER_ID = 55L;
    private static final String CSRF_TOKEN = "valid-token-avatar-12345";

    @Mock
    private AvatarService avatarService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private HttpSession session;

    @Mock
    private RequestDispatcher requestDispatcher;

    @Mock
    private Part filePart;

    private AvatarServlet servlet;
    private StringWriter stringWriter;
    private PrintWriter printWriter;

    @BeforeEach
    void setUp() throws IOException {
        servlet = new AvatarServlet(avatarService);
        stringWriter = new StringWriter();
        printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);
        when(request.getRequestDispatcher(anyString())).thenReturn(requestDispatcher);
    }

    private void mockAuthenticatedUser() {
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("userId")).thenReturn(ACTOR_USER_ID);
        when(session.getAttribute("htmlFormToken")).thenReturn(CSRF_TOKEN);
    }

    @Nested
    @DisplayName("GET /profile/avatar Tests")
    class GetAvatarTests {

        @Test
        @DisplayName("Unauthenticated GET /profile/avatar redirects to /login?expired=1")
        void unauthenticatedGet_redirectsToLogin() throws Exception {
            when(request.getSession(false)).thenReturn(null);
            when(request.getContextPath()).thenReturn("/crm");
            when(request.getServletPath()).thenReturn("/profile/avatar");

            servlet.doGet(request, response);

            verify(response).sendRedirect("/crm/login?expired=1");
            verifyNoInteractions(avatarService);
        }

        @Test
        @DisplayName("Authenticated GET /profile/avatar forwards to JSP with CSRF token and hasAvatar state")
        void authenticatedGet_forwardsToJsp() throws Exception {
            mockAuthenticatedUser();
            when(request.getServletPath()).thenReturn("/profile/avatar");
            when(request.getRequestDispatcher("/jsp/users/avatar.jsp")).thenReturn(requestDispatcher);
            when(avatarService.find(ACTOR_USER_ID)).thenReturn(new UserAvatar("img.png", "thumb.png"));

            servlet.doGet(request, response);

            verify(request).setAttribute(eq("hasAvatar"), eq(true));
            verify(request).setAttribute(eq("csrfToken"), eq(CSRF_TOKEN));
            verify(requestDispatcher).forward(request, response);
        }

        @Test
        @DisplayName("GET /profile/avatar/image serves PNG bytes with security header nosniff")
        void getImage_servesPngWithHeaders() throws Exception {
            mockAuthenticatedUser();
            when(request.getServletPath()).thenReturn("/profile/avatar/image");
            byte[] fakeImage = new byte[]{1, 2, 3, 4};
            when(avatarService.read(ACTOR_USER_ID, false)).thenReturn(fakeImage);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ServletOutputStream servletOutputStream = new ServletOutputStream() {
                @Override public boolean isReady() { return true; }
                @Override public void setWriteListener(jakarta.servlet.WriteListener writeListener) { }
                @Override public void write(int b) { outputStream.write(b); }
                @Override public void write(byte[] b) throws IOException { outputStream.write(b); }
            };
            when(response.getOutputStream()).thenReturn(servletOutputStream);

            servlet.doGet(request, response);

            verify(response).setContentType("image/png");
            verify(response).setHeader("X-Content-Type-Options", "nosniff");
            verify(response).setContentLength(fakeImage.length);
            assertArrayEquals(fakeImage, outputStream.toByteArray());
        }

        @Test
        @DisplayName("GET /profile/avatar/thumbnail serves thumbnail PNG bytes")
        void getThumbnail_servesThumbnailBytes() throws Exception {
            mockAuthenticatedUser();
            when(request.getServletPath()).thenReturn("/profile/avatar/thumbnail");
            byte[] fakeThumb = new byte[]{5, 6, 7, 8};
            when(avatarService.read(ACTOR_USER_ID, true)).thenReturn(fakeThumb);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ServletOutputStream servletOutputStream = new ServletOutputStream() {
                @Override public boolean isReady() { return true; }
                @Override public void setWriteListener(jakarta.servlet.WriteListener writeListener) { }
                @Override public void write(int b) { outputStream.write(b); }
                @Override public void write(byte[] b) throws IOException { outputStream.write(b); }
            };
            when(response.getOutputStream()).thenReturn(servletOutputStream);

            servlet.doGet(request, response);

            verify(response).setContentType("image/png");
            verify(response).setHeader("X-Content-Type-Options", "nosniff");
            assertArrayEquals(fakeThumb, outputStream.toByteArray());
        }

        @Test
        @DisplayName("GET /profile/avatar/image returns 404 when user has no avatar")
        void getImage_notFound_returns404() throws Exception {
            mockAuthenticatedUser();
            when(request.getServletPath()).thenReturn("/profile/avatar/image");
            when(avatarService.read(ACTOR_USER_ID, false)).thenReturn(null);

            servlet.doGet(request, response);

            verify(response).setStatus(404);
        }

        @Test
        @DisplayName("GET /api/users/me/avatar returns JSON metadata with csrfToken and URLs")
        void getApiMetadata_returnsJson() throws Exception {
            mockAuthenticatedUser();
            when(request.getServletPath()).thenReturn("/api/users/me/avatar");
            when(request.getContextPath()).thenReturn("/crm");
            when(avatarService.find(ACTOR_USER_ID)).thenReturn(new UserAvatar("img.png", "thumb.png"));

            servlet.doGet(request, response);

            verify(response).setStatus(200);
            verify(response).setContentType("application/json;charset=UTF-8");
            String body = stringWriter.toString();
            assertTrue(body.contains("\"success\":true"));
            assertTrue(body.contains("/crm/profile/avatar/image"));
        }

        @Test
        @DisplayName("GET /api/users/me/avatar unauthenticated returns 401 JSON")
        void unauthenticatedApiGet_returns401() throws Exception {
            when(request.getSession(false)).thenReturn(null);
            when(request.getServletPath()).thenReturn("/api/users/me/avatar");
            when(request.getContextPath()).thenReturn("/crm");

            servlet.doGet(request, response);

            verify(response).setStatus(401);
            verify(response).setContentType("application/json;charset=UTF-8");
            String body = stringWriter.toString();
            assertTrue(body.contains("\"success\":false"));
        }
    }

    @Nested
    @DisplayName("POST /profile/avatar Tests")
    class PostAvatarTests {

        @Test
        @DisplayName("POST with non-multipart Content-Type returns 415 Unsupported Media Type")
        void nonMultipart_returns415() throws Exception {
            mockAuthenticatedUser();
            when(request.getServletPath()).thenReturn("/profile/avatar");
            when(request.getContentType()).thenReturn("application/x-www-form-urlencoded");

            servlet.doPost(request, response);

            verify(response).setStatus(415);
        }

        @Test
        @DisplayName("POST with missing/invalid CSRF returns 403 Forbidden")
        void invalidCsrf_returns403() throws Exception {
            mockAuthenticatedUser();
            when(request.getServletPath()).thenReturn("/profile/avatar");
            when(request.getContentType()).thenReturn("multipart/form-data; boundary=---123");
            when(request.getParameter("csrfToken")).thenReturn("wrong-csrf-token");
            when(request.getRequestDispatcher("/jsp/users/avatar.jsp")).thenReturn(requestDispatcher);

            servlet.doPost(request, response);

            verify(response).setStatus(403);
            verifyNoInteractions(avatarService);
        }

        @Test
        @DisplayName("POST /profile/avatar with valid file and CSRF uploads and redirects")
        void validAvatarUpload_redirectsSuccess() throws Exception {
            mockAuthenticatedUser();
            when(request.getServletPath()).thenReturn("/profile/avatar");
            when(request.getContextPath()).thenReturn("/crm");
            when(request.getContentType()).thenReturn("multipart/form-data; boundary=---123");
            when(request.getParameter("csrfToken")).thenReturn(CSRF_TOKEN);

            when(filePart.getName()).thenReturn("avatar");
            when(filePart.getSubmittedFileName()).thenReturn("photo.jpg");
            when(filePart.getSize()).thenReturn(1024L);
            when(filePart.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[]{1, 2, 3}));
            doReturn(List.of(filePart)).when(request).getParts();

            when(avatarService.upload(eq(ACTOR_USER_ID), any(InputStream.class), eq("photo.jpg"), eq(1024L)))
                    .thenReturn(new UserAvatar("new.png", "new-thumb.png"));

            servlet.doPost(request, response);

            verify(avatarService).upload(eq(ACTOR_USER_ID), any(InputStream.class), eq("photo.jpg"), eq(1024L));
            verify(response).sendRedirect("/crm/profile/avatar?updated=1");
        }

        @Test
        @DisplayName("POST /api/users/me/avatar returns 200 JSON on successful upload")
        void validApiUpload_returns200Json() throws Exception {
            mockAuthenticatedUser();
            when(request.getServletPath()).thenReturn("/api/users/me/avatar");
            when(request.getContextPath()).thenReturn("/crm");
            when(request.getContentType()).thenReturn("multipart/form-data; boundary=---123");
            when(request.getHeader("X-CSRF-Token")).thenReturn(CSRF_TOKEN);

            when(filePart.getName()).thenReturn("avatar");
            when(filePart.getSubmittedFileName()).thenReturn("avatar.png");
            when(filePart.getSize()).thenReturn(2048L);
            when(filePart.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[]{4, 5, 6}));
            doReturn(List.of(filePart)).when(request).getParts();

            when(avatarService.upload(eq(ACTOR_USER_ID), any(InputStream.class), eq("avatar.png"), eq(2048L)))
                    .thenReturn(new UserAvatar("api.png", "api-thumb.png"));

            servlet.doPost(request, response);

            verify(response).setStatus(200);
            verify(response).setContentType("application/json;charset=UTF-8");
            String body = stringWriter.toString();
            assertTrue(body.contains("\"success\":true"));
        }

        @Test
        @DisplayName("POST with file size exceeding limit throws IllegalStateException and returns 413 Payload Too Large")
        void oversizedFile_returns413() throws Exception {
            mockAuthenticatedUser();
            when(request.getServletPath()).thenReturn("/profile/avatar");
            when(request.getContentType()).thenReturn("multipart/form-data; boundary=---123");
            when(request.getRequestDispatcher("/jsp/users/avatar.jsp")).thenReturn(requestDispatcher);
            when(request.getParts()).thenThrow(new IllegalStateException("File size exceeds configured maximum"));

            servlet.doPost(request, response);

            verify(response).setStatus(413);
        }

        @Test
        @DisplayName("POST with AvatarException returns the exception status and error message")
        void avatarException_returnsErrorStatusAndMessage() throws Exception {
            mockAuthenticatedUser();
            when(request.getServletPath()).thenReturn("/profile/avatar");
            when(request.getContentType()).thenReturn("multipart/form-data; boundary=---123");
            when(request.getParameter("csrfToken")).thenReturn(CSRF_TOKEN);
            when(request.getRequestDispatcher("/jsp/users/avatar.jsp")).thenReturn(requestDispatcher);

            when(filePart.getName()).thenReturn("avatar");
            when(filePart.getSubmittedFileName()).thenReturn("malformed.jpg");
            when(filePart.getSize()).thenReturn(500L);
            when(filePart.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[]{0, 0}));
            doReturn(List.of(filePart)).when(request).getParts();

            when(avatarService.upload(eq(ACTOR_USER_ID), any(InputStream.class), eq("malformed.jpg"), eq(500L)))
                    .thenThrow(new AvatarException(400, "File phải là ảnh JPG/JPEG hoặc PNG hợp lệ."));

            servlet.doPost(request, response);

            verify(response).setStatus(400);
            verify(request).setAttribute(eq("message"), eq("File phải là ảnh JPG/JPEG hoặc PNG hợp lệ."));
        }
    }
}
