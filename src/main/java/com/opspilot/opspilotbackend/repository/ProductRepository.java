package com.opspilot.opspilotbackend.repository;

import com.opspilot.opspilotbackend.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository
        extends JpaRepository<Product, Long> {

    boolean existsBySku(String sku);

    Optional<Product> findByNameIgnoreCase(String name);

    List<Product> findByCompanyIdOrderByNameAsc(
            Long companyId
    );

    Optional<Product> findByIdAndCompanyId(
            Long id,
            Long companyId
    );

    Optional<Product> findByCompanyIdAndNameIgnoreCase(
            Long companyId,
            String name
    );

    boolean existsByCompanyIdAndSku(
            Long companyId,
            String sku
    );

    long countByCompanyId(Long companyId);
}