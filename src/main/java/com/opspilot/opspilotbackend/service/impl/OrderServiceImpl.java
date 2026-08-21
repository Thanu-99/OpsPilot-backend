package com.opspilot.opspilotbackend.service.impl;

import com.opspilot.opspilotbackend.dto.OrderItemRequestDto;
import com.opspilot.opspilotbackend.dto.OrderRequestDto;
import com.opspilot.opspilotbackend.dto.OrderResponseDto;
import com.opspilot.opspilotbackend.entity.Inventory;
import com.opspilot.opspilotbackend.entity.Order;
import com.opspilot.opspilotbackend.entity.OrderItem;
import com.opspilot.opspilotbackend.entity.OrderStatus;
import com.opspilot.opspilotbackend.entity.Product;
import com.opspilot.opspilotbackend.entity.User;
import com.opspilot.opspilotbackend.exception.ResourceNotFoundException;
import com.opspilot.opspilotbackend.kafka.KafkaEventProducer;
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
    private final KafkaEventProducer kafkaEventProducer;

    public OrderServiceImpl(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            ProductRepository productRepository,
            InventoryRepository inventoryRepository,
            AuditLogService auditLogService,
            UserRepository userRepository,
            KafkaEventProducer kafkaEventProducer
    ) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.auditLogService = auditLogService;
        this.userRepository = userRepository;
        this.kafkaEventProducer = kafkaEventProducer;
    }

    @Override
    public OrderResponseDto createOrder(
            OrderRequestDto request
    ) {
        User currentUser = getCurrentUser();

        validateOrderItems(request);

        Order order = Order.builder()
                .companyId(currentUser.getCompanyId())
                .totalAmount(BigDecimal.ZERO)
                .status(OrderStatus.PENDING)
                .build();

        Order savedOrder = orderRepository.save(order);

        List<OrderItem> orderItems = buildOrderItems(
                savedOrder,
                request.getItems(),
                currentUser.getCompanyId()
        );

        BigDecimal totalAmount =
                calculateTotalAmount(orderItems);

        savedOrder.setTotalAmount(totalAmount);
        savedOrder = orderRepository.save(savedOrder);

        orderItemRepository.saveAll(orderItems);

        createAuditLog(
                currentUser,
                "CREATE",
                savedOrder.getId(),
                "Created order with total amount: "
                        + totalAmount
        );

        kafkaEventProducer.sendEvent(
                "ORDER_CREATED:" + savedOrder.getId()
        );

        return OrderMapper.toResponse(
                savedOrder,
                orderItems
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDto> getAllOrders() {
        User currentUser = getCurrentUser();

        return orderRepository
                .findByCompanyIdOrderByCreatedAtDesc(
                        currentUser.getCompanyId()
                )
                .stream()
                .map(order -> OrderMapper.toResponse(
                        order,
                        orderItemRepository.findByOrderId(
                                order.getId()
                        )
                ))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDto getOrderById(Long id) {
        User currentUser = getCurrentUser();

        Order order = findCompanyOrder(
                id,
                currentUser.getCompanyId()
        );

        List<OrderItem> items =
                orderItemRepository.findByOrderId(
                        order.getId()
                );

        return OrderMapper.toResponse(order, items);
    }

    @Override
    public OrderResponseDto updateOrder(
            Long id,
            OrderRequestDto request
    ) {
        User currentUser = getCurrentUser();

        validateOrderItems(request);

        Order order = findCompanyOrder(
                id,
                currentUser.getCompanyId()
        );

        if (order.getStatus() == OrderStatus.DELIVERED ||
                order.getStatus() == OrderStatus.CANCELLED) {
            throw new ResourceNotFoundException(
                    "A delivered or cancelled order cannot be edited"
            );
        }

        List<OrderItem> existingItems =
                orderItemRepository.findByOrderId(
                        order.getId()
                );

        restoreInventory(
                existingItems,
                currentUser.getCompanyId()
        );

        orderItemRepository.deleteAll(existingItems);

        List<OrderItem> newItems = buildOrderItems(
                order,
                request.getItems(),
                currentUser.getCompanyId()
        );

        BigDecimal totalAmount =
                calculateTotalAmount(newItems);

        order.setTotalAmount(totalAmount);

        Order savedOrder = orderRepository.save(order);

        orderItemRepository.saveAll(newItems);

        createAuditLog(
                currentUser,
                "UPDATE",
                savedOrder.getId(),
                "Updated order. New total amount: "
                        + totalAmount
        );

        kafkaEventProducer.sendEvent(
                "ORDER_UPDATED:" + savedOrder.getId()
        );

        return OrderMapper.toResponse(
                savedOrder,
                newItems
        );
    }

    @Override
    public OrderResponseDto updateOrderStatus(
            Long id,
            OrderStatus status
    ) {
        User currentUser = getCurrentUser();

        if (status == null) {
            throw new ResourceNotFoundException(
                    "Order status is required"
            );
        }

        Order order = findCompanyOrder(
                id,
                currentUser.getCompanyId()
        );

        OrderStatus currentStatus = order.getStatus();

        if (currentStatus == OrderStatus.DELIVERED ||
                currentStatus == OrderStatus.CANCELLED) {
            throw new ResourceNotFoundException(
                    "Cannot change the status of a delivered or cancelled order"
            );
        }

        if (currentStatus == status) {
            List<OrderItem> unchangedItems =
                    orderItemRepository.findByOrderId(id);

            return OrderMapper.toResponse(
                    order,
                    unchangedItems
            );
        }

        List<OrderItem> items =
                orderItemRepository.findByOrderId(id);

        if (status == OrderStatus.CANCELLED) {
            restoreInventory(
                    items,
                    currentUser.getCompanyId()
            );
        }

        order.setStatus(status);

        Order savedOrder = orderRepository.save(order);

        createAuditLog(
                currentUser,
                "STATUS_CHANGE",
                savedOrder.getId(),
                "Order status changed from "
                        + currentStatus
                        + " to "
                        + status
        );

        kafkaEventProducer.sendEvent(
                "ORDER_STATUS_CHANGED:"
                        + savedOrder.getId()
                        + ":"
                        + currentStatus
                        + "->"
                        + status
        );

        return OrderMapper.toResponse(
                savedOrder,
                items
        );
    }

    @Override
    public void deleteOrder(Long id) {
        User currentUser = getCurrentUser();

        Order order = findCompanyOrder(
                id,
                currentUser.getCompanyId()
        );

        List<OrderItem> items =
                orderItemRepository.findByOrderId(id);

        if (shouldRestoreStockWhenDeleted(order)) {
            restoreInventory(
                    items,
                    currentUser.getCompanyId()
            );
        }

        orderItemRepository.deleteAll(items);
        orderRepository.delete(order);

        createAuditLog(
                currentUser,
                "DELETE",
                id,
                "Deleted order"
        );

        kafkaEventProducer.sendEvent(
                "ORDER_DELETED:" + id
        );
    }

    private List<OrderItem> buildOrderItems(
            Order order,
            List<OrderItemRequestDto> itemRequests,
            Long companyId
    ) {
        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderItemRequestDto itemRequest : itemRequests) {
            validateRequestedQuantity(itemRequest);

            Product product = findCompanyProduct(
                    itemRequest.getProductId(),
                    companyId
            );

            removeFromInventory(
                    product,
                    itemRequest.getQuantity(),
                    companyId
            );

            BigDecimal unitPrice = product.getPrice();

            BigDecimal subtotal = unitPrice.multiply(
                    BigDecimal.valueOf(
                            itemRequest.getQuantity()
                    )
            );

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(unitPrice)
                    .subtotal(subtotal)
                    .build();

            orderItems.add(orderItem);
        }

        return orderItems;
    }

    private void removeFromInventory(
            Product product,
            Integer requestedQuantity,
            Long companyId
    ) {
        Inventory inventory = inventoryRepository
                .findByProduct_IdAndProduct_CompanyId(
                        product.getId(),
                        companyId
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Inventory not found for product: "
                                        + product.getName()
                        )
                );

        if (inventory.getQuantity() < requestedQuantity) {
            throw new ResourceNotFoundException(
                    "Insufficient stock for product: "
                            + product.getName()
            );
        }

        int updatedQuantity =
                inventory.getQuantity() - requestedQuantity;

        inventory.setQuantity(updatedQuantity);
        inventoryRepository.save(inventory);

        product.setQuantity(updatedQuantity);
        productRepository.save(product);
    }

    private void restoreInventory(
            List<OrderItem> items,
            Long companyId
    ) {
        for (OrderItem item : items) {
            Product product = findCompanyProduct(
                    item.getProduct().getId(),
                    companyId
            );

            Inventory inventory = inventoryRepository
                    .findByProduct_IdAndProduct_CompanyId(
                            product.getId(),
                            companyId
                    )
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Inventory not found for product: "
                                            + product.getName()
                            )
                    );

            int restoredQuantity =
                    inventory.getQuantity()
                            + item.getQuantity();

            inventory.setQuantity(restoredQuantity);
            inventoryRepository.save(inventory);

            product.setQuantity(restoredQuantity);
            productRepository.save(product);
        }
    }

    private BigDecimal calculateTotalAmount(
            List<OrderItem> orderItems
    ) {
        return orderItems.stream()
                .map(OrderItem::getSubtotal)
                .reduce(
                        BigDecimal.ZERO,
                        BigDecimal::add
                );
    }

    private void validateOrderItems(
            OrderRequestDto request
    ) {
        if (request.getItems() == null ||
                request.getItems().isEmpty()) {
            throw new ResourceNotFoundException(
                    "Order must contain at least one item"
            );
        }
    }

    private void validateRequestedQuantity(
            OrderItemRequestDto itemRequest
    ) {
        if (itemRequest.getProductId() == null) {
            throw new ResourceNotFoundException(
                    "Product ID is required"
            );
        }

        if (itemRequest.getQuantity() == null ||
                itemRequest.getQuantity() <= 0) {
            throw new ResourceNotFoundException(
                    "Quantity must be greater than zero"
            );
        }
    }

    private boolean shouldRestoreStockWhenDeleted(
            Order order
    ) {
        return order.getStatus() != OrderStatus.DELIVERED
                && order.getStatus() != OrderStatus.CANCELLED;
    }

    private Order findCompanyOrder(
            Long orderId,
            Long companyId
    ) {
        return orderRepository
                .findByIdAndCompanyId(
                        orderId,
                        companyId
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Order not found"
                        )
                );
    }

    private Product findCompanyProduct(
            Long productId,
            Long companyId
    ) {
        return productRepository
                .findByIdAndCompanyId(
                        productId,
                        companyId
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Product not found"
                        )
                );
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

    private void createAuditLog(
            User currentUser,
            String action,
            Long entityId,
            String details
    ) {
        auditLogService.createAuditLog(
                currentUser.getId(),
                action,
                "ORDER",
                entityId,
                details
        );
    }
}