package com.opspilot.opspilotbackend.service.impl;

import com.opspilot.opspilotbackend.dto.SalesAnalyticsDto;
import com.opspilot.opspilotbackend.entity.Order;
import com.opspilot.opspilotbackend.entity.OrderStatus;
import com.opspilot.opspilotbackend.entity.User;
import com.opspilot.opspilotbackend.exception.ResourceNotFoundException;
import com.opspilot.opspilotbackend.repository.OrderRepository;
import com.opspilot.opspilotbackend.repository.UserRepository;
import com.opspilot.opspilotbackend.service.SalesAnalyticsService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class SalesAnalyticsServiceImpl
        implements SalesAnalyticsService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public SalesAnalyticsServiceImpl(
            OrderRepository orderRepository,
            UserRepository userRepository
    ) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SalesAnalyticsDto> getSalesAnalytics(
            LocalDate startDate,
            LocalDate endDate
    ) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException(
                    "Start date and end date are required"
            );
        }

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException(
                    "Start date cannot be after end date"
            );
        }

        User currentUser = getCurrentUser();

        List<Order> companyOrders =
                orderRepository
                        .findByCompanyIdOrderByCreatedAtDesc(
                                currentUser.getCompanyId()
                        );

        Map<LocalDate, List<Order>> ordersByDate =
                companyOrders.stream()
                        .filter(this::isSalesOrder)
                        .filter(order ->
                                order.getCreatedAt() != null
                        )
                        .filter(order -> {
                            LocalDate orderDate =
                                    order.getCreatedAt()
                                            .toLocalDate();

                            return !orderDate.isBefore(startDate)
                                    && !orderDate.isAfter(endDate);
                        })
                        .collect(Collectors.groupingBy(
                                order ->
                                        order.getCreatedAt()
                                                .toLocalDate()
                        ));

        return startDate
                .datesUntil(endDate.plusDays(1))
                .map(date -> {
                    List<Order> orders =
                            ordersByDate.getOrDefault(
                                    date,
                                    List.of()
                            );

                    BigDecimal revenue = orders.stream()
                            .map(Order::getTotalAmount)
                            .filter(Objects::nonNull)
                            .reduce(
                                    BigDecimal.ZERO,
                                    BigDecimal::add
                            );

                    return SalesAnalyticsDto.builder()
                            .date(date)
                            .revenue(revenue)
                            .orderCount(orders.size())
                            .build();
                })
                .toList();
    }

    private boolean isSalesOrder(Order order) {
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