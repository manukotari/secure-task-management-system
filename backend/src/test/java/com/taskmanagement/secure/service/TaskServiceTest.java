package com.taskmanagement.secure.service;

import com.taskmanagement.secure.dto.task.CreateTaskRequest;
import com.taskmanagement.secure.dto.task.TaskResponse;
import com.taskmanagement.secure.dto.task.UpdateTaskStatusRequest;
import com.taskmanagement.secure.entity.*;
import com.taskmanagement.secure.exception.BadRequestException;
import com.taskmanagement.secure.exception.ForbiddenException;
import com.taskmanagement.secure.repository.TaskRepository;
import com.taskmanagement.secure.repository.TeamRepository;
import com.taskmanagement.secure.repository.UserRepository;
import com.taskmanagement.secure.security.TaskSecurityEvaluator;
import com.taskmanagement.secure.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private TaskSecurityEvaluator taskSecurityEvaluator;

    @InjectMocks
    private TaskService taskService;

    private User managerUser;
    private User regularUser;
    private User externalUser;
    private Team managedTeam;
    private UserPrincipal managerPrincipal;
    private UserPrincipal userPrincipal;

    @BeforeEach
    void setUp() {
        managerUser = new User("manager@test.com", "hash", "Manager");
        managerUser.setId(2L);

        regularUser = new User("user@test.com", "hash", "Alice");
        regularUser.setId(3L);

        externalUser = new User("external@test.com", "hash", "External");
        externalUser.setId(4L);

        managedTeam = new Team("Core Team", "Core Devs", managerUser);
        managedTeam.setId(10L);
        managedTeam.addMember(regularUser);

        managerPrincipal = new UserPrincipal(2L, "manager@test.com", "Manager", "pwd", true,
                List.of(new SimpleGrantedAuthority("ROLE_MANAGER")));

        userPrincipal = new UserPrincipal(3L, "user@test.com", "Alice", "pwd", true,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    @DisplayName("Manager cannot assign task to user outside of their managed team")
    void managerCannotAssignToUserOutsideTeam() {
        CreateTaskRequest request = new CreateTaskRequest(
                "Feature X", "Build feature X", TaskPriority.HIGH, TaskStatus.TODO,
                4L, 10L, Instant.now().plusSeconds(86400)
        );

        when(userRepository.findById(2L)).thenReturn(Optional.of(managerUser));
        when(teamRepository.findById(10L)).thenReturn(Optional.of(managedTeam));
        when(userRepository.findById(4L)).thenReturn(Optional.of(externalUser));

        assertThrows(BadRequestException.class, () -> taskService.createTask(request, managerPrincipal));
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    @DisplayName("Updating task status to COMPLETED sets completedAt timestamp")
    void updatingStatusToCompletedSetsCompletionTimestamp() {
        Task task = new Task("Task 1", "Desc", TaskPriority.MEDIUM, TaskStatus.IN_PROGRESS,
                managerUser, regularUser, managedTeam, Instant.now().plusSeconds(86400));
        task.setId(50L);

        when(taskSecurityEvaluator.canUpdateTaskStatus(50L, userPrincipal)).thenReturn(true);
        when(taskRepository.findByIdWithDetails(50L)).thenReturn(Optional.of(task));
        when(userRepository.findById(3L)).thenReturn(Optional.of(regularUser));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateTaskStatusRequest request = new UpdateTaskStatusRequest(TaskStatus.COMPLETED);
        TaskResponse response = taskService.updateTaskStatus(50L, request, userPrincipal);

        assertNotNull(response);
        assertEquals(TaskStatus.COMPLETED, response.getStatus());
        assertNotNull(task.getCompletedAt());
        verify(auditService).logTaskAction(eq(task), eq("STATUS_CHANGED"), eq("status"), eq("IN_PROGRESS"), eq("COMPLETED"), eq(regularUser));
    }

    @Test
    @DisplayName("Regular user cannot create tasks (Forbidden)")
    void regularUserCannotCreateTasks() {
        CreateTaskRequest request = new CreateTaskRequest("New Task", "Desc", TaskPriority.LOW, TaskStatus.TODO, 3L, 10L, null);

        assertThrows(ForbiddenException.class, () -> taskService.createTask(request, userPrincipal));
        verify(taskRepository, never()).save(any(Task.class));
    }
}
