package com.opspilot.opspilotbackend.repository;

import com.opspilot.opspilotbackend.entity.Order;
import com.opspilot.opspilotbackend.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository
        extends JpaRepository<Order, Long> {

    /*
     * Kept temporarily for compatibility with older agents.
     */
    List<Order> findByStatus(OrderStatus status);

    List<Order> findByCompanyIdOrderByCreatedAtDesc(
            Long companyId
    );

    List<Order> findByCompanyIdAndStatusOrderByCreatedAtDesc(
            Long companyId,
            OrderStatus status
    );

    Optional<Order> findByIdAndCompanyId(
            Long id,
            Long companyId
    );

    long countByCompanyId(Long companyId);
}