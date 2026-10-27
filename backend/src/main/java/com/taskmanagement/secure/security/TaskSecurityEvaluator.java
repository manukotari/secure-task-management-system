package com.taskmanagement.secure.security;

import com.taskmanagement.secure.entity.RoleName;
import com.taskmanagement.secure.entity.Task;
import com.taskmanagement.secure.repository.TaskRepository;
import com.taskmanagement.secure.repository.TeamRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component("taskSecurity")
public class TaskSecurityEvaluator {

    private final TaskRepository taskRepository;
    private final TeamRepository teamRepository;

    public TaskSecurityEvaluator(TaskRepository taskRepository, TeamRepository teamRepository) {
        this.taskRepository = taskRepository;
        this.teamRepository = teamRepository;
    }

    @Transactional(readOnly = true)
    public boolean canViewTask(Long taskId, UserPrincipal principal) {
        if (principal == null) return false;
        if (hasRole(principal, RoleName.ROLE_ADMIN)) return true;

        Optional<Task> taskOpt = taskRepository.findById(taskId);
        if (taskOpt.isEmpty()) return false;

        Task task = taskOpt.get();

        // Assignee can always view
        if (task.getAssignee() != null && task.getAssignee().getId().equals(principal.getId())) {
            return true;
        }

        // Creator can always view
        if (task.getCreator() != null && task.getCreator().getId().equals(principal.getId())) {
            return true;
        }

        // Team manager or team member can view
        if (task.getTeam() != null) {
            if (task.getTeam().getManager() != null && task.getTeam().getManager().getId().equals(principal.getId())) {
                return true;
            }
            if (task.getTeam().hasMember(principal.getId())) {
                return true;
            }
        }

        return false;
    }

    @Transactional(readOnly = true)
    public boolean canModifyTask(Long taskId, UserPrincipal principal) {
        if (principal == null) return false;
        if (hasRole(principal, RoleName.ROLE_ADMIN)) return true;
        if (!hasRole(principal, RoleName.ROLE_MANAGER)) return false;

        Optional<Task> taskOpt = taskRepository.findById(taskId);
        if (taskOpt.isEmpty()) return false;

        Task task = taskOpt.get();

        // Manager can modify if it belongs to their team or was created by them
        if (task.getTeam() != null && task.getTeam().getManager() != null &&
                task.getTeam().getManager().getId().equals(principal.getId())) {
            return true;
        }

        return task.getCreator() != null && task.getCreator().getId().equals(principal.getId());
    }

    @Transactional(readOnly = true)
    public boolean canUpdateTaskStatus(Long taskId, UserPrincipal principal) {
        if (principal == null) return false;
        if (hasRole(principal, RoleName.ROLE_ADMIN)) return true;

        Optional<Task> taskOpt = taskRepository.findById(taskId);
        if (taskOpt.isEmpty()) return false;

        Task task = taskOpt.get();

        // Assigned user can update their own task status
        if (task.getAssignee() != null && task.getAssignee().getId().equals(principal.getId())) {
            return true;
        }

        // Manager of the team can update status
        if (hasRole(principal, RoleName.ROLE_MANAGER)) {
            if (task.getTeam() != null && task.getTeam().getManager() != null &&
                    task.getTeam().getManager().getId().equals(principal.getId())) {
                return true;
            }
            if (task.getCreator() != null && task.getCreator().getId().equals(principal.getId())) {
                return true;
            }
        }

        return false;
    }

    @Transactional(readOnly = true)
    public boolean canDeleteTask(Long taskId, UserPrincipal principal) {
        if (principal == null) return false;
        if (hasRole(principal, RoleName.ROLE_ADMIN)) return true;
        if (!hasRole(principal, RoleName.ROLE_MANAGER)) return false;

        Optional<Task> taskOpt = taskRepository.findById(taskId);
        if (taskOpt.isEmpty()) return false;

        Task task = taskOpt.get();

        // Manager of the team can delete
        return task.getTeam() != null && task.getTeam().getManager() != null &&
                task.getTeam().getManager().getId().equals(principal.getId());
    }

    private boolean hasRole(UserPrincipal principal, RoleName roleName) {
        return principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(roleName.name()));
    }
}
