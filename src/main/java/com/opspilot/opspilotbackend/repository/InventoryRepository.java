package com.opspilot.opspilotbackend.repository;

import com.opspilot.opspilotbackend.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository
        extends JpaRepository<Inventory, Long> {

    /*
     * Older methods retained for compatibility with existing
     * backend tools and agents.
     */
    boolean existsByProductId(Long productId);

    Optional<Inventory> findByProductId(Long productId);

    @Query("""
            SELECT COUNT(i)
            FROM Inventory i
            WHERE i.active = true
            AND i.quantity <= i.reorderLevel
            """)
    long countLowStockItems();

    /*
     * Company-scoped methods used by secure business services.
     */
    List<Inventory>
    findByProduct_CompanyIdOrderByProduct_NameAsc(
            Long companyId
    );

    Optional<Inventory>
    findByIdAndProduct_CompanyId(
            Long inventoryId,
            Long companyId
    );

    Optional<Inventory>
    findByProduct_IdAndProduct_CompanyId(
            Long productId,
            Long companyId
    );

    boolean
    existsByProduct_IdAndProduct_CompanyId(
            Long productId,
            Long companyId
    );

    @Query("""
            SELECT COUNT(i)
            FROM Inventory i
            WHERE i.active = true
            AND i.product.companyId = :companyId
            AND i.quantity <= i.reorderLevel
            """)
    long countLowStockItemsByCompanyId(
            @Param("companyId") Long companyId
    );
}