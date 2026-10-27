package com.taskmanagement.secure.service;

import com.taskmanagement.secure.dto.common.PagedResponse;
import com.taskmanagement.secure.dto.task.CreateTaskRequest;
import com.taskmanagement.secure.dto.task.TaskResponse;
import com.taskmanagement.secure.dto.task.UpdateTaskRequest;
import com.taskmanagement.secure.dto.task.UpdateTaskStatusRequest;
import com.taskmanagement.secure.entity.*;
import com.taskmanagement.secure.exception.BadRequestException;
import com.taskmanagement.secure.exception.ForbiddenException;
import com.taskmanagement.secure.exception.ResourceNotFoundException;
import com.taskmanagement.secure.repository.TaskRepository;
import com.taskmanagement.secure.repository.TeamRepository;
import com.taskmanagement.secure.repository.UserRepository;
import com.taskmanagement.secure.security.TaskSecurityEvaluator;
import com.taskmanagement.secure.security.UserPrincipal;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final AuditService auditService;
    private final TaskSecurityEvaluator taskSecurityEvaluator;

    public TaskService(
            TaskRepository taskRepository,
            UserRepository userRepository,
            TeamRepository teamRepository,
            AuditService auditService,
            TaskSecurityEvaluator taskSecurityEvaluator) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.auditService = auditService;
        this.taskSecurityEvaluator = taskSecurityEvaluator;
    }

    @Transactional
    public TaskResponse createTask(CreateTaskRequest request, UserPrincipal principal) {
        boolean isAdmin = hasRole(principal, RoleName.ROLE_ADMIN);
        boolean isManager = hasRole(principal, RoleName.ROLE_MANAGER);

        if (!isAdmin && !isManager) {
            throw new ForbiddenException("Only administrators and managers are authorized to create tasks.");
        }

        User creator = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", principal.getId()));

        Team team = null;
        if (request.getTeamId() != null) {
            team = teamRepository.findById(request.getTeamId())
                    .orElseThrow(() -> new ResourceNotFoundException("Team", "id", request.getTeamId()));

            // If manager, must manage this team
            if (isManager && !isAdmin) {
                if (team.getManager() == null || !team.getManager().getId().equals(principal.getId())) {
                    throw new ForbiddenException("Managers can only create tasks for their own managed teams.");
                }
            }
        }

        User assignee = null;
        if (request.getAssigneeId() != null) {
            assignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getAssigneeId()));

            if (!assignee.isActive()) {
                throw new BadRequestException("Cannot assign task to a deactivated user.");
            }

            // If manager, assignee must belong to the team
            if (isManager && !isAdmin && team != null) {
                if (!team.hasMember(assignee.getId()) && !team.getManager().getId().equals(assignee.getId())) {
                    throw new BadRequestException("Managers can only assign tasks to members of their team.");
                }
            }
        }

        Task task = new Task(
                request.getTitle().trim(),
                request.getDescription(),
                request.getPriority(),
                request.getStatus(),
                creator,
                assignee,
                team,
                request.getDueDate()
        );

        Task savedTask = taskRepository.save(task);

        auditService.logTaskAction(savedTask, "CREATED", null, null, null, creator);

        return TaskResponse.fromEntity(savedTask);
    }

    @Transactional(readOnly = true)
    public TaskResponse getTaskById(Long id, UserPrincipal principal) {
        if (!taskSecurityEvaluator.canViewTask(id, principal)) {
            throw new ForbiddenException("You are not authorized to access this task.");
        }

        Task task = taskRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", id));

        return TaskResponse.fromEntity(task);
    }

    @Transactional
    public TaskResponse updateTask(Long id, UpdateTaskRequest request, UserPrincipal principal) {
        if (!taskSecurityEvaluator.canModifyTask(id, principal)) {
            throw new ForbiddenException("You are not authorized to modify this task.");
        }

        Task task = taskRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", id));

        User actor = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", principal.getId()));

        // Audit title change
        if (!Objects.equals(task.getTitle(), request.getTitle())) {
            auditService.logTaskAction(task, "UPDATED", "title", task.getTitle(), request.getTitle(), actor);
            task.setTitle(request.getTitle().trim());
        }

        // Audit description change
        if (!Objects.equals(task.getDescription(), request.getDescription())) {
            auditService.logTaskAction(task, "UPDATED", "description", "...", "...", actor);
            task.setDescription(request.getDescription());
        }

        // Audit priority change
        if (request.getPriority() != null && task.getPriority() != request.getPriority()) {
            auditService.logTaskAction(task, "PRIORITY_CHANGED", "priority", task.getPriority().name(), request.getPriority().name(), actor);
            task.setPriority(request.getPriority());
        }

        // Audit status change
        if (request.getStatus() != null && task.getStatus() != request.getStatus()) {
            auditService.logTaskAction(task, "STATUS_CHANGED", "status", task.getStatus().name(), request.getStatus().name(), actor);
            task.setStatus(request.getStatus());
            if (request.getStatus() == TaskStatus.COMPLETED) {
                task.setCompletedAt(Instant.now());
            } else {
                task.setCompletedAt(null);
            }
        }

        // Audit due date change
        if (!Objects.equals(task.getDueDate(), request.getDueDate())) {
            auditService.logTaskAction(task, "DUE_DATE_CHANGED", "dueDate",
                    task.getDueDate() != null ? task.getDueDate().toString() : "none",
                    request.getDueDate() != null ? request.getDueDate().toString() : "none",
                    actor);
            task.setDueDate(request.getDueDate());
        }

        // Team change
        if (request.getTeamId() != null) {
            Team newTeam = teamRepository.findById(request.getTeamId())
                    .orElseThrow(() -> new ResourceNotFoundException("Team", "id", request.getTeamId()));
            task.setTeam(newTeam);
        }

        // Assignee change
        if (request.getAssigneeId() != null) {
            if (task.getAssignee() == null || !task.getAssignee().getId().equals(request.getAssigneeId())) {
                User newAssignee = userRepository.findById(request.getAssigneeId())
                        .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getAssigneeId()));
                if (!newAssignee.isActive()) {
                    throw new BadRequestException("Cannot assign task to deactivated user.");
                }
                String oldAssigneeEmail = task.getAssignee() != null ? task.getAssignee().getEmail() : "unassigned";
                auditService.logTaskAction(task, "REASSIGNED", "assignee", oldAssigneeEmail, newAssignee.getEmail(), actor);
                task.setAssignee(newAssignee);
            }
        } else if (task.getAssignee() != null && request.getAssigneeId() == null) {
            // Unassigning
            auditService.logTaskAction(task, "REASSIGNED", "assignee", task.getAssignee().getEmail(), "unassigned", actor);
            task.setAssignee(null);
        }

        Task saved = taskRepository.save(task);
        return TaskResponse.fromEntity(saved);
    }

    @Transactional
    public TaskResponse updateTaskStatus(Long id, UpdateTaskStatusRequest request, UserPrincipal principal) {
        if (!taskSecurityEvaluator.canUpdateTaskStatus(id, principal)) {
            throw new ForbiddenException("You are not authorized to update the status of this task.");
        }

        Task task = taskRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", id));

        User actor = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", principal.getId()));

        if (task.getStatus() != request.getStatus()) {
            auditService.logTaskAction(task, "STATUS_CHANGED", "status", task.getStatus().name(), request.getStatus().name(), actor);
            task.setStatus(request.getStatus());
            if (request.getStatus() == TaskStatus.COMPLETED) {
                task.setCompletedAt(Instant.now());
            } else {
                task.setCompletedAt(null);
            }
        }

        Task saved = taskRepository.save(task);
        return TaskResponse.fromEntity(saved);
    }

    @Transactional
    public void deleteTask(Long id, UserPrincipal principal) {
        if (!taskSecurityEvaluator.canDeleteTask(id, principal)) {
            throw new ForbiddenException("You are not authorized to delete this task.");
        }

        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", id));

        User actor = userRepository.findById(principal.getId()).orElse(null);
        auditService.logTaskAction(task, "DELETED", null, null, null, actor);

        taskRepository.delete(task);
    }

    @Transactional(readOnly = true)
    public PagedResponse<TaskResponse> listTasks(
            String search,
            TaskStatus status,
            TaskPriority priority,
            Long assigneeId,
            Long teamId,
            Boolean overdue,
            Pageable pageable,
            UserPrincipal principal) {

        boolean isAdmin = hasRole(principal, RoleName.ROLE_ADMIN);
        boolean isManager = hasRole(principal, RoleName.ROLE_MANAGER);

        List<Long> managerTeamIds = new ArrayList<>();
        if (isManager && !isAdmin) {
            managerTeamIds = teamRepository.findByManagerId(principal.getId()).stream()
                    .map(Team::getId)
                    .collect(Collectors.toList());
        }

        final List<Long> accessibleTeamIds = managerTeamIds;

        Specification<Task> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Scoping according to user role
            if (isAdmin) {
                // Admin can see everything, apply optional requested team/assignee filters
                if (assigneeId != null) {
                    predicates.add(cb.equal(root.get("assignee").get("id"), assigneeId));
                }
                if (teamId != null) {
                    predicates.add(cb.equal(root.get("team").get("id"), teamId));
                }
            } else if (isManager) {
                // Manager sees tasks from their teams OR created by them OR assigned to them
                Predicate teamMatch = root.get("team").get("id").in(accessibleTeamIds);
                Predicate creatorMatch = cb.equal(root.get("creator").get("id"), principal.getId());
                Predicate assigneeMatch = cb.equal(root.get("assignee").get("id"), principal.getId());

                if (accessibleTeamIds.isEmpty()) {
                    predicates.add(cb.or(creatorMatch, assigneeMatch));
                } else {
                    predicates.add(cb.or(teamMatch, creatorMatch, assigneeMatch));
                }

                if (assigneeId != null) {
                    predicates.add(cb.equal(root.get("assignee").get("id"), assigneeId));
                }
                if (teamId != null && accessibleTeamIds.contains(teamId)) {
                    predicates.add(cb.equal(root.get("team").get("id"), teamId));
                }
            } else {
                // Regular user: can only see tasks assigned to them
                predicates.add(cb.equal(root.get("assignee").get("id"), principal.getId()));
            }

            // 2. Keyword Search on title or description
            if (StringUtils.hasText(search)) {
                String searchPattern = "%" + search.trim().toLowerCase() + "%";
                Predicate titleLike = cb.like(cb.lower(root.get("title")), searchPattern);
                Predicate descLike = cb.like(cb.lower(root.get("description")), searchPattern);
                predicates.add(cb.or(titleLike, descLike));
            }

            // 3. Status filter
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            // 4. Priority filter
            if (priority != null) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }

            // 5. Overdue filter
            if (Boolean.TRUE.equals(overdue)) {
                predicates.add(cb.lessThan(root.get("dueDate"), Instant.now()));
                predicates.add(cb.notEqual(root.get("status"), TaskStatus.COMPLETED));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Task> page = taskRepository.findAll(spec, pageable);
        return PagedResponse.from(page.map(TaskResponse::fromEntity));
    }

    private boolean hasRole(UserPrincipal principal, RoleName roleName) {
        return principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(roleName.name()));
    }
}
