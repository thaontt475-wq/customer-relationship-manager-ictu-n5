package com.crm.service.email;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EmailService {
    private static final Logger LOGGER = Logger.getLogger(EmailService.class.getName());

    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final String fromAddress;
    private final String appBaseUrl;

    public EmailService() {
        host = System.getenv("CRM_SMTP_HOST");
        port = parsePort(System.getenv("CRM_SMTP_PORT"));
        username = System.getenv("CRM_SMTP_USERNAME");
        password = System.getenv("CRM_SMTP_PASSWORD");
        fromAddress = System.getenv("CRM_SMTP_FROM");
        appBaseUrl = System.getenv("CRM_APP_BASE_URL");
    }

    public void sendPasswordResetEmail(String toEmail, String rawToken) {
        if (!isConfigured()) {
            LOGGER.warning("Password reset email was not sent because SMTP is not configured");
            return;
        }

        String resetLink = appBaseUrl + "/reset-password?token=" + rawToken;
        String subject = "CRM - Đặt lại mật khẩu";
        StringBuilder sb = new StringBuilder();
        sb.append("Bạn đã yêu cầu đặt lại mật khẩu cho tài khoản của mình.\n\n");
        sb.append("Vui lòng nhấn vào link sau để đặt lại mật khẩu (có hiệu lực trong 15 phút):\n");
        sb.append(resetLink).append("\n\n");
        sb.append("Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.");
        String content = sb.toString();

        try {
            Session session = createSession();
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromAddress));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
            message.setSubject(subject);
            message.setText(content);
            Transport.send(message);
        } catch (MessagingException | RuntimeException e) {
            // Log error without exposing credentials or token
            LOGGER.log(Level.SEVERE, "Failed to send password reset email to " + toEmail, e);
        }
    }

    public void sendAccountActivationEmail(
            String toEmail,
            String fullName,
            String username,
            String temporaryPassword) {

        if (!isConfigured()) {
            LOGGER.warning("Account activation email was not sent because SMTP is not configured");
            return;
        }

        String loginLink = appBaseUrl + "/login";
        String subject = "CRM - Kích hoạt tài khoản";

        StringBuilder content = new StringBuilder();
        content.append("Xin chào ")
                .append(fullName == null || fullName.isBlank() ? "bạn" : fullName)
                .append(",\n\n");
        content.append("Tài khoản CRM của bạn đã được tạo.\n\n");
        content.append("Tên đăng nhập: ").append(username).append("\n");
        content.append("Mật khẩu tạm: ").append(temporaryPassword).append("\n\n");
        content.append("Đăng nhập tại: ").append(loginLink).append("\n\n");
        content.append("Vui lòng đổi mật khẩu sau khi đăng nhập.");

        try {
            Session session = createSession();
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromAddress));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
            message.setSubject(subject);
            message.setText(content.toString());
            Transport.send(message);
        } catch (MessagingException | RuntimeException ex) {
            LOGGER.log(Level.SEVERE, "Failed to send account activation email to " + toEmail, ex);
        }
    }
    private Session createSession() {
        Properties props = new Properties();
        boolean usesAuthentication = !isBlank(username) && !isBlank(password);
        props.put("mail.smtp.auth", String.valueOf(usesAuthentication));
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", String.valueOf(port));
        if (!usesAuthentication) {
            return Session.getInstance(props);
        }

        Authenticator auth = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        };
        return Session.getInstance(props, auth);
    }

    private boolean isConfigured() {
        return !isBlank(host) && !isBlank(fromAddress) && !isBlank(appBaseUrl);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static int parsePort(String configuredPort) {
        if (isBlank(configuredPort)) {
            return 25;
        }
        try {
            return Integer.parseInt(configuredPort);
        } catch (NumberFormatException e) {
            return 25;
        }
    }
}
