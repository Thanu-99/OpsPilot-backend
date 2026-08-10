package com.opspilot.opspilotbackend.service;

import com.opspilot.opspilotbackend.dto.OrderRequestDto;
import com.opspilot.opspilotbackend.dto.OrderResponseDto;
import com.opspilot.opspilotbackend.entity.OrderStatus;

import java.util.List;

public interface OrderService {

    OrderResponseDto createOrder(OrderRequestDto request);

    List<OrderResponseDto> getAllOrders();

    OrderResponseDto getOrderById(Long id);

    OrderResponseDto updateOrder(Long id, OrderRequestDto request);

    OrderResponseDto updateOrderStatus(Long id, OrderStatus status);

    void deleteOrder(Long id);
}