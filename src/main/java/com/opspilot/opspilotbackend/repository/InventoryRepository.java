package com.opspilot.opspilotbackend.repository;

import com.opspilot.opspilotbackend.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    boolean existsByProductId(Long productId);

    Optional<Inventory> findByProductId(Long productId);

    @Query("""
            SELECT COUNT(i)
            FROM Inventory i
            WHERE i.active = true
            AND i.quantity <= i.reorderLevel
            """)
    long countLowStockItems();
}