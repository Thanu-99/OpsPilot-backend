package com.opspilot.opspilotbackend.service.impl;

import com.opspilot.opspilotbackend.dto.InventoryRequestDto;
import com.opspilot.opspilotbackend.dto.InventoryResponseDto;
import com.opspilot.opspilotbackend.entity.Inventory;
import com.opspilot.opspilotbackend.entity.Product;
import com.opspilot.opspilotbackend.entity.User;
import com.opspilot.opspilotbackend.exception.ResourceNotFoundException;
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
import java.util.Objects;

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
            KafkaEventProducer kafkaEventProducer
    ) {
        this.inventoryRepository = inventoryRepository;
        this.productRepository = productRepository;
        this.auditLogService = auditLogService;
        this.userRepository = userRepository;
        this.kafkaEventProducer = kafkaEventProducer;
    }

    @Override
    public InventoryResponseDto createInventory(
            InventoryRequestDto request
    ) {
        User currentUser = getCurrentUser();

        Product product = findCompanyProduct(
                request.getProductId(),
                currentUser.getCompanyId()
        );

        boolean inventoryAlreadyExists =
                inventoryRepository
                        .existsByProduct_IdAndProduct_CompanyId(
                                product.getId(),
                                currentUser.getCompanyId()
                        );

        if (inventoryAlreadyExists) {
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

        Inventory savedInventory =
                inventoryRepository.save(inventory);

        synchronizeProductQuantity(
                product,
                request.getQuantity()
        );

        createAuditLog(
                currentUser,
                "CREATE",
                savedInventory.getId(),
                "Created inventory for product: "
                        + product.getName()
        );

        kafkaEventProducer.sendEvent(
                "INVENTORY_CREATED:"
                        + savedInventory.getId()
                        + ":"
                        + product.getName()
        );

        return InventoryMapper.toResponse(savedInventory);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryResponseDto> getAllInventory() {
        User currentUser = getCurrentUser();

        return inventoryRepository
                .findByProduct_CompanyIdOrderByProduct_NameAsc(
                        currentUser.getCompanyId()
                )
                .stream()
                .map(InventoryMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryResponseDto getInventoryById(Long id) {
        User currentUser = getCurrentUser();

        Inventory inventory = findCompanyInventory(
                id,
                currentUser.getCompanyId()
        );

        return InventoryMapper.toResponse(inventory);
    }

    @Override
    public InventoryResponseDto updateInventory(
            Long id,
            InventoryRequestDto request
    ) {
        User currentUser = getCurrentUser();

        Inventory inventory = findCompanyInventory(
                id,
                currentUser.getCompanyId()
        );

        Long currentInventoryId = inventory.getId();

        Product product = findCompanyProduct(
                request.getProductId(),
                currentUser.getCompanyId()
        );

        boolean productHasDifferentInventory =
                inventoryRepository
                        .findByProduct_IdAndProduct_CompanyId(
                                product.getId(),
                                currentUser.getCompanyId()
                        )
                        .filter(existingInventory ->
                                !Objects.equals(
                                        existingInventory.getId(),
                                        currentInventoryId
                                )
                        )
                        .isPresent();

        if (productHasDifferentInventory) {
            throw new ResourceNotFoundException(
                    "Inventory already exists for this product"
            );
        }

        inventory.setProduct(product);
        inventory.setQuantity(request.getQuantity());
        inventory.setReorderLevel(request.getReorderLevel());

        Inventory savedInventory =
                inventoryRepository.save(inventory);

        synchronizeProductQuantity(
                product,
                request.getQuantity()
        );

        if (savedInventory.getQuantity() <=
                savedInventory.getReorderLevel()) {
            kafkaEventProducer.sendEvent(
                    "LOW_STOCK:"
                            + currentUser.getId()
                            + ":"
                            + savedInventory.getId()
                            + ":"
                            + product.getName()
                            + ":"
                            + savedInventory.getQuantity()
            );
        }

        createAuditLog(
                currentUser,
                "UPDATE",
                savedInventory.getId(),
                "Updated inventory for product: "
                        + product.getName()
        );

        kafkaEventProducer.sendEvent(
                "INVENTORY_UPDATED:"
                        + savedInventory.getId()
                        + ":"
                        + product.getName()
                        + ":"
                        + savedInventory.getQuantity()
        );

        return InventoryMapper.toResponse(savedInventory);
    }

    @Override
    public void deleteInventory(Long id) {
        User currentUser = getCurrentUser();

        Inventory inventory = findCompanyInventory(
                id,
                currentUser.getCompanyId()
        );

        Product product = inventory.getProduct();
        String productName = product.getName();

        inventoryRepository.delete(inventory);

        synchronizeProductQuantity(product, 0);

        createAuditLog(
                currentUser,
                "DELETE",
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

    private Inventory findCompanyInventory(
            Long inventoryId,
            Long companyId
    ) {
        return inventoryRepository
                .findByIdAndProduct_CompanyId(
                        inventoryId,
                        companyId
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Inventory not found"
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

    private void synchronizeProductQuantity(
            Product product,
            Integer inventoryQuantity
    ) {
        product.setQuantity(inventoryQuantity);
        productRepository.save(product);
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
                "INVENTORY",
                entityId,
                details
        );
    }
}