package com.opspilot.opspilotbackend.service.impl;

import com.opspilot.opspilotbackend.dto.DashboardSummaryDto;
import com.opspilot.opspilotbackend.entity.Order;
import com.opspilot.opspilotbackend.entity.OrderStatus;
import com.opspilot.opspilotbackend.entity.User;
import com.opspilot.opspilotbackend.exception.ResourceNotFoundException;
import com.opspilot.opspilotbackend.repository.InventoryRepository;
import com.opspilot.opspilotbackend.repository.OrderRepository;
import com.opspilot.opspilotbackend.repository.ProductRepository;
import com.opspilot.opspilotbackend.repository.UserRepository;
import com.opspilot.opspilotbackend.service.DashboardService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
public class DashboardServiceImpl implements DashboardService {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public DashboardServiceImpl(
            ProductRepository productRepository,
            InventoryRepository inventoryRepository,
            OrderRepository orderRepository,
            UserRepository userRepository
    ) {
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardSummaryDto getSummary() {
        User currentUser = getCurrentUser();
        Long companyId = currentUser.getCompanyId();

        long totalProducts =
                productRepository.countByCompanyId(companyId);

        long lowStockProducts =
                inventoryRepository
                        .countLowStockItemsByCompanyId(
                                companyId
                        );

        List<Order> orders =
                orderRepository
                        .findByCompanyIdOrderByCreatedAtDesc(
                                companyId
                        );

        long totalOrders = orders.size();

        long pendingOrders = orders.stream()
                .filter(order ->
                        order.getStatus() == OrderStatus.PENDING
                )
                .count();

        BigDecimal totalRevenue = orders.stream()
                .filter(this::isRevenueOrder)
                .map(Order::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalDate today = LocalDate.now();

        BigDecimal todayRevenue = orders.stream()
                .filter(this::isRevenueOrder)
                .filter(order -> order.getCreatedAt() != null)
                .filter(order ->
                        order.getCreatedAt()
                                .toLocalDate()
                                .equals(today)
                )
                .map(Order::getTotalAmount)
                .filter(Objects::nonNull)
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

    private boolean isRevenueOrder(Order order) {
        return order.getStatus() != null
                && order.getStatus() != OrderStatus.CANCELLED;
    }

    private User getCurrentUser() {
        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null ||
                !authentication.isAuthenticated() ||
                authentication.getName() == null) {
            throw new ResourceNotFoundException(
                    "Authenticated user not found"
            );
        }

        return userRepository
                .findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Authenticated user not found"
                        )
                );
    }
}