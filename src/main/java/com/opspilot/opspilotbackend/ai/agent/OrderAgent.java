package com.opspilot.opspilotbackend.ai.agent;

import com.opspilot.opspilotbackend.entity.Order;
import com.opspilot.opspilotbackend.entity.OrderStatus;
import com.opspilot.opspilotbackend.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class OrderAgent {

    private final OrderRepository orderRepository;

    public OrderAgent(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public String handle(String message) {

        String lower = message.toLowerCase();

        List<Order> orders = orderRepository.findAll();

        if (lower.contains("how many orders")
                || lower.contains("total orders")) {

            int count = orders.size();

            return count == 1
                    ? "There is currently 1 order."
                    : "There are currently " + count + " orders.";
        }

        if (lower.contains("pending orders")) {

            long count = orders.stream()
                    .filter(order -> order.getStatus() == OrderStatus.PENDING)
                    .count();

            return count == 1
                    ? "There is currently 1 pending order."
                    : "There are currently " + count + " pending orders.";
        }

        if (lower.contains("total order value")
                || lower.contains("order value")) {

            BigDecimal total = orders.stream()
                    .map(Order::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            return "The total value of all orders is ₹" + total + ".";
        }

        return null;
    }
}