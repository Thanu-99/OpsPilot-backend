package com.opspilot.opspilotbackend.ai.agent;

import com.opspilot.opspilotbackend.dto.ProductResponseDto;
import com.opspilot.opspilotbackend.service.ProductService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductAgent {

    private final ProductService productService;

    public ProductAgent(ProductService productService) {
        this.productService = productService;
    }

    public String handle(String message) {

        String lower = message.toLowerCase();

        if (lower.contains("how many products")
                || lower.contains("total products")) {

            List<ProductResponseDto> products =
                    productService.getAllProducts();

            int count = products.size();

            return count == 1
                    ? "There is currently 1 product."
                    : "There are currently " + count + " products.";
        }

        if (lower.contains("show me all products")
                || lower.contains("list all products")
                || lower.contains("what products are available")) {

            List<ProductResponseDto> products =
                    productService.getAllProducts();

            if (products.isEmpty()) {
                return "There are currently no products.";
            }

            StringBuilder response =
                    new StringBuilder("Available products:\n");

            for (ProductResponseDto product : products) {

                response.append("• ")
                        .append(product.getName())
                        .append(" — ₹")
                        .append(product.getPrice())
                        .append("\n");
            }

            return response.toString().trim();
        }

        return null;
    }
}