package com.opspilot.opspilotbackend.controller.order;
import jakarta.validation.Valid;
import com.opspilot.opspilotbackend.dto.OrderRequestDto;
import com.opspilot.opspilotbackend.dto.OrderResponseDto;
import com.opspilot.opspilotbackend.entity.OrderStatus;
import com.opspilot.opspilotbackend.service.OrderService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public OrderResponseDto createOrder(
            @Valid @RequestBody OrderRequestDto request) {

        return orderService.createOrder(request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public List<OrderResponseDto> getAllOrders() {

        return orderService.getAllOrders();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public OrderResponseDto getOrderById(
            @PathVariable Long id) {

        return orderService.getOrderById(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public OrderResponseDto updateOrder(
            @PathVariable Long id,
            @Valid @RequestBody OrderRequestDto request) {

        return orderService.updateOrder(id, request);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public OrderResponseDto updateOrderStatus(
            @PathVariable Long id,
            @RequestParam OrderStatus status) {

        return orderService.updateOrderStatus(id, status);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteOrder(
            @PathVariable Long id) {

        orderService.deleteOrder(id);
    }
}