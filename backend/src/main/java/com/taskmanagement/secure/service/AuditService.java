package com.taskmanagement.secure.service;

import com.taskmanagement.secure.dto.audit.TaskAuditLogResponse;
import com.taskmanagement.secure.dto.common.PagedResponse;
import com.taskmanagement.secure.entity.Task;
import com.taskmanagement.secure.entity.TaskAuditLog;
import com.taskmanagement.secure.entity.User;
import com.taskmanagement.secure.exception.ForbiddenException;
import com.taskmanagement.secure.repository.TaskAuditLogRepository;
import com.taskmanagement.secure.security.TaskSecurityEvaluator;
import com.taskmanagement.secure.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuditService {

    private final TaskAuditLogRepository auditLogRepository;
    private final TaskSecurityEvaluator taskSecurityEvaluator;

    public AuditService(TaskAuditLogRepository auditLogRepository, TaskSecurityEvaluator taskSecurityEvaluator) {
        this.auditLogRepository = auditLogRepository;
        this.taskSecurityEvaluator = taskSecurityEvaluator;
    }

    @Transactional
    public void logTaskAction(Task task, String action, String fieldChanged, String oldValue, String newValue, User actor) {
        String actorEmail = actor != null ? actor.getEmail() : "system@tasksystem.com";
        TaskAuditLog log = new TaskAuditLog(task, action, fieldChanged, oldValue, newValue, actor, actorEmail);
        auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public List<TaskAuditLogResponse> getTaskAuditLogs(Long taskId, UserPrincipal principal) {
        if (!taskSecurityEvaluator.canViewTask(taskId, principal)) {
            throw new ForbiddenException("You are not authorized to view the history for this task");
        }

        return auditLogRepository.findByTaskIdWithActor(taskId).stream()
                .map(TaskAuditLogResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PagedResponse<TaskAuditLogResponse> getAllAuditLogs(Pageable pageable) {
        Page<TaskAuditLog> page = auditLogRepository.findAllWithDetails(pageable);
        return PagedResponse.from(page.map(TaskAuditLogResponse::fromEntity));
    }
}
