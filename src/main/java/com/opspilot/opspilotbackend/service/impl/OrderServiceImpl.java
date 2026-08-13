package com.opspilot.opspilotbackend.service.impl;
import com.opspilot.opspilotbackend.exception.ResourceNotFoundException;
import com.opspilot.opspilotbackend.dto.OrderItemRequestDto;
import com.opspilot.opspilotbackend.dto.OrderRequestDto;
import com.opspilot.opspilotbackend.dto.OrderResponseDto;
import com.opspilot.opspilotbackend.entity.Inventory;
import com.opspilot.opspilotbackend.entity.Order;
import com.opspilot.opspilotbackend.entity.OrderItem;
import com.opspilot.opspilotbackend.entity.OrderStatus;
import com.opspilot.opspilotbackend.entity.Product;
import com.opspilot.opspilotbackend.entity.User;
import com.opspilot.opspilotbackend.mapper.OrderMapper;
import com.opspilot.opspilotbackend.repository.InventoryRepository;
import com.opspilot.opspilotbackend.repository.OrderItemRepository;
import com.opspilot.opspilotbackend.repository.OrderRepository;
import com.opspilot.opspilotbackend.repository.ProductRepository;
import com.opspilot.opspilotbackend.repository.UserRepository;
import com.opspilot.opspilotbackend.service.AuditLogService;
import com.opspilot.opspilotbackend.service.OrderService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final AuditLogService auditLogService;
    private final UserRepository userRepository;

    public OrderServiceImpl(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            ProductRepository productRepository,
            InventoryRepository inventoryRepository,
            AuditLogService auditLogService,
            UserRepository userRepository) {

        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.auditLogService = auditLogService;
        this.userRepository = userRepository;
    }

    @Override
    public OrderResponseDto createOrder(OrderRequestDto request) {

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new ResourceNotFoundException("Order must contain at least one item");
        }

        Order order = Order.builder()
                .totalAmount(BigDecimal.ZERO)
                .status(OrderStatus.PENDING)
                .build();

        order = orderRepository.save(order);

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderItemRequestDto itemRequest : request.getItems()) {

            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

            if (itemRequest.getQuantity() == null ||
                    itemRequest.getQuantity() <= 0) {

                throw new ResourceNotFoundException("Quantity must be greater than zero");
            }

            Inventory inventory = inventoryRepository
                    .findByProductId(product.getId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Inventory not found for product: "
                                            + product.getName()
                            )
                    );

            if (inventory.getQuantity() < itemRequest.getQuantity()) {
                throw new ResourceNotFoundException(
                        "Insufficient stock for product: "
                                + product.getName()
                );
            }

            BigDecimal unitPrice = product.getPrice();

            BigDecimal subtotal = unitPrice.multiply(
                    BigDecimal.valueOf(itemRequest.getQuantity())
            );

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(unitPrice)
                    .subtotal(subtotal)
                    .build();

            orderItems.add(orderItem);
            totalAmount = totalAmount.add(subtotal);

            inventory.setQuantity(
                    inventory.getQuantity() - itemRequest.getQuantity()
            );

            inventoryRepository.save(inventory);
        }

        order.setTotalAmount(totalAmount);
        order = orderRepository.save(order);

        orderItemRepository.saveAll(orderItems);

        audit(
                "CREATE",
                "ORDER",
                order.getId(),
                "Created order with total amount: " + totalAmount
        );

        return OrderMapper.toResponse(order, orderItems);
    }

    @Override
    public List<OrderResponseDto> getAllOrders() {

        return orderRepository.findAll()
                .stream()
                .map(order -> OrderMapper.toResponse(
                        order,
                        orderItemRepository.findByOrderId(order.getId())
                ))
                .toList();
    }

    @Override
    public OrderResponseDto getOrderById(Long id) {

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        List<OrderItem> items =
                orderItemRepository.findByOrderId(id);

        return OrderMapper.toResponse(order, items);
    }

    @Override
    public OrderResponseDto updateOrder(
            Long id,
            OrderRequestDto request) {

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new ResourceNotFoundException("Order must contain at least one item");
        }

        List<OrderItem> existingItems =
                orderItemRepository.findByOrderId(id);

        for (OrderItem item : existingItems) {

            Inventory inventory = inventoryRepository
                    .findByProductId(item.getProduct().getId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Inventory not found for product: "
                                            + item.getProduct().getName()
                            )
                    );

            inventory.setQuantity(
                    inventory.getQuantity() + item.getQuantity()
            );

            inventoryRepository.save(inventory);
        }

        orderItemRepository.deleteAll(existingItems);

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> newItems = new ArrayList<>();

        for (OrderItemRequestDto itemRequest : request.getItems()) {

            Product product = productRepository
                    .findById(itemRequest.getProductId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException("Product not found")
                    );

            if (itemRequest.getQuantity() == null ||
                    itemRequest.getQuantity() <= 0) {

                throw new ResourceNotFoundException(
                        "Quantity must be greater than zero"
                );
            }

            Inventory inventory = inventoryRepository
                    .findByProductId(product.getId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Inventory not found for product: "
                                            + product.getName()
                            )
                    );

            if (inventory.getQuantity() < itemRequest.getQuantity()) {
                throw new ResourceNotFoundException(
                        "Insufficient stock for product: "
                                + product.getName()
                );
            }

            BigDecimal unitPrice = product.getPrice();

            BigDecimal subtotal = unitPrice.multiply(
                    BigDecimal.valueOf(itemRequest.getQuantity())
            );

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(unitPrice)
                    .subtotal(subtotal)
                    .build();

            newItems.add(orderItem);
            totalAmount = totalAmount.add(subtotal);

            inventory.setQuantity(
                    inventory.getQuantity() - itemRequest.getQuantity()
            );

            inventoryRepository.save(inventory);
        }

        order.setTotalAmount(totalAmount);

        order = orderRepository.save(order);

        orderItemRepository.saveAll(newItems);

        audit(
                "UPDATE",
                "ORDER",
                order.getId(),
                "Updated order. New total amount: " + totalAmount
        );

        return OrderMapper.toResponse(order, newItems);
    }

    @Override
    public OrderResponseDto updateOrderStatus(
            Long id,
            OrderStatus status) {

        if (status == null) {
            throw new ResourceNotFoundException("Order status is required");
        }

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        OrderStatus currentStatus = order.getStatus();

        if (currentStatus == OrderStatus.DELIVERED ||
                currentStatus == OrderStatus.CANCELLED) {

            throw new ResourceNotFoundException(
                    "Cannot change status of a completed or cancelled order"
            );
        }

        order.setStatus(status);

        order = orderRepository.save(order);

        audit(
                "STATUS_CHANGE",
                "ORDER",
                order.getId(),
                "Order status changed from "
                        + currentStatus
                        + " to "
                        + status
        );

        List<OrderItem> items =
                orderItemRepository.findByOrderId(id);

        return OrderMapper.toResponse(order, items);
    }

    @Override
    public void deleteOrder(Long id) {

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        List<OrderItem> items =
                orderItemRepository.findByOrderId(id);

        for (OrderItem item : items) {

            Inventory inventory = inventoryRepository
                    .findByProductId(item.getProduct().getId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Inventory not found for product: "
                                            + item.getProduct().getName()
                            )
                    );

            inventory.setQuantity(
                    inventory.getQuantity() + item.getQuantity()
            );

            inventoryRepository.save(inventory);
        }

        orderItemRepository.deleteAll(items);
        orderRepository.delete(order);

        audit(
                "DELETE",
                "ORDER",
                id,
                "Deleted order"
        );
    }

    private void audit(
            String action,
            String entityType,
            Long entityId,
            String details) {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null ||
                authentication.getName() == null) {
            return;
        }

        User user = userRepository
                .findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Authenticated user not found"
                        )
                );

        auditLogService.createAuditLog(
                user.getId(),
                action,
                entityType,
                entityId,
                details
        );
    }
}