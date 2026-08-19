package com.opspilot.opspilotbackend.service.impl;

import com.opspilot.opspilotbackend.dto.ManagerTaskRequestDto;
import com.opspilot.opspilotbackend.dto.WorkTaskRequestDto;
import com.opspilot.opspilotbackend.dto.WorkTaskResponseDto;
import com.opspilot.opspilotbackend.entity.Department;
import com.opspilot.opspilotbackend.entity.TaskStatus;
import com.opspilot.opspilotbackend.entity.User;
import com.opspilot.opspilotbackend.entity.UserRole;
import com.opspilot.opspilotbackend.entity.WorkTask;
import com.opspilot.opspilotbackend.exception.ResourceNotFoundException;
import com.opspilot.opspilotbackend.mapper.WorkTaskMapper;
import com.opspilot.opspilotbackend.repository.DepartmentRepository;
import com.opspilot.opspilotbackend.repository.UserRepository;
import com.opspilot.opspilotbackend.repository.WorkTaskRepository;
import com.opspilot.opspilotbackend.service.WorkTaskService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@Transactional
public class WorkTaskServiceImpl implements WorkTaskService {

    private final WorkTaskRepository workTaskRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;

    public WorkTaskServiceImpl(
            WorkTaskRepository workTaskRepository,
            UserRepository userRepository,
            DepartmentRepository departmentRepository) {

        this.workTaskRepository = workTaskRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
    }

    @Override
    public WorkTaskResponseDto createTask(WorkTaskRequestDto request) {

        WorkTask task = workTaskRepository.save(
                WorkTaskMapper.toEntity(request)
        );

        return WorkTaskMapper.toResponseDto(task);
    }

    @Override
    public WorkTaskResponseDto createTaskForManager(
            String managerEmail,
            ManagerTaskRequestDto request) {

        User manager = userRepository.findByEmail(managerEmail)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Authenticated user not found"
                        )
                );

        if (manager.getRole() != UserRole.MANAGER) {
            throw new AccessDeniedException(
                    "Only managers can assign team work"
            );
        }

        User employee = userRepository
                .findById(request.getAssignedToUserId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Assigned employee not found"
                        )
                );

        if (employee.getRole() != UserRole.EMPLOYEE
                || !manager.getId().equals(employee.getManagerId())) {
            throw new AccessDeniedException(
                    "You can only assign work to your direct reports"
            );
        }

        Department department = departmentRepository
                .findByManagerId(manager.getId())
                .stream()
                .filter(item ->
                        item.getId().equals(
                                employee.getDepartmentId()
                        )
                )
                .findFirst()
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "This employee is not assigned to one of your departments"
                        )
                );

        WorkTask task = WorkTask.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .status(TaskStatus.TODO)
                .priority(request.getPriority())
                .companyId(manager.getCompanyId())
                .departmentId(department.getId())
                .assignedToUserId(employee.getId())
                .createdByUserId(manager.getId())
                .dueDate(request.getDueDate())
                .build();

        task = workTaskRepository.save(task);

        return WorkTaskMapper.toResponseDto(task);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkTaskResponseDto> getAllTasks() {

        return workTaskRepository.findAll()
                .stream()
                .map(WorkTaskMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkTaskResponseDto> getTasksForCurrentUser(String email) {

        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Authenticated user not found"
                        )
                );

        List<WorkTask> tasks;

        if (currentUser.getRole() == UserRole.ADMIN) {
            tasks = workTaskRepository.findByCompanyIdOrderByDueDateAsc(
                    currentUser.getCompanyId()
            );
        } else if (currentUser.getRole() == UserRole.MANAGER) {
            tasks = departmentRepository
                    .findByManagerId(currentUser.getId())
                    .stream()
                    .flatMap(department ->
                            workTaskRepository
                                    .findByDepartmentIdOrderByDueDateAsc(
                                            department.getId()
                                    )
                                    .stream()
                    )
                    .sorted(Comparator.comparing(
                            WorkTask::getDueDate,
                            Comparator.nullsLast(
                                    Comparator.naturalOrder()
                            )
                    ))
                    .toList();
        } else {
            tasks = workTaskRepository
                    .findByAssignedToUserIdOrderByDueDateAsc(
                            currentUser.getId()
                    );
        }

        return tasks.stream()
                .map(WorkTaskMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public WorkTaskResponseDto getTaskById(Long id) {

        WorkTask task = workTaskRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Task not found")
                );

        return WorkTaskMapper.toResponseDto(task);
    }

    @Override
    public WorkTaskResponseDto updateTask(
            Long id,
            WorkTaskRequestDto request) {

        WorkTask task = workTaskRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Task not found")
                );

        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus(request.getStatus());
        task.setPriority(request.getPriority());
        task.setCompanyId(request.getCompanyId());
        task.setDepartmentId(request.getDepartmentId());
        task.setAssignedToUserId(request.getAssignedToUserId());
        task.setCreatedByUserId(request.getCreatedByUserId());
        task.setDueDate(request.getDueDate());

        task = workTaskRepository.save(task);

        return WorkTaskMapper.toResponseDto(task);
    }

    @Override
    public void deleteTask(Long id) {

        WorkTask task = workTaskRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Task not found")
                );

        workTaskRepository.delete(task);
    }
}