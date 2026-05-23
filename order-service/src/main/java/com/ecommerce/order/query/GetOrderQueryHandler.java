package com.ecommerce.order.query;

import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.entity.OrderReadModel;
import com.ecommerce.order.repository.OrderReadModelRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetOrderQueryHandler {

    private final OrderReadModelRepository readModelRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID orderId) {
        OrderReadModel model = readModelRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        return toResponse(model);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getUserOrders(UUID userId, Pageable pageable) {
        return readModelRepository.findByUserId(userId, pageable).map(this::toResponse);
    }

    private OrderResponse toResponse(OrderReadModel m) {
        Object items;
        try {
            items = objectMapper.readValue(m.getItemsJson(), Object.class);
        } catch (Exception e) {
            items = m.getItemsJson();
        }
        return OrderResponse.builder()
            .id(m.getId())
            .userId(m.getUserId())
            .status(m.getStatus().name())
            .total(m.getTotal())
            .items(items)
            .createdAt(m.getCreatedAt())
            .updatedAt(m.getUpdatedAt())
            .build();
    }
}
