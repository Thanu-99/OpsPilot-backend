package com.opspilot.opspilotbackend.service.impl;

import com.opspilot.opspilotbackend.exception.ResourceNotFoundException;
import com.opspilot.opspilotbackend.dto.ProductRequestDto;
import com.opspilot.opspilotbackend.dto.ProductResponseDto;
import com.opspilot.opspilotbackend.entity.Product;
import com.opspilot.opspilotbackend.entity.User;
import com.opspilot.opspilotbackend.mapper.ProductMapper;
import com.opspilot.opspilotbackend.repository.ProductRepository;
import com.opspilot.opspilotbackend.repository.UserRepository;
import com.opspilot.opspilotbackend.service.AuditLogService;
import com.opspilot.opspilotbackend.service.ProductService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final AuditLogService auditLogService;
    private final UserRepository userRepository;

    public ProductServiceImpl(
            ProductRepository productRepository,
            AuditLogService auditLogService,
            UserRepository userRepository) {

        this.productRepository = productRepository;
        this.auditLogService = auditLogService;
        this.userRepository = userRepository;
    }

    @Override
    @CacheEvict(value = "products", allEntries = true)
    public ProductResponseDto createProduct(ProductRequestDto request) {

        if (request.getSku() != null &&
                productRepository.existsBySku(request.getSku())) {

            throw new ResourceNotFoundException(
                    "Product with this SKU already exists"
            );
        }

        Product product = ProductMapper.toEntity(request);
        product = productRepository.save(product);

        audit(
                "CREATE",
                "PRODUCT",
                product.getId(),
                "Created product: " + product.getName()
        );

        return ProductMapper.toResponse(product);
    }

    @Override
    @Cacheable(value = "products")
    public List<ProductResponseDto> getAllProducts() {

        return productRepository.findAll()
                .stream()
                .map(ProductMapper::toResponse)
                .toList();
    }

    @Override
    @Cacheable(value = "products", key = "#id")
    public ProductResponseDto getProductById(Long id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Product not found")
                );

        return ProductMapper.toResponse(product);
    }

    @Override
    @Cacheable(value = "products", key = "'name:' + #name")
    public ProductResponseDto getProductByName(String name) {

        Product product = productRepository.findByNameIgnoreCase(name)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Product not found: " + name
                        )
                );

        return ProductMapper.toResponse(product);
    }

    @Override
    @CacheEvict(value = "products", allEntries = true)
    public ProductResponseDto updateProduct(
            Long id,
            ProductRequestDto request) {

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Product not found")
                );

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setQuantity(request.getQuantity());
        product.setCategory(request.getCategory());
        product.setSku(request.getSku());

        product = productRepository.save(product);

        audit(
                "UPDATE",
                "PRODUCT",
                product.getId(),
                "Updated product: " + product.getName()
        );

        return ProductMapper.toResponse(product);
    }

    @Override
    @CacheEvict(value = "products", allEntries = true)
    public void deleteProduct(Long id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Product not found")
                );

        productRepository.delete(product);

        audit(
                "DELETE",
                "PRODUCT",
                product.getId(),
                "Deleted product: " + product.getName()
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