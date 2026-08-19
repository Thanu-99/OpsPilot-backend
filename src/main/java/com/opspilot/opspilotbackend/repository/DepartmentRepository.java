package com.opspilot.opspilotbackend.repository;

import com.opspilot.opspilotbackend.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    List<Department> findByCompanyIdAndActiveTrue(Long companyId);

    List<Department> findByManagerId(Long managerId);
}