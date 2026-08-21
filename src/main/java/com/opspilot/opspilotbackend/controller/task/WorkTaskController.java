package com.opspilot.opspilotbackend.controller.task;

import com.opspilot.opspilotbackend.dto.ManagerTaskRequestDto;
import com.opspilot.opspilotbackend.dto.TaskStatusUpdateRequestDto;
import com.opspilot.opspilotbackend.dto.WorkTaskRequestDto;
import com.opspilot.opspilotbackend.dto.WorkTaskResponseDto;
import com.opspilot.opspilotbackend.service.NotificationService;
import com.opspilot.opspilotbackend.service.WorkTaskService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/work-tasks")
public class WorkTaskController {

    private final WorkTaskService workTaskService;
    private final NotificationService notificationService;

    public WorkTaskController(
            WorkTaskService workTaskService,
            NotificationService notificationService) {

        this.workTaskService = workTaskService;
        this.notificationService = notificationService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public WorkTaskResponseDto createTask(
            @Valid @RequestBody WorkTaskRequestDto request) {

        return workTaskService.createTask(request);
    }

    @PostMapping("/manager")
    @PreAuthorize("hasRole('MANAGER')")
    public WorkTaskResponseDto createTaskForManager(
            Authentication authentication,
            @Valid @RequestBody ManagerTaskRequestDto request) {

        WorkTaskResponseDto createdTask =
                workTaskService.createTaskForManager(
                        authentication.getName(),
                        request
                );

        String message = "You have been assigned: "
                + createdTask.getTitle();

        if (createdTask.getDueDate() != null) {
            message += ". Deadline: " + createdTask.getDueDate();
        }

        notificationService.createNotification(
                createdTask.getAssignedToUserId(),
                "TASK_ASSIGNED",
                "New task assigned",
                message
        );

        return createdTask;
    }

    @PatchMapping("/{id}/my-status")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public WorkTaskResponseDto updateMyTaskStatus(
            @PathVariable Long id,
            Authentication authentication,
            @Valid @RequestBody TaskStatusUpdateRequestDto request) {

        WorkTaskResponseDto updatedTask =
                workTaskService.updateTaskStatusForEmployee(
                        authentication.getName(),
                        id,
                        request
                );

        notificationService.createNotification(
                updatedTask.getCreatedByUserId(),
                "TASK_PROGRESS",
                "Task progress updated",
                "Task \"" + updatedTask.getTitle()
                        + "\" is now "
                        + updatedTask.getStatus()
                        .name()
                        .toLowerCase()
                        .replace("_", " ")
        );

        return updatedTask;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<WorkTaskResponseDto> getAllTasks() {

        return workTaskService.getAllTasks();
    }

    @GetMapping("/my")
    @PreAuthorize(
            "hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')"
    )
    public List<WorkTaskResponseDto> getMyTasks(
            Authentication authentication) {

        return workTaskService.getTasksForCurrentUser(
                authentication.getName()
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public WorkTaskResponseDto getTaskById(
            @PathVariable Long id) {

        return workTaskService.getTaskById(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public WorkTaskResponseDto updateTask(
            @PathVariable Long id,
            @Valid @RequestBody WorkTaskRequestDto request) {

        return workTaskService.updateTask(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteTask(@PathVariable Long id) {

        workTaskService.deleteTask(id);

        return "Task deleted successfully";
    }
}