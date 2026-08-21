package com.opspilot.opspilotbackend.service.impl;

import com.opspilot.opspilotbackend.dto.ProductRequestDto;
import com.opspilot.opspilotbackend.dto.ProductResponseDto;
import com.opspilot.opspilotbackend.entity.Inventory;
import com.opspilot.opspilotbackend.entity.Product;
import com.opspilot.opspilotbackend.entity.User;
import com.opspilot.opspilotbackend.exception.ResourceNotFoundException;
import com.opspilot.opspilotbackend.mapper.ProductMapper;
import com.opspilot.opspilotbackend.repository.InventoryRepository;
import com.opspilot.opspilotbackend.repository.OrderItemRepository;
import com.opspilot.opspilotbackend.repository.ProductRepository;
import com.opspilot.opspilotbackend.repository.UserRepository;
import com.opspilot.opspilotbackend.service.AuditLogService;
import com.opspilot.opspilotbackend.service.ProductService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final OrderItemRepository orderItemRepository;
    private final AuditLogService auditLogService;
    private final UserRepository userRepository;

    public ProductServiceImpl(
            ProductRepository productRepository,
            InventoryRepository inventoryRepository,
            OrderItemRepository orderItemRepository,
            AuditLogService auditLogService,
            UserRepository userRepository
    ) {
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.orderItemRepository = orderItemRepository;
        this.auditLogService = auditLogService;
        this.userRepository = userRepository;
    }

    @Override
    public ProductResponseDto createProduct(
            ProductRequestDto request
    ) {
        User currentUser = getCurrentUser();

        validateSkuForCreate(
                currentUser.getCompanyId(),
                request.getSku()
        );

        Product product = ProductMapper.toEntity(request);
        product.setCompanyId(currentUser.getCompanyId());
        product.setActive(true);

        Product savedProduct =
                productRepository.save(product);

        Inventory inventory = Inventory.builder()
                .product(savedProduct)
                .quantity(savedProduct.getQuantity())
                .reorderLevel(
                        calculateDefaultReorderLevel(
                                savedProduct.getQuantity()
                        )
                )
                .active(true)
                .build();

        inventoryRepository.save(inventory);

        createAuditLog(
                currentUser,
                "CREATE",
                savedProduct.getId(),
                "Created product and inventory: "
                        + savedProduct.getName()
        );

        return ProductMapper.toResponse(savedProduct);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponseDto> getAllProducts() {
        User currentUser = getCurrentUser();

        return productRepository
                .findByCompanyIdOrderByNameAsc(
                        currentUser.getCompanyId()
                )
                .stream()
                .filter(Product::isActive)
                .map(ProductMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponseDto getProductById(Long id) {
        User currentUser = getCurrentUser();

        Product product = findActiveCompanyProduct(
                id,
                currentUser.getCompanyId()
        );

        return ProductMapper.toResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponseDto getProductByName(
            String name
    ) {
        User currentUser = getCurrentUser();

        Product product = productRepository
                .findByCompanyIdAndNameIgnoreCase(
                        currentUser.getCompanyId(),
                        name
                )
                .filter(Product::isActive)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Product not found: " + name
                        )
                );

        return ProductMapper.toResponse(product);
    }

    @Override
    public ProductResponseDto updateProduct(
            Long id,
            ProductRequestDto request
    ) {
        User currentUser = getCurrentUser();

        Product product = findActiveCompanyProduct(
                id,
                currentUser.getCompanyId()
        );

        validateSkuForUpdate(
                currentUser.getCompanyId(),
                id,
                request.getSku()
        );

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setQuantity(request.getQuantity());
        product.setCategory(request.getCategory());
        product.setSku(request.getSku());

        Product savedProduct =
                productRepository.save(product);

        synchronizeInventory(
                savedProduct,
                request.getQuantity(),
                currentUser.getCompanyId()
        );

        createAuditLog(
                currentUser,
                "UPDATE",
                savedProduct.getId(),
                "Updated product and inventory: "
                        + savedProduct.getName()
        );

        return ProductMapper.toResponse(savedProduct);
    }

    @Override
    public void deleteProduct(Long id) {
        User currentUser = getCurrentUser();

        Product product = findActiveCompanyProduct(
                id,
                currentUser.getCompanyId()
        );

        String productName = product.getName();
        Long productId = product.getId();

        Inventory inventory = inventoryRepository
                .findByProduct_IdAndProduct_CompanyId(
                        productId,
                        currentUser.getCompanyId()
                )
                .orElse(null);

        boolean hasOrderHistory =
                !orderItemRepository
                        .findByProductId(productId)
                        .isEmpty();

        if (hasOrderHistory) {
            product.setActive(false);
            product.setQuantity(0);
            productRepository.save(product);

            if (inventory != null) {
                inventory.setActive(false);
                inventory.setQuantity(0);
                inventoryRepository.save(inventory);
            }

            createAuditLog(
                    currentUser,
                    "ARCHIVE",
                    productId,
                    "Archived product with order history: "
                            + productName
            );

            return;
        }

        if (inventory != null) {
            inventoryRepository.delete(inventory);
        }

        productRepository.delete(product);

        createAuditLog(
                currentUser,
                "DELETE",
                productId,
                "Deleted product: " + productName
        );
    }

    private void synchronizeInventory(
            Product product,
            Integer quantity,
            Long companyId
    ) {
        Inventory inventory = inventoryRepository
                .findByProduct_IdAndProduct_CompanyId(
                        product.getId(),
                        companyId
                )
                .orElseGet(() ->
                        Inventory.builder()
                                .product(product)
                                .quantity(quantity)
                                .reorderLevel(
                                        calculateDefaultReorderLevel(
                                                quantity
                                        )
                                )
                                .active(true)
                                .build()
                );

        inventory.setProduct(product);
        inventory.setQuantity(quantity);
        inventory.setActive(true);

        if (inventory.getReorderLevel() == null ||
                inventory.getReorderLevel() <= 0) {
            inventory.setReorderLevel(
                    calculateDefaultReorderLevel(quantity)
            );
        }

        inventoryRepository.save(inventory);
    }

    private int calculateDefaultReorderLevel(
            Integer quantity
    ) {
        if (quantity == null || quantity <= 0) {
            return 5;
        }

        return Math.max(
                5,
                (int) Math.ceil(quantity * 0.20)
        );
    }

    private Product findActiveCompanyProduct(
            Long productId,
            Long companyId
    ) {
        return productRepository
                .findByIdAndCompanyId(
                        productId,
                        companyId
                )
                .filter(Product::isActive)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Product not found"
                        )
                );
    }

    private void validateSkuForCreate(
            Long companyId,
            String sku
    ) {
        if (sku == null || sku.isBlank()) {
            return;
        }

        if (productRepository.existsByCompanyIdAndSku(
                companyId,
                sku.trim()
        )) {
            throw new ResourceNotFoundException(
                    "A product with this SKU already exists"
            );
        }
    }

    private void validateSkuForUpdate(
            Long companyId,
            Long currentProductId,
            String sku
    ) {
        if (sku == null || sku.isBlank()) {
            return;
        }

        boolean skuUsedByAnotherProduct =
                productRepository
                        .findByCompanyIdOrderByNameAsc(
                                companyId
                        )
                        .stream()
                        .anyMatch(product ->
                                !Objects.equals(
                                        product.getId(),
                                        currentProductId
                                )
                                        && product.getSku() != null
                                        && product.getSku()
                                        .equalsIgnoreCase(
                                                sku.trim()
                                        )
                        );

        if (skuUsedByAnotherProduct) {
            throw new ResourceNotFoundException(
                    "A product with this SKU already exists"
            );
        }
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
                "PRODUCT",
                entityId,
                details
        );
    }
}