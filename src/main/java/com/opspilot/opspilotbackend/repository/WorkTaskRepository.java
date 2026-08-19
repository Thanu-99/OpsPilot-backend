package com.opspilot.opspilotbackend.repository;

import com.opspilot.opspilotbackend.entity.TaskStatus;
import com.opspilot.opspilotbackend.entity.WorkTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface WorkTaskRepository extends JpaRepository<WorkTask, Long> {

    List<WorkTask> findByCompanyIdOrderByDueDateAsc(Long companyId);

    List<WorkTask> findByAssignedToUserIdOrderByDueDateAsc(Long assignedToUserId);

    List<WorkTask> findByDepartmentIdOrderByDueDateAsc(Long departmentId);

    List<WorkTask> findByDepartmentIdAndStatusOrderByDueDateAsc(
            Long departmentId,
            TaskStatus status
    );

    List<WorkTask> findByDepartmentIdAndDueDateBeforeOrderByDueDateAsc(
            Long departmentId,
            LocalDate dueDate
    );
}