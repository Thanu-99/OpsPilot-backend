package com.opspilot.opspilotbackend.service.impl;

import com.opspilot.opspilotbackend.exception.ResourceNotFoundException;
import com.opspilot.opspilotbackend.dto.InventoryRequestDto;
import com.opspilot.opspilotbackend.dto.InventoryResponseDto;
import com.opspilot.opspilotbackend.entity.Inventory;
import com.opspilot.opspilotbackend.entity.Product;
import com.opspilot.opspilotbackend.entity.User;
import com.opspilot.opspilotbackend.kafka.KafkaEventProducer;
import com.opspilot.opspilotbackend.mapper.InventoryMapper;
import com.opspilot.opspilotbackend.repository.InventoryRepository;
import com.opspilot.opspilotbackend.repository.ProductRepository;
import com.opspilot.opspilotbackend.repository.UserRepository;
import com.opspilot.opspilotbackend.service.AuditLogService;
import com.opspilot.opspilotbackend.service.InventoryService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final AuditLogService auditLogService;
    private final UserRepository userRepository;
    private final KafkaEventProducer kafkaEventProducer;

    public InventoryServiceImpl(
            InventoryRepository inventoryRepository,
            ProductRepository productRepository,
            AuditLogService auditLogService,
            UserRepository userRepository,
            KafkaEventProducer kafkaEventProducer) {

        this.inventoryRepository = inventoryRepository;
        this.productRepository = productRepository;
        this.auditLogService = auditLogService;
        this.userRepository = userRepository;
        this.kafkaEventProducer = kafkaEventProducer;
    }

    @Override
    public InventoryResponseDto createInventory(
            InventoryRequestDto request) {

        Product product = productRepository.findById(
                        request.getProductId()
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException("Product not found")
                );

        if (inventoryRepository.existsByProductId(
                request.getProductId())) {

            throw new ResourceNotFoundException(
                    "Inventory already exists for this product"
            );
        }

        Inventory inventory = Inventory.builder()
                .product(product)
                .quantity(request.getQuantity())
                .reorderLevel(request.getReorderLevel())
                .active(true)
                .build();

        inventory = inventoryRepository.save(inventory);

        audit(
                "CREATE",
                "INVENTORY",
                inventory.getId(),
                "Created inventory for product: "
                        + product.getName()
        );

        kafkaEventProducer.sendEvent(
                "INVENTORY_CREATED:"
                        + inventory.getId()
                        + ":"
                        + product.getName()
        );

        return InventoryMapper.toResponse(inventory);
    }

    @Override
    public List<InventoryResponseDto> getAllInventory() {

        return inventoryRepository.findAll()
                .stream()
                .map(InventoryMapper::toResponse)
                .toList();
    }

    @Override
    public InventoryResponseDto getInventoryById(Long id) {

        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Inventory not found"
                        )
                );

        return InventoryMapper.toResponse(inventory);
    }

    @Override
    public InventoryResponseDto updateInventory(
            Long id,
            InventoryRequestDto request) {

        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Inventory not found"
                        )
                );

        Product product = productRepository.findById(
                        request.getProductId()
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException("Product not found")
                );

        inventory.setProduct(product);
        inventory.setQuantity(request.getQuantity());
        inventory.setReorderLevel(request.getReorderLevel());

        inventory = inventoryRepository.save(inventory);

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (inventory.getQuantity() <= inventory.getReorderLevel()) {

            if (authentication != null &&
                    authentication.getName() != null) {

                User currentUser = userRepository
                        .findByEmail(authentication.getName())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Authenticated user not found"
                                )
                        );

                kafkaEventProducer.sendEvent(
                        "LOW_STOCK:"
                                + currentUser.getId()
                                + ":"
                                + inventory.getId()
                                + ":"
                                + product.getName()
                                + ":"
                                + inventory.getQuantity()
                );
            }
        }

        audit(
                "UPDATE",
                "INVENTORY",
                inventory.getId(),
                "Updated inventory for product: "
                        + product.getName()
        );

        kafkaEventProducer.sendEvent(
                "INVENTORY_UPDATED:"
                        + inventory.getId()
                        + ":"
                        + product.getName()
                        + ":"
                        + inventory.getQuantity()
        );

        return InventoryMapper.toResponse(inventory);
    }

    @Override
    public void deleteInventory(Long id) {

        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Inventory not found"
                        )
                );

        String productName =
                inventory.getProduct().getName();

        inventoryRepository.delete(inventory);

        audit(
                "DELETE",
                "INVENTORY",
                id,
                "Deleted inventory for product: "
                        + productName
        );

        kafkaEventProducer.sendEvent(
                "INVENTORY_DELETED:"
                        + id
                        + ":"
                        + productName
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

        User currentUser = userRepository
                .findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Authenticated user not found"
                        )
                );

        auditLogService.createAuditLog(
                currentUser.getId(),
                action,
                entityType,
                entityId,
                details
        );
    }
}

