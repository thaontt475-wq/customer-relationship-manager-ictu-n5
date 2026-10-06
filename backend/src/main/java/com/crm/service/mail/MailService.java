package com.crm.service.mail;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class MailService {

    private final String smtpHost;
    private final String smtpPort;
    private final String smtpUsername;
    private final String smtpPassword;
    private final String frontendBaseUrl;

    public MailService() {
        this.smtpHost = getEnv("CRM_SMTP_HOST", "smtp.gmail.com");
        this.smtpPort = getEnv("CRM_SMTP_PORT", "587");
        this.smtpUsername = System.getenv("CRM_SMTP_USERNAME");
        this.smtpPassword = System.getenv("CRM_SMTP_PASSWORD");
        this.frontendBaseUrl = getEnv(
                "CRM_FRONTEND_BASE_URL",
                "http://localhost:5500"
        );
    }

    public void sendPasswordResetEmail(
            String toEmail,
            String rawToken
    ) throws MessagingException {

        validateConfiguration();

        Properties properties = new Properties();

        boolean localReceiver = java.util.Set.of("localhost", "127.0.0.1", "::1").contains(smtpHost);
        boolean authentication = Boolean.parseBoolean(getEnv("CRM_SMTP_AUTH", "true"));
        boolean startTls = Boolean.parseBoolean(getEnv("CRM_SMTP_STARTTLS", "true"));
        if (!localReceiver && (!authentication || !startTls)) {
            throw new IllegalStateException("SMTP ngoài localhost yêu cầu authentication và STARTTLS");
        }
        properties.put("mail.smtp.auth", Boolean.toString(authentication));
        properties.put("mail.smtp.starttls.enable", Boolean.toString(startTls));
        properties.put("mail.smtp.starttls.required", Boolean.toString(startTls));
        properties.put("mail.smtp.host", smtpHost);
        properties.put("mail.smtp.port", smtpPort);

        properties.put(
                "mail.smtp.ssl.trust",
                smtpHost
        );

        properties.put(
                "mail.smtp.connectiontimeout",
                "10000"
        );

        properties.put(
                "mail.smtp.timeout",
                "10000"
        );

        Session session = Session.getInstance(
                properties,
                new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(
                                smtpUsername,
                                smtpPassword
                        );
                    }
                }
        );

        String encodedToken =
                URLEncoder.encode(
                        rawToken,
                        StandardCharsets.UTF_8
                );

        String resetLink =
                frontendBaseUrl
                        + "/reset-password.html?token="
                        + encodedToken;

        MimeMessage message =
                new MimeMessage(session);

        message.setFrom(
                new InternetAddress(
                        getEnv("CRM_SMTP_FROM", smtpUsername),
                        false
                )
        );

        message.setRecipients(
                Message.RecipientType.TO,
                InternetAddress.parse(toEmail)
        );

        message.setSubject(
                "Đặt lại mật khẩu CRM Enterprise",
                StandardCharsets.UTF_8.name()
        );

        String html = """
                <!doctype html>
                <html lang="vi">
                <body style="
                    margin:0;
                    padding:0;
                    background:#eef2f7;
                    font-family:Arial,sans-serif;
                    color:#1e293b;
                ">

                <div style="
                    max-width:560px;
                    margin:40px auto;
                    padding:32px;
                    background:#ffffff;
                    border-radius:18px;
                    box-shadow:0 10px 30px rgba(15,23,42,.08);
                ">

                    <div style="
                        font-size:22px;
                        font-weight:700;
                        margin-bottom:18px;
                    ">
                        CRM Enterprise
                    </div>

                    <h2 style="
                        margin:0 0 12px;
                        font-size:24px;
                    ">
                        Đặt lại mật khẩu
                    </h2>

                    <p style="
                        line-height:1.7;
                        color:#64748b;
                    ">
                        Chúng tôi nhận được yêu cầu đặt lại mật khẩu
                        cho tài khoản CRM của bạn.
                    </p>

                    <p style="
                        line-height:1.7;
                        color:#64748b;
                    ">
                        Nhấn nút bên dưới để tạo mật khẩu mới.
                    </p>

                    <div style="
                        margin:28px 0;
                    ">
                        <a
                            href="%s"
                            style="
                                display:inline-block;
                                padding:14px 24px;
                                background:#2563eb;
                                color:#ffffff;
                                text-decoration:none;
                                border-radius:12px;
                                font-weight:700;
                            "
                        >
                            Đặt lại mật khẩu
                        </a>
                    </div>

                    <p style="
                        line-height:1.7;
                        color:#64748b;
                        font-size:13px;
                    ">
                        Liên kết có hiệu lực trong 30 phút và chỉ
                        được sử dụng một lần.
                    </p>

                    <p style="
                        line-height:1.7;
                        color:#94a3b8;
                        font-size:12px;
                    ">
                        Nếu bạn không yêu cầu đặt lại mật khẩu,
                        hãy bỏ qua email này.
                    </p>

                </div>

                </body>
                </html>
                """.formatted(resetLink);

        message.setContent(
                html,
                "text/html; charset=UTF-8"
        );

Transport.send(message);
    }

    private void validateConfiguration() {

        if (java.util.Set.of("localhost", "127.0.0.1", "::1").contains(smtpHost)
                && "false".equalsIgnoreCase(getEnv("CRM_SMTP_AUTH", "true"))) return;

        if (
                smtpUsername == null ||
                smtpUsername.isBlank()
        ) {
            throw new IllegalStateException(
                    "SMTP_USERNAME chưa được cấu hình"
            );
        }

        if (
                smtpPassword == null ||
                smtpPassword.isBlank()
        ) {
            throw new IllegalStateException(
                    "SMTP_PASSWORD chưa được cấu hình"
            );
        }
    }

    private String getEnv(
            String name,
            String defaultValue
    ) {

        String value =
                System.getenv(name);

        if (
                value == null ||
                value.isBlank()
        ) {
            return defaultValue;
        }

        return value.trim();
    }
}
