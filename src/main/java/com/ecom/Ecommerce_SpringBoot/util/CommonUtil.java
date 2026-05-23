package com.ecom.Ecommerce_SpringBoot.util;

import com.ecom.Ecommerce_SpringBoot.entities.ProductOrder;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class CommonUtil {

    private static final Logger log = LoggerFactory.getLogger(CommonUtil.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    /** Logs the "mail disabled" notice once at first send-attempt, then stays quiet. */
    private final AtomicBoolean disabledNoticeLogged = new AtomicBoolean(false);

    /**
     * Mail is considered configured when both username and password are present
     * and neither is one of the documented placeholder values from
     * {@code .env.production.example}. If mail isn't configured the public
     * {@code sendMail}/{@code sendOrderMail} methods just no-op so the app
     * keeps working without SMTP credentials.
     */
    public boolean isMailConfigured() {
        if (mailUsername == null || mailUsername.isBlank()) return false;
        if (mailPassword == null || mailPassword.isBlank()) return false;
        if ("changeme".equalsIgnoreCase(mailPassword)) return false;
        if (mailUsername.toLowerCase().endsWith("@example.com")) return false;
        if (mailUsername.toLowerCase().endsWith("@example.local")) return false;
        return true;
    }

    private void noteMailSkipped(String reason) {
        if (disabledNoticeLogged.compareAndSet(false, true)) {
            log.info("Mail not configured (spring.mail.username/password missing or placeholder). " +
                    "Skipping all outbound mail. Reason: {}", reason);
        }
    }

    public Boolean sendMail(String url, String recipientEmail) throws UnsupportedEncodingException, MessagingException {
        if (!isMailConfigured()) {
            noteMailSkipped("password-reset request for " + recipientEmail);
            return false;
        }

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);
        helper.setFrom("noreply@7eleven-testing-round.local", "7-Eleven Testing Round");
        helper.setTo(recipientEmail);

        String content = "<p>Hello,</p>" + "<p>You have requested to reset your password.</p>"
                + "<p>Click the link below to change your password:</p>" + "<p><a href=\"" + url
                + "\">Change my password</a></p>";

        helper.setSubject("Password Reset");
        helper.setText(content, true);
        mailSender.send(mimeMessage);

        return true;
    }

    public static String generateUrl(HttpServletRequest request) {

        // http://localhost:8080/forgot-password
        String siteUrl = request.getRequestURL().toString();

        return siteUrl.replace(request.getServletPath(), "");
    }

    public Boolean sendOrderMail(ProductOrder productOrder, String codeStatus) throws UnsupportedEncodingException, MessagingException {
        if (!isMailConfigured()) {
            noteMailSkipped("order confirmation for " + productOrder.getOrderId());
            return false;
        }

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);
        helper.setFrom("noreply@7eleven-testing-round.local", "7-Eleven Testing Round");
        helper.setTo(productOrder.getAddressOrder().getEmail());

        String content = "<p>Hello [[name]]</p>,<p>Thanks order successfully <b>[[orderStatus]]</b>.</p>" +
                "<p><b>Product Details</b> :</p>"
                + "<p>Name : [[productName]]</p>"
                + "<p>Category : [[category]]</p>"
                + "<p>Quantity : [[quantity]]</p>"
                + "<p>Price : [[price]]</p>"
                + "<p>Payment Type : [[paymentType]]</p>";

        content = content.replace("[[name]]", productOrder.getAddressOrder().getFirstName());
        content = content.replace("[[orderStatus]]", codeStatus);
        content = content.replace("[[productName]]", productOrder.getProduct().getTitle());
        content = content.replace("[[category]]", productOrder.getProduct().getCategory());
        content = content.replace("[[quantity]]", String.valueOf(productOrder.getQuantity()));
        content = content.replace("[[price]]", String.valueOf(productOrder.getPrice()));
        content = content.replace("[[paymentType]]", productOrder.getPaymentType());

        helper.setSubject("Order Product Status");
        helper.setText(content, true);
        mailSender.send(mimeMessage);

        return true;
    }
}
