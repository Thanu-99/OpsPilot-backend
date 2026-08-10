package com.opspilot.opspilotbackend.mapper;

import com.opspilot.opspilotbackend.dto.OrderItemResponseDto;
import com.opspilot.opspilotbackend.dto.OrderResponseDto;
import com.opspilot.opspilotbackend.entity.Order;
import com.opspilot.opspilotbackend.entity.OrderItem;

import java.util.List;

public class OrderMapper {

    public static OrderItemResponseDto toItemResponse(OrderItem item) {

        return OrderItemResponseDto.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .subtotal(item.getSubtotal())
                .build();
    }

    public static OrderResponseDto toResponse(
            Order order,
            List<OrderItem> items) {

        return OrderResponseDto.builder()
                .id(order.getId())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .items(
                        items.stream()
                                .map(OrderMapper::toItemResponse)
                                .toList()
                )
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}