package com.opspilot.opspilotbackend.repository;

import com.opspilot.opspilotbackend.entity.EmployeeLeave;
import com.opspilot.opspilotbackend.entity.LeaveStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface EmployeeLeaveRepository
        extends JpaRepository<EmployeeLeave, Long> {

    List<EmployeeLeave>
    findByCompanyIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long companyId,
            LeaveStatus status,
            LocalDate latestStartDate,
            LocalDate earliestEndDate
    );
}
