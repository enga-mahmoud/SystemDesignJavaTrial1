package com.ecommerce.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendWelcomeEmail(String to, String name) {
        log.info("SENDING EMAIL [welcome] to={} subject='Welcome to EShop, {}!'", to, name);
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(to);
            msg.setFrom("noreply@eshop.com");
            msg.setSubject("Welcome to EShop, " + name + "!");
            msg.setText("Hi " + name + ",\n\nThank you for registering with EShop!\n\nHappy shopping!\nThe EShop Team");
            mailSender.send(msg);
        } catch (Exception e) {
            log.warn("Mail server unavailable, email logged only: welcome email to {}", to);
        }
    }

    public void sendOrderConfirmedEmail(String to, String orderId, String amount) {
        log.info("SENDING EMAIL [order.confirmed] to={} orderId={} amount={}", to, orderId, amount);
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(to);
            msg.setFrom("noreply@eshop.com");
            msg.setSubject("Order Confirmed - #" + orderId);
            msg.setText("Your order #" + orderId + " has been confirmed!\n\nTotal: $" + amount
                    + "\n\nThank you for shopping with EShop!");
            mailSender.send(msg);
        } catch (Exception e) {
            log.warn("Mail server unavailable, email logged only: order confirmed to {}", to);
        }
    }

    public void sendOrderCancelledEmail(String to, String orderId) {
        log.info("SENDING EMAIL [order.cancelled] to={} orderId={}", to, orderId);
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(to);
            msg.setFrom("noreply@eshop.com");
            msg.setSubject("Order Cancelled - #" + orderId);
            msg.setText("Your order #" + orderId + " has been cancelled.\n\n"
                    + "If you have questions, contact support@eshop.com\n\nEShop Team");
            mailSender.send(msg);
        } catch (Exception e) {
            log.warn("Mail server unavailable, email logged only: order cancelled to {}", to);
        }
    }

    public void sendPaymentCompletedEmail(String to, String orderId, String amount) {
        log.info("SENDING EMAIL [payment.completed] to={} orderId={} amount={}", to, orderId, amount);
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(to);
            msg.setFrom("noreply@eshop.com");
            msg.setSubject("Payment Received - Order #" + orderId);
            msg.setText("Payment of $" + amount + " received for order #" + orderId + ".\n\n"
                    + "Your order is being processed.\n\nEShop Team");
            mailSender.send(msg);
        } catch (Exception e) {
            log.warn("Mail server unavailable, email logged only: payment confirmed to {}", to);
        }
    }

    public void sendPaymentFailedEmail(String to, String orderId, String reason) {
        log.info("SENDING EMAIL [payment.failed] to={} orderId={} reason={}", to, orderId, reason);
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(to);
            msg.setFrom("noreply@eshop.com");
            msg.setSubject("Payment Failed - Order #" + orderId);
            msg.setText("Payment for order #" + orderId + " failed.\n\nReason: " + reason + "\n\n"
                    + "Please try again or contact support@eshop.com\n\nEShop Team");
            mailSender.send(msg);
        } catch (Exception e) {
            log.warn("Mail server unavailable, email logged only: payment failed to {}", to);
        }
    }
}
