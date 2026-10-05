package com.crm.service.email;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.InputStream;
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
        Properties fileProps = loadPropertiesFile();

        host = resolveConfig("CRM_SMTP_HOST", "mail.smtp.host", fileProps, "");
        port = parsePort(resolveConfig("CRM_SMTP_PORT", "mail.smtp.port", fileProps, "587"));
        username = resolveConfig("CRM_SMTP_USERNAME", "mail.smtp.username", fileProps, "");

        String rawPassword = resolveConfig("CRM_SMTP_PASSWORD", "mail.smtp.password", fileProps, "");
        if (rawPassword != null && host != null && host.contains("gmail")) {
            rawPassword = rawPassword.replace(" ", "").trim();
        }
        password = rawPassword != null ? rawPassword.trim() : "";

        String rawFrom = resolveConfig("CRM_SMTP_FROM", "mail.smtp.from", fileProps, "");
        if (isBlank(rawFrom) && !isBlank(username) && username.contains("@")) {
            rawFrom = username;
        }
        fromAddress = rawFrom;

        appBaseUrl = resolveAppBaseUrl(fileProps);
    }

    private static Properties loadPropertiesFile() {
        Properties props = new Properties();
        try (InputStream is = EmailService.class.getResourceAsStream("/email.properties")) {
            if (is != null) {
                props.load(is);
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Could not load /email.properties from classpath", e);
        }
        return props;
    }

    private static String resolveConfig(String envOrPropKey, String fileKey, Properties fileProps, String defaultValue) {
        String val = System.getProperty(envOrPropKey);
        if (isBlank(val)) {
            val = System.getenv(envOrPropKey);
        }
        if (isBlank(val) && fileProps != null && fileKey != null) {
            val = fileProps.getProperty(fileKey);
        }
        return isBlank(val) ? defaultValue : val.trim();
    }

    private static String resolveAppBaseUrl(Properties fileProps) {
        String configured = resolveConfig("CRM_APP_BASE_URL", "app.base.url", fileProps, "http://localhost:8080");
        configured = configured.trim();
        while (configured.endsWith("/")) {
            configured = configured.substring(0, configured.length() - 1);
        }
        return configured;
    }

    public EmailSendResult sendPasswordResetEmail(String toEmail, String rawToken) {
        String resetLink = appBaseUrl + "/reset-password?token=" + rawToken;
        String subject = "CRM - Đặt lại mật khẩu";
        String content = buildPasswordResetEmailContent(resetLink);

        if (!isConfigured()) {
            LOGGER.warning("Password reset email was not sent because SMTP is not configured. Direct reset link generated: " + resetLink);
            return new EmailSendResult(
                    false,
                    "Dịch vụ gửi mail (SMTP) chưa được cấu hình. Hệ thống đã tạo liên kết đặt lại mật khẩu trực tiếp bên dưới.",
                    resetLink,
                    subject,
                    content,
                    toEmail
            );
        }

        try {
            Session session = createSession();
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromAddress));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
            message.setSubject(subject);
            message.setText(content);
            Transport.send(message);

            LOGGER.info("Successfully sent password reset email to " + toEmail + " with reset base URL: " + appBaseUrl);
            return new EmailSendResult(
                    true,
                    "Email chứa liên kết đặt lại mật khẩu đã được gửi thành công đến " + toEmail,
                    resetLink,
                    subject,
                    content,
                    toEmail
            );
        } catch (MessagingException | RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Failed to send password reset email to " + toEmail, e);
            return new EmailSendResult(
                    false,
                    "Không thể kết nối máy chủ gửi mail SMTP (" + e.getMessage() + "). Vui lòng sử dụng liên kết trực tiếp bên dưới.",
                    resetLink,
                    subject,
                    content,
                    toEmail
            );
        }
    }

    public static String buildPasswordResetEmailContent(String resetLink) {
        StringBuilder content = new StringBuilder();
        content.append("Bạn đã yêu cầu đặt lại mật khẩu cho tài khoản của mình.\n\n");
        content.append("Vui lòng nhấn vào liên kết sau để đặt lại mật khẩu:\n");
        content.append(resetLink).append("\n\n");
        content.append("Liên kết có hiệu lực trong 30 phút.\n\n");
        content.append("Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.");
        return content.toString();
    }

    public EmailSendResult sendPasswordChangeNotificationEmail(String toEmail, String fullName) {
        String subject = "CRM - Thông báo thay đổi mật khẩu thành công";
        StringBuilder content = new StringBuilder();
        content.append("Xin chào ").append(fullName == null || fullName.isBlank() ? "bạn" : fullName).append(",\n\n");
        content.append("Mật khẩu tài khoản CRM (").append(toEmail).append(") của bạn đã được thay đổi thành công vào lúc: ")
               .append(java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")))
               .append(".\n\n");
        content.append("Nếu bạn không thực hiện thay đổi này, vui lòng liên hệ ngay với ban quản trị hoặc đặt lại mật khẩu để bảo vệ tài khoản.\n\n");
        content.append("Trân trọng,\nĐội ngũ CRM");

        if (!isConfigured()) {
            LOGGER.warning("Password change notification email was not sent to " + toEmail + " because SMTP is not configured. Content:\n" + content);
            return new EmailSendResult(
                    false,
                    "Dịch vụ gửi mail (SMTP) chưa cấu hình. Hệ thống đã ghi nhận việc đổi mật khẩu cho " + toEmail,
                    null,
                    subject,
                    content.toString(),
                    toEmail
            );
        }

        try {
            Session session = createSession();
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromAddress));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
            message.setSubject(subject);
            message.setText(content.toString());
            Transport.send(message);

            LOGGER.info("Successfully sent password change confirmation email to " + toEmail);
            return new EmailSendResult(
                    true,
                    "Đã gửi email thông báo đổi mật khẩu thành công đến " + toEmail,
                    null,
                    subject,
                    content.toString(),
                    toEmail
            );
        } catch (MessagingException | RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Failed to send password change confirmation email to " + toEmail, e);
            return new EmailSendResult(
                    false,
                    "Không thể gửi email xác nhận qua SMTP (" + e.getMessage() + ")",
                    null,
                    subject,
                    content.toString(),
                    toEmail
            );
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
        content.append("Email đăng nhập: ").append(toEmail).append("\n");
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
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3");
        props.put("mail.smtp.ssl.trust", host);
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", String.valueOf(port));
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");

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

    public boolean isConfigured() {
        if (isBlank(host) || isBlank(fromAddress) || isBlank(appBaseUrl)) {
            return false;
        }
        if ("smtp.gmail.com".equalsIgnoreCase(host) && (isBlank(username) || isBlank(password))) {
            return false;
        }
        return true;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static int parsePort(String configuredPort) {
        if (isBlank(configuredPort)) {
            return 587;
        }
        try {
            return Integer.parseInt(configuredPort.trim());
        } catch (NumberFormatException e) {
            return 587;
        }
    }
}

