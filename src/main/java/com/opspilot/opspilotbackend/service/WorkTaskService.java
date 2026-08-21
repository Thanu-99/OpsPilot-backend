package com.opspilot.opspilotbackend.service;

import com.opspilot.opspilotbackend.dto.ManagerTaskRequestDto;
import com.opspilot.opspilotbackend.dto.TaskStatusUpdateRequestDto;
import com.opspilot.opspilotbackend.dto.WorkTaskRequestDto;
import com.opspilot.opspilotbackend.dto.WorkTaskResponseDto;

import java.util.List;

public interface WorkTaskService {

    WorkTaskResponseDto createTask(WorkTaskRequestDto request);

    WorkTaskResponseDto createTaskForManager(
            String managerEmail,
            ManagerTaskRequestDto request
    );

    WorkTaskResponseDto updateTaskStatusForEmployee(
            String employeeEmail,
            Long taskId,
            TaskStatusUpdateRequestDto request
    );

    List<WorkTaskResponseDto> getAllTasks();

    List<WorkTaskResponseDto> getTasksForCurrentUser(String email);

    WorkTaskResponseDto getTaskById(Long id);

    WorkTaskResponseDto updateTask(
            Long id,
            WorkTaskRequestDto request
    );

    void deleteTask(Long id);
}