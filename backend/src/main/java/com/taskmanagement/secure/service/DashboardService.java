package com.taskmanagement.secure.service;

import com.taskmanagement.secure.dto.dashboard.DashboardStatsResponse;
import com.taskmanagement.secure.dto.task.TaskResponse;
import com.taskmanagement.secure.entity.RoleName;
import com.taskmanagement.secure.entity.Task;
import com.taskmanagement.secure.entity.TaskPriority;
import com.taskmanagement.secure.entity.TaskStatus;
import com.taskmanagement.secure.entity.Team;
import com.taskmanagement.secure.repository.TaskRepository;
import com.taskmanagement.secure.repository.TeamRepository;
import com.taskmanagement.secure.repository.UserRepository;
import com.taskmanagement.secure.security.UserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;

    public DashboardService(
            TaskRepository taskRepository,
            UserRepository userRepository,
            TeamRepository teamRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
    }

    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats(UserPrincipal principal) {
        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(RoleName.ROLE_ADMIN.name()));
        boolean isManager = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(RoleName.ROLE_MANAGER.name()));

        DashboardStatsResponse response = new DashboardStatsResponse();
        Instant now = Instant.now();

        if (isAdmin) {
            // Global Statistics for Admin
            response.setTotalTasks(taskRepository.count());
            response.setCompletedTasks(taskRepository.countByStatus(TaskStatus.COMPLETED));
            response.setInProgressTasks(taskRepository.countByStatus(TaskStatus.IN_PROGRESS));
            response.setTodoTasks(taskRepository.countByStatus(TaskStatus.TODO));
            response.setInReviewTasks(taskRepository.countByStatus(TaskStatus.IN_REVIEW));
            response.setOverdueTasks(taskRepository.countOverdueTasks(now));

            Map<String, Long> statusMap = new HashMap<>();
            for (TaskStatus s : TaskStatus.values()) {
                statusMap.put(s.name(), taskRepository.countByStatus(s));
            }
            response.setStatusDistribution(statusMap);

            Map<String, Long> priorityMap = new HashMap<>();
            for (TaskPriority p : TaskPriority.values()) {
                priorityMap.put(p.name(), taskRepository.countByPriority(p));
            }
            response.setPriorityDistribution(priorityMap);

            List<TaskResponse> recent = taskRepository.findTop5ByOrderByCreatedAtDesc().stream()
                    .map(TaskResponse::fromEntity)
                    .collect(Collectors.toList());
            response.setRecentTasks(recent);

            response.setTotalUsers(userRepository.count());
            response.setTotalTeams(teamRepository.count());

        } else if (isManager) {
            // Team-scoped Statistics for Manager
            List<Long> teamIds = teamRepository.findByManagerId(principal.getId()).stream()
                    .map(Team::getId)
                    .collect(Collectors.toList());

            if (teamIds.isEmpty()) {
                response.setTotalTasks(0);
                response.setCompletedTasks(0);
                response.setInProgressTasks(0);
                response.setTodoTasks(0);
                response.setInReviewTasks(0);
                response.setOverdueTasks(0);
                response.setRecentTasks(List.of());
            } else {
                response.setTotalTasks(taskRepository.countByTeamIdIn(teamIds));
                response.setCompletedTasks(taskRepository.countByTeamIdInAndStatus(teamIds, TaskStatus.COMPLETED));
                response.setInProgressTasks(taskRepository.countByTeamIdInAndStatus(teamIds, TaskStatus.IN_PROGRESS));
                response.setTodoTasks(taskRepository.countByTeamIdInAndStatus(teamIds, TaskStatus.TODO));
                response.setInReviewTasks(taskRepository.countByTeamIdInAndStatus(teamIds, TaskStatus.IN_REVIEW));
                response.setOverdueTasks(taskRepository.countOverdueTasksForTeams(teamIds, now));

                Map<String, Long> statusMap = new HashMap<>();
                for (TaskStatus s : TaskStatus.values()) {
                    statusMap.put(s.name(), taskRepository.countByTeamIdInAndStatus(teamIds, s));
                }
                response.setStatusDistribution(statusMap);

                Map<String, Long> priorityMap = new HashMap<>();
                for (TaskPriority p : TaskPriority.values()) {
                    priorityMap.put(p.name(), taskRepository.countByTeamIdInAndPriority(teamIds, p));
                }
                response.setPriorityDistribution(priorityMap);

                List<TaskResponse> recent = taskRepository.findTop5ByTeamIdInOrderByCreatedAtDesc(teamIds).stream()
                        .map(TaskResponse::fromEntity)
                        .collect(Collectors.toList());
                response.setRecentTasks(recent);
            }

        } else {
            // Personal Statistics for User
            Long userId = principal.getId();
            response.setTotalTasks(taskRepository.countByAssigneeId(userId));
            response.setCompletedTasks(taskRepository.countByAssigneeIdAndStatus(userId, TaskStatus.COMPLETED));
            response.setInProgressTasks(taskRepository.countByAssigneeIdAndStatus(userId, TaskStatus.IN_PROGRESS));
            response.setTodoTasks(taskRepository.countByAssigneeIdAndStatus(userId, TaskStatus.TODO));
            response.setInReviewTasks(taskRepository.countByAssigneeIdAndStatus(userId, TaskStatus.IN_REVIEW));
            response.setOverdueTasks(taskRepository.countOverdueTasksForAssignee(userId, now));

            Map<String, Long> statusMap = new HashMap<>();
            for (TaskStatus s : TaskStatus.values()) {
                statusMap.put(s.name(), taskRepository.countByAssigneeIdAndStatus(userId, s));
            }
            response.setStatusDistribution(statusMap);

            Map<String, Long> priorityMap = new HashMap<>();
            for (TaskPriority p : TaskPriority.values()) {
                priorityMap.put(p.name(), taskRepository.countByAssigneeIdAndPriority(userId, p));
            }
            response.setPriorityDistribution(priorityMap);

            List<TaskResponse> recent = taskRepository.findTop5ByAssigneeIdOrderByCreatedAtDesc(userId).stream()
                    .map(TaskResponse::fromEntity)
                    .collect(Collectors.toList());
            response.setRecentTasks(recent);
        }

        return response;
    }
}
