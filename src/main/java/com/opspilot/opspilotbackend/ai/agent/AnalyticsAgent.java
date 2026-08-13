package com.opspilot.opspilotbackend.ai.agent;

import com.opspilot.opspilotbackend.entity.Order;
import com.opspilot.opspilotbackend.entity.OrderStatus;
import com.opspilot.opspilotbackend.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class AnalyticsAgent {

    private final OrderRepository orderRepository;

    public AnalyticsAgent(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public String handle(String message) {

        String lower = message.toLowerCase();

        List<Order> orders = orderRepository.findAll();

        /*
         * SALES / REVENUE
         */
        if (lower.contains("revenue")
                || lower.contains("sales revenue")
                || lower.contains("total sales")
                || lower.contains("sales performance")
                || lower.equals("sales")
                || lower.contains(" sales ")) {

            List<Order> deliveredOrders = orders.stream()
                    .filter(order -> order.getStatus() == OrderStatus.DELIVERED)
                    .toList();

            BigDecimal revenue = deliveredOrders.stream()
                    .map(Order::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            long completedCount = deliveredOrders.size();

            return "Completed sales revenue is ₹"
                    + revenue
                    + " from "
                    + completedCount
                    + " completed "
                    + (completedCount == 1 ? "order." : "orders.");
        }

        /*
         * AVERAGE ORDER VALUE
         */
        if (lower.contains("average order value")
                || lower.contains("average order")) {

            List<Order> deliveredOrders = orders.stream()
                    .filter(order -> order.getStatus() == OrderStatus.DELIVERED)
                    .toList();

            if (deliveredOrders.isEmpty()) {
                return "There are no completed orders to calculate the average order value.";
            }

            BigDecimal total = deliveredOrders.stream()
                    .map(Order::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal average = total.divide(
                    BigDecimal.valueOf(deliveredOrders.size()),
                    2,
                    RoundingMode.HALF_UP
            );

            return "The average order value is ₹" + average + ".";
        }

        /*
         * COMPLETED ORDERS
         */
        if (lower.contains("completed orders")
                || lower.contains("completed sales")
                || lower.contains("delivered orders")) {

            long count = orders.stream()
                    .filter(order -> order.getStatus() == OrderStatus.DELIVERED)
                    .count();

            return count == 1
                    ? "There is currently 1 completed order."
                    : "There are currently " + count + " completed orders.";
        }

        /*
         * TOTAL ORDER VALUE
         */
        if (lower.contains("total order value")
                || lower.contains("value of all orders")
                || lower.contains("total value of orders")) {

            BigDecimal total = orders.stream()
                    .map(Order::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            return "The total value of all orders is ₹" + total + ".";
        }

        return null;
    }
}