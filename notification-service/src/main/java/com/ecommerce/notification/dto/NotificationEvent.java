package com.ecommerce.notification.dto;

import lombok.Data;

@Data
public class NotificationEvent {
    private String eventType;
    private String userId;
    private String email;
    private String orderId;
    private String amount;
    private String failureReason;
    private String productName;
}
