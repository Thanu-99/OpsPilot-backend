package com.opspilot.opspilotbackend.controller.product;

import com.opspilot.opspilotbackend.dto.ProductRequestDto;
import com.opspilot.opspilotbackend.dto.ProductResponseDto;
import com.opspilot.opspilotbackend.service.ProductService;
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
    public ProductResponseDto createProduct(
            @RequestBody ProductRequestDto request) {

        return productService.createProduct(request);
    }

    @GetMapping
    public List<ProductResponseDto> getAllProducts() {

        return productService.getAllProducts();
    }

    @GetMapping("/{id}")
    public ProductResponseDto getProductById(
            @PathVariable Long id) {

        return productService.getProductById(id);
    }

    @PutMapping("/{id}")
    public ProductResponseDto updateProduct(
            @PathVariable Long id,
            @RequestBody ProductRequestDto request) {

        return productService.updateProduct(id, request);
    }

    @DeleteMapping("/{id}")
    public void deleteProduct(
            @PathVariable Long id) {

        productService.deleteProduct(id);
    }
}