package com.opspilot.opspilotbackend.repository;
import java.util.Optional;
import com.opspilot.opspilotbackend.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsBySku(String sku);
    Optional<Product> findByNameIgnoreCase(String name);

}