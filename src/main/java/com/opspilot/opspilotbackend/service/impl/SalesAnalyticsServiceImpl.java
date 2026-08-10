package com.opspilot.opspilotbackend.service.impl;

import com.opspilot.opspilotbackend.dto.SalesAnalyticsDto;
import com.opspilot.opspilotbackend.entity.Order;
import com.opspilot.opspilotbackend.repository.OrderRepository;
import com.opspilot.opspilotbackend.service.SalesAnalyticsService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SalesAnalyticsServiceImpl implements SalesAnalyticsService {

    private final OrderRepository orderRepository;

    public SalesAnalyticsServiceImpl(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public List<SalesAnalyticsDto> getSalesAnalytics(
            LocalDate startDate,
            LocalDate endDate) {

        return orderRepository.findAll()
                .stream()
                .filter(order -> order.getCreatedAt() != null)
                .filter(order -> {
                    LocalDate orderDate = order.getCreatedAt().toLocalDate();

                    return !orderDate.isBefore(startDate)
                            && !orderDate.isAfter(endDate);
                })
                .collect(Collectors.groupingBy(
                        order -> order.getCreatedAt().toLocalDate()
                ))
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {

                    LocalDate date = entry.getKey();
                    List<Order> orders = entry.getValue();

                    BigDecimal revenue = orders.stream()
                            .map(Order::getTotalAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return SalesAnalyticsDto.builder()
                            .date(date)
                            .revenue(revenue)
                            .orderCount(orders.size())
                            .build();
                })
                .toList();
    }
}