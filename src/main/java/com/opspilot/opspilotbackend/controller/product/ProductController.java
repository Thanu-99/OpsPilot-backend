package com.opspilot.opspilotbackend.controller.product;
import jakarta.validation.Valid;
import com.opspilot.opspilotbackend.dto.ProductRequestDto;
import com.opspilot.opspilotbackend.dto.ProductResponseDto;
import com.opspilot.opspilotbackend.service.ProductService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ProductResponseDto createProduct(
            @Valid @RequestBody ProductRequestDto request) {

        return productService.createProduct(request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public List<ProductResponseDto> getAllProducts() {

        return productService.getAllProducts();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ProductResponseDto getProductById(
            @PathVariable Long id) {

        return productService.getProductById(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ProductResponseDto updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequestDto request) {

        return productService.updateProduct(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteProduct(
            @PathVariable Long id) {

        productService.deleteProduct(id);
    }
}