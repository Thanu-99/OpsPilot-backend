package com.opspilot.opspilotbackend.service.impl;

import com.opspilot.opspilotbackend.dto.DashboardSummaryDto;
import com.opspilot.opspilotbackend.entity.Order;
import com.opspilot.opspilotbackend.entity.OrderStatus;
import com.opspilot.opspilotbackend.repository.InventoryRepository;
import com.opspilot.opspilotbackend.repository.OrderRepository;
import com.opspilot.opspilotbackend.repository.ProductRepository;
import com.opspilot.opspilotbackend.service.DashboardService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class DashboardServiceImpl implements DashboardService {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final OrderRepository orderRepository;

    public DashboardServiceImpl(
            ProductRepository productRepository,
            InventoryRepository inventoryRepository,
            OrderRepository orderRepository) {

        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.orderRepository = orderRepository;
    }

    @Override
    public DashboardSummaryDto getSummary() {

        long totalProducts = productRepository.count();

        long lowStockProducts = inventoryRepository.countLowStockItems();

        List<Order> orders = orderRepository.findAll();

        long totalOrders = orders.size();

        long pendingOrders = orders.stream()
                .filter(order -> order.getStatus() == OrderStatus.PENDING)
                .count();

        BigDecimal totalRevenue = orders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalDate today = LocalDate.now();

        BigDecimal todayRevenue = orders.stream()
                .filter(order ->
                        order.getCreatedAt() != null
                                && order.getCreatedAt()
                                .toLocalDate()
                                .equals(today))
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return DashboardSummaryDto.builder()
                .totalProducts(totalProducts)
                .lowStockProducts(lowStockProducts)
                .totalOrders(totalOrders)
                .pendingOrders(pendingOrders)
                .totalRevenue(totalRevenue)
                .todayRevenue(todayRevenue)
                .build();
    }
}