package com.opspilot.opspilotbackend.ai.service;

import com.opspilot.opspilotbackend.entity.Company;
import com.opspilot.opspilotbackend.entity.Department;
import com.opspilot.opspilotbackend.entity.EmployeeLeave;
import com.opspilot.opspilotbackend.entity.Inventory;
import com.opspilot.opspilotbackend.entity.LeaveStatus;
import com.opspilot.opspilotbackend.entity.Order;
import com.opspilot.opspilotbackend.entity.OrderStatus;
import com.opspilot.opspilotbackend.entity.Product;
import com.opspilot.opspilotbackend.entity.TaskStatus;
import com.opspilot.opspilotbackend.entity.User;
import com.opspilot.opspilotbackend.entity.UserRole;
import com.opspilot.opspilotbackend.entity.WorkTask;
import com.opspilot.opspilotbackend.repository.CompanyRepository;
import com.opspilot.opspilotbackend.repository.DepartmentRepository;
import com.opspilot.opspilotbackend.repository.EmployeeLeaveRepository;
import com.opspilot.opspilotbackend.repository.InventoryRepository;
import com.opspilot.opspilotbackend.repository.OrderRepository;
import com.opspilot.opspilotbackend.repository.ProductRepository;
import com.opspilot.opspilotbackend.repository.UserRepository;
import com.opspilot.opspilotbackend.repository.WorkTaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AIContextService {

    private static final int MAX_DETAIL_ROWS = 12;

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final DepartmentRepository departmentRepository;
    private final EmployeeLeaveRepository employeeLeaveRepository;
    private final WorkTaskRepository workTaskRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final OrderRepository orderRepository;

    public AIContextService(
            UserRepository userRepository,
            CompanyRepository companyRepository,
            DepartmentRepository departmentRepository,
            EmployeeLeaveRepository employeeLeaveRepository,
            WorkTaskRepository workTaskRepository,
            ProductRepository productRepository,
            InventoryRepository inventoryRepository,
            OrderRepository orderRepository
    ) {
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.departmentRepository = departmentRepository;
        this.employeeLeaveRepository = employeeLeaveRepository;
        this.workTaskRepository = workTaskRepository;
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public String buildVerifiedContext(
            String email,
            String question
    ) {
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Authenticated user was not found"
                        )
                );

        StringBuilder context = new StringBuilder();

        QuestionScope scope = determineScope(question);

        appendIdentity(context, currentUser);

        switch (currentUser.getRole()) {
            case ADMIN ->
                    appendAdminContext(
                            context,
                            currentUser,
                            scope
                    );

            case MANAGER ->
                    appendManagerContext(
                            context,
                            currentUser,
                            scope
                    );

            case EMPLOYEE ->
                    appendEmployeeContext(context, currentUser);
        }

        return context.toString();
    }

    private void appendIdentity(
            StringBuilder context,
            User currentUser
    ) {
        Company company = companyRepository
                .findById(currentUser.getCompanyId())
                .orElse(null);

        context.append("AUTHENTICATED IDENTITY\n");
        appendLine(context, "User ID", currentUser.getId());
        appendLine(context, "Name", fullName(currentUser));
        appendLine(context, "Email", currentUser.getEmail());
        appendLine(context, "Role", currentUser.getRole());
        appendLine(context, "Company ID", currentUser.getCompanyId());
        appendLine(
                context,
                "Company",
                company == null ? "Unknown" : company.getName()
        );
        appendLine(
                context,
                "Department ID",
                valueOrNone(currentUser.getDepartmentId())
        );
        appendLine(
                context,
                "Manager ID",
                valueOrNone(currentUser.getManagerId())
        );
        context.append("\n");

        context.append("COMPANY OFFICE POLICY\n");
        appendLine(
                context,
                "Office start time",
                company == null ? null : company.getOfficeStartTime()
        );
        appendLine(
                context,
                "Office end time",
                company == null ? null : company.getOfficeEndTime()
        );
        appendLine(
                context,
                "Working days",
                company == null ? null : company.getWorkingDays()
        );
        appendLine(
                context,
                "Company timezone",
                company == null ? null : company.getTimezone()
        );
        context.append("\n");
    }

    private void appendAdminContext(
            StringBuilder context,
            User admin,
            QuestionScope scope
    ) {
        Long companyId = admin.getCompanyId();

        List<User> users =
                userRepository
                        .findByCompanyIdOrderByFirstNameAscLastNameAsc(
                                companyId
                        );

        List<Department> departments =
                departmentRepository
                        .findByCompanyIdAndActiveTrue(companyId);

        List<WorkTask> tasks =
                workTaskRepository
                        .findByCompanyIdOrderByDueDateAsc(
                                companyId
                        );

        context.append("ACCESS POLICY\n");
        context.append(
                "Administrator access: company-wide people, departments, reporting lines, work tasks, products, inventory, orders and analytics for this company only.\n\n"
        );

        if (scope.includePeopleAndWork()) {
            appendPeople(context, users);
            appendDepartments(context, departments, users);
            appendTasks(context, tasks, users, departments);
            appendLeaveSummary(context, companyId, users);
        }

        if (scope.includeOperations()) {
            appendOperations(context, companyId);
        }
    }

    private void appendManagerContext(
            StringBuilder context,
            User manager,
            QuestionScope scope
    ) {
        Long companyId = manager.getCompanyId();

        List<User> directReports =
                userRepository
                        .findByManagerIdAndActiveTrue(
                                manager.getId()
                        )
                        .stream()
                        .filter(user ->
                                Objects.equals(
                                        user.getCompanyId(),
                                        companyId
                                )
                        )
                        .toList();

        List<Department> managedDepartments =
                departmentRepository
                        .findByManagerId(manager.getId())
                        .stream()
                        .filter(department ->
                                Objects.equals(
                                        department.getCompanyId(),
                                        companyId
                                )
                        )
                        .toList();

        Map<Long, WorkTask> visibleTasks =
                new LinkedHashMap<>();

        for (Department department : managedDepartments) {
            workTaskRepository
                    .findByDepartmentIdOrderByDueDateAsc(
                            department.getId()
                    )
                    .stream()
                    .filter(task ->
                            Objects.equals(
                                    task.getCompanyId(),
                                    companyId
                            )
                    )
                    .forEach(task ->
                            visibleTasks.put(
                                    task.getId(),
                                    task
                            )
                    );
        }

        for (User employee : directReports) {
            workTaskRepository
                    .findByAssignedToUserIdOrderByDueDateAsc(
                            employee.getId()
                    )
                    .stream()
                    .filter(task ->
                            Objects.equals(
                                    task.getCompanyId(),
                                    companyId
                            )
                    )
                    .forEach(task ->
                            visibleTasks.put(
                                    task.getId(),
                                    task
                            )
                    );
        }

        context.append("ACCESS POLICY\n");
        context.append(
                "Manager access: direct reports, managed departments, visible team tasks and company operational data. Unrelated reporting lines and administrator-only information are excluded.\n\n"
        );

        if (scope.includePeopleAndWork()) {
            appendPeople(context, directReports);

            appendDepartments(
                    context,
                    managedDepartments,
                    directReports
            );

            appendTasks(
                    context,
                    new ArrayList<>(visibleTasks.values()),
                    directReports,
                    managedDepartments
            );
            appendLeaveSummary(context, companyId, directReports);
        }

        if (scope.includeOperations()) {
            appendOperations(context, companyId);
        }
    }

    private QuestionScope determineScope(String question) {
        String normalized = question == null
                ? ""
                : question.toLowerCase(Locale.ROOT);

        boolean peopleAndWork = containsAny(
                normalized,
                "employee", "team", "manager", "people", "staff",
                "department", "task", "work", "workload", "deadline",
                "overdue", "blocked", "assignment", "priority",
                "leave", "absent", "absence", "holiday", "office",
                "timing", "hours", "schedule", "policy"
        );

        boolean operations = containsAny(
                normalized,
                "business", "operation", "product", "inventory", "stock",
                "order", "sale", "revenue", "profit", "money", "customer",
                "sku", "performance"
        );

        if (!peopleAndWork && !operations) {
            return new QuestionScope(true, true);
        }

        return new QuestionScope(peopleAndWork, operations);
    }

    private boolean containsAny(
            String value,
            String... keywords
    ) {
        for (String keyword : keywords) {
            if (value.contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    private record QuestionScope(
            boolean includePeopleAndWork,
            boolean includeOperations
    ) {
    }

    private void appendEmployeeContext(
            StringBuilder context,
            User employee
    ) {
        List<WorkTask> personalTasks =
                workTaskRepository
                        .findByAssignedToUserIdOrderByDueDateAsc(
                                employee.getId()
                        )
                        .stream()
                        .filter(task ->
                                Objects.equals(
                                        task.getCompanyId(),
                                        employee.getCompanyId()
                                )
                        )
                        .toList();

        List<Department> visibleDepartments =
                new ArrayList<>();

        if (employee.getDepartmentId() != null) {
            departmentRepository
                    .findById(employee.getDepartmentId())
                    .filter(department ->
                            Objects.equals(
                                    department.getCompanyId(),
                                    employee.getCompanyId()
                            )
                    )
                    .ifPresent(visibleDepartments::add);
        }

        context.append("ACCESS POLICY\n");
        context.append(
                "Employee access: personal identity, department identity, assigned tasks, priorities, deadlines and task status only. Company-wide personnel, revenue, orders and confidential administration data are excluded.\n\n"
        );

        appendDepartments(
                context,
                visibleDepartments,
                List.of(employee)
        );

        appendTasks(
                context,
                personalTasks,
                List.of(employee),
                visibleDepartments
        );

        appendLeaveSummary(
                context,
                employee.getCompanyId(),
                List.of(employee)
        );
    }

    private void appendLeaveSummary(
            StringBuilder context,
            Long companyId,
            List<User> visibleUsers
    ) {
        LocalDate today = LocalDate.now();
        Set<Long> visibleUserIds = visibleUsers.stream()
                .map(User::getId)
                .collect(Collectors.toSet());

        long visibleLeaveCount = employeeLeaveRepository
                .findByCompanyIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        companyId,
                        LeaveStatus.APPROVED,
                        today,
                        today
                )
                .stream()
                .map(EmployeeLeave::getUserId)
                .filter(visibleUserIds::contains)
                .distinct()
                .count();

        context.append("LEAVE SUMMARY\n");
        appendLine(context, "Date", today);
        appendLine(
                context,
                "Visible employees on approved leave today",
                visibleLeaveCount
        );
        context.append("\n");
    }

    private void appendPeople(
            StringBuilder context,
            List<User> users
    ) {
        long administrators =
                countUsersByRole(users, UserRole.ADMIN);

        long managers =
                countUsersByRole(users, UserRole.MANAGER);

        long employees =
                countUsersByRole(users, UserRole.EMPLOYEE);

        long activeUsers = users.stream()
                .filter(User::isActive)
                .count();

        context.append("PEOPLE SUMMARY\n");
        appendLine(context, "Visible users", users.size());
        appendLine(context, "Active users", activeUsers);
        appendLine(context, "Administrators", administrators);
        appendLine(context, "Managers", managers);
        appendLine(context, "Employees", employees);

        context.append("VISIBLE PEOPLE\n");

        users.stream()
                .limit(MAX_DETAIL_ROWS)
                .forEach(user -> context
                        .append("- ")
                        .append(fullName(user))
                        .append(" | role=")
                        .append(user.getRole())
                        .append(" | active=")
                        .append(user.isActive())
                        .append(" | departmentId=")
                        .append(valueOrNone(
                                user.getDepartmentId()
                        ))
                        .append(" | managerId=")
                        .append(valueOrNone(
                                user.getManagerId()
                        ))
                        .append("\n")
                );

        context.append("\n");
    }

    private void appendDepartments(
            StringBuilder context,
            List<Department> departments,
            List<User> visibleUsers
    ) {
        context.append("DEPARTMENTS\n");
        appendLine(
                context,
                "Visible departments",
                departments.size()
        );

        for (Department department : departments) {
            long visibleMembers = visibleUsers.stream()
                    .filter(user ->
                            Objects.equals(
                                    user.getDepartmentId(),
                                    department.getId()
                            )
                    )
                    .count();

            context.append("- ")
                    .append(department.getName())
                    .append(" | id=")
                    .append(department.getId())
                    .append(" | managerId=")
                    .append(valueOrNone(
                            department.getManagerId()
                    ))
                    .append(" | visibleMembers=")
                    .append(visibleMembers)
                    .append(" | active=")
                    .append(department.isActive())
                    .append("\n");
        }

        context.append("\n");
    }

    private void appendTasks(
            StringBuilder context,
            List<WorkTask> tasks,
            List<User> visibleUsers,
            List<Department> departments
    ) {
        Map<Long, String> userNames = visibleUsers.stream()
                .collect(Collectors.toMap(
                        User::getId,
                        this::fullName,
                        (first, second) -> first
                ));

        Map<Long, String> departmentNames =
                departments.stream()
                        .collect(Collectors.toMap(
                                Department::getId,
                                Department::getName,
                                (first, second) -> first
                        ));

        long overdue = tasks.stream()
                .filter(task -> task.getDueDate() != null)
                .filter(task ->
                        task.getDueDate()
                                .isBefore(LocalDate.now())
                )
                .filter(task ->
                        task.getStatus()
                                != TaskStatus.COMPLETED
                )
                .count();

        context.append("TASK SUMMARY\n");
        appendLine(context, "Visible tasks", tasks.size());
        appendLine(
                context,
                "Todo",
                countTasksByStatus(tasks, TaskStatus.TODO)
        );
        appendLine(
                context,
                "In progress",
                countTasksByStatus(
                        tasks,
                        TaskStatus.IN_PROGRESS
                )
        );
        appendLine(
                context,
                "In review",
                countTasksByStatus(
                        tasks,
                        TaskStatus.IN_REVIEW
                )
        );
        appendLine(
                context,
                "Completed",
                countTasksByStatus(
                        tasks,
                        TaskStatus.COMPLETED
                )
        );
        appendLine(
                context,
                "Blocked",
                countTasksByStatus(
                        tasks,
                        TaskStatus.BLOCKED
                )
        );
        appendLine(
                context,
                "Overdue and incomplete",
                overdue
        );

        context.append("VISIBLE TASKS\n");

        tasks.stream()
                .sorted(Comparator.comparing(
                        WorkTask::getDueDate,
                        Comparator.nullsLast(
                                Comparator.naturalOrder()
                        )
                ))
                .limit(MAX_DETAIL_ROWS)
                .forEach(task -> context
                        .append("- ")
                        .append(task.getTitle())
                        .append(" | id=")
                        .append(task.getId())
                        .append(" | status=")
                        .append(task.getStatus())
                        .append(" | priority=")
                        .append(task.getPriority())
                        .append(" | dueDate=")
                        .append(valueOrNone(
                                task.getDueDate()
                        ))
                        .append(" | assignedTo=")
                        .append(userNames.getOrDefault(
                                task.getAssignedToUserId(),
                                "User #"
                                        + task.getAssignedToUserId()
                        ))
                        .append(" | department=")
                        .append(departmentNames.getOrDefault(
                                task.getDepartmentId(),
                                valueOrNone(
                                        task.getDepartmentId()
                                )
                        ))
                        .append("\n")
                );

        context.append("\n");
    }

    private void appendOperations(
            StringBuilder context,
            Long companyId
    ) {
        List<Product> products =
                productRepository
                        .findByCompanyIdOrderByNameAsc(
                                companyId
                        );

        List<Inventory> inventoryItems =
                inventoryRepository
                        .findByProduct_CompanyIdOrderByProduct_NameAsc(
                                companyId
                        );

        List<Order> orders =
                orderRepository
                        .findByCompanyIdOrderByCreatedAtDesc(
                                companyId
                        );

        List<Order> revenueOrders = orders.stream()
                .filter(order ->
                        order.getStatus()
                                != OrderStatus.CANCELLED
                )
                .toList();

        BigDecimal revenue = revenueOrders.stream()
                .map(Order::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(
                        BigDecimal.ZERO,
                        BigDecimal::add
                );

        long lowStock = inventoryItems.stream()
                .filter(item ->
                        item.getQuantity() != null
                )
                .filter(item ->
                        item.getReorderLevel() != null
                )
                .filter(item ->
                        item.getQuantity()
                                <= item.getReorderLevel()
                )
                .count();

        long outOfStock = inventoryItems.stream()
                .filter(item ->
                        item.getQuantity() != null
                )
                .filter(item ->
                        item.getQuantity() <= 0
                )
                .count();

        Map<OrderStatus, Long> ordersByStatus =
                orders.stream()
                        .map(Order::getStatus)
                        .filter(Objects::nonNull)
                        .collect(Collectors.groupingBy(
                                Function.identity(),
                                Collectors.counting()
                        ));

        context.append("OPERATIONAL SUMMARY\n");
        appendLine(context, "Products", products.size());
        appendLine(
                context,
                "Inventory records",
                inventoryItems.size()
        );
        appendLine(context, "Low-stock records", lowStock);
        appendLine(context, "Out-of-stock records", outOfStock);
        appendLine(context, "Total orders", orders.size());
        appendLine(
                context,
                "Non-cancelled orders",
                revenueOrders.size()
        );
        appendLine(
                context,
                "Revenue excluding cancelled orders",
                revenue
        );
        appendLine(
                context,
                "Orders by status",
                ordersByStatus
        );

        context.append("PRODUCTS AND INVENTORY\n");

        products.stream()
                .limit(MAX_DETAIL_ROWS)
                .forEach(product -> {
                    Inventory inventory =
                            inventoryItems.stream()
                                    .filter(item ->
                                            item.getProduct() != null
                                    )
                                    .filter(item ->
                                            Objects.equals(
                                                    item.getProduct()
                                                            .getId(),
                                                    product.getId()
                                            )
                                    )
                                    .findFirst()
                                    .orElse(null);

                    context.append("- ")
                            .append(product.getName())
                            .append(" | id=")
                            .append(product.getId())
                            .append(" | sku=")
                            .append(valueOrNone(product.getSku()))
                            .append(" | category=")
                            .append(valueOrNone(
                                    product.getCategory()
                            ))
                            .append(" | price=")
                            .append(product.getPrice())
                            .append(" | quantity=")
                            .append(
                                    inventory == null
                                            ? product.getQuantity()
                                            : inventory.getQuantity()
                            )
                            .append(" | reorderLevel=")
                            .append(
                                    inventory == null
                                            ? "none"
                                            : inventory.getReorderLevel()
                            )
                            .append(" | active=")
                            .append(product.isActive())
                            .append("\n");
                });

        context.append("RECENT ORDERS\n");

        orders.stream()
                .limit(MAX_DETAIL_ROWS)
                .forEach(order -> context
                        .append("- Order #")
                        .append(order.getId())
                        .append(" | status=")
                        .append(order.getStatus())
                        .append(" | amount=")
                        .append(order.getTotalAmount())
                        .append(" | createdAt=")
                        .append(valueOrNone(
                                order.getCreatedAt()
                        ))
                        .append("\n")
                );

        context.append("\n");
    }

    private long countUsersByRole(
            List<User> users,
            UserRole role
    ) {
        return users.stream()
                .filter(user -> user.getRole() == role)
                .count();
    }

    private long countTasksByStatus(
            List<WorkTask> tasks,
            TaskStatus status
    ) {
        return tasks.stream()
                .filter(task -> task.getStatus() == status)
                .count();
    }

    private String fullName(User user) {
        return (
                safe(user.getFirstName())
                        + " "
                        + safe(user.getLastName())
        ).trim();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String valueOrNone(Object value) {
        return value == null ? "none" : value.toString();
    }

    private void appendLine(
            StringBuilder context,
            String label,
            Object value
    ) {
        context.append(label)
                .append(": ")
                .append(valueOrNone(value))
                .append("\n");
    }
}
