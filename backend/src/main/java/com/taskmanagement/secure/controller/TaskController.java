package com.taskmanagement.secure.controller;

import com.taskmanagement.secure.dto.audit.TaskAuditLogResponse;
import com.taskmanagement.secure.dto.common.ApiResponse;
import com.taskmanagement.secure.dto.common.PagedResponse;
import com.taskmanagement.secure.dto.task.CreateTaskRequest;
import com.taskmanagement.secure.dto.task.TaskResponse;
import com.taskmanagement.secure.dto.task.UpdateTaskRequest;
import com.taskmanagement.secure.dto.task.UpdateTaskStatusRequest;
import com.taskmanagement.secure.entity.TaskPriority;
import com.taskmanagement.secure.entity.TaskStatus;
import com.taskmanagement.secure.security.UserPrincipal;
import com.taskmanagement.secure.service.AuditService;
import com.taskmanagement.secure.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tasks")
@Tag(name = "Task Management", description = "Endpoints for creating, managing, filtering, and updating tasks")
public class TaskController {

    private final TaskService taskService;
    private final AuditService auditService;

    public TaskController(TaskService taskService, AuditService auditService) {
        this.taskService = taskService;
        this.auditService = auditService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Create a new task (Admin or team manager)")
    public ResponseEntity<TaskResponse> createTask(
            @Valid @RequestBody CreateTaskRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.createTask(request, principal));
    }

    @GetMapping
    @Operation(summary = "List and filter tasks with pagination and sorting")
    public ResponseEntity<PagedResponse<TaskResponse>> listTasks(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskPriority priority,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(required = false) Long teamId,
            @RequestParam(required = false) Boolean overdue,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.ok(taskService.listTasks(
                search, status, priority, assigneeId, teamId, overdue, pageable, principal));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get task details by ID with authorization verification")
    public ResponseEntity<TaskResponse> getTaskById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(taskService.getTaskById(id, principal));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Update task details (title, description, priority, assignee, due date)")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTaskRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(taskService.updateTask(id, request, principal));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update task status (Available to assigned user, manager, and admin)")
    public ResponseEntity<TaskResponse> updateTaskStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTaskStatusRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(taskService.updateTaskStatus(id, request, principal));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Delete task (Admin or team manager)")
    public ResponseEntity<ApiResponse> deleteTask(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        taskService.deleteTask(id, principal);
        return ResponseEntity.ok(ApiResponse.success("Task deleted successfully"));
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "Get activity audit history for a task")
    public ResponseEntity<List<TaskAuditLogResponse>> getTaskHistory(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(auditService.getTaskAuditLogs(id, principal));
    }
}
