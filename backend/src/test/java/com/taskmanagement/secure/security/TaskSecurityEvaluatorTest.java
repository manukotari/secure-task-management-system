package com.taskmanagement.secure.security;

import com.taskmanagement.secure.entity.*;
import com.taskmanagement.secure.repository.TaskRepository;
import com.taskmanagement.secure.repository.TeamRepository;
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskSecurityEvaluatorTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TeamRepository teamRepository;

    @InjectMocks
    private TaskSecurityEvaluator evaluator;

    private User adminUser;
    private User managerUser;
    private User regularUser;
    private User otherUser;
    private Team team;
    private Task teamTask;

    private UserPrincipal adminPrincipal;
    private UserPrincipal managerPrincipal;
    private UserPrincipal userPrincipal;
    private UserPrincipal otherUserPrincipal;

    @BeforeEach
    void setUp() {
        adminUser = new User("admin@test.com", "hash", "Admin");
        adminUser.setId(1L);

        managerUser = new User("manager@test.com", "hash", "Manager");
        managerUser.setId(2L);

        regularUser = new User("user@test.com", "hash", "Alice");
        regularUser.setId(3L);

        otherUser = new User("other@test.com", "hash", "Bob");
        otherUser.setId(4L);

        team = new Team("Engineering", "Dev team", managerUser);
        team.setId(10L);
        team.addMember(regularUser);

        teamTask = new Task("Task 1", "Desc 1", TaskPriority.MEDIUM, TaskStatus.TODO,
                managerUser, regularUser, team, Instant.now().plusSeconds(86400));
        teamTask.setId(100L);

        adminPrincipal = new UserPrincipal(1L, "admin@test.com", "Admin", "pwd", true,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        managerPrincipal = new UserPrincipal(2L, "manager@test.com", "Manager", "pwd", true,
                List.of(new SimpleGrantedAuthority("ROLE_MANAGER")));

        userPrincipal = new UserPrincipal(3L, "user@test.com", "Alice", "pwd", true,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        otherUserPrincipal = new UserPrincipal(4L, "other@test.com", "Bob", "pwd", true,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    @DisplayName("Admin can view, modify, update status, and delete any task")
    void adminCanPerformAllOperations() {
        assertTrue(evaluator.canViewTask(100L, adminPrincipal));
        assertTrue(evaluator.canModifyTask(100L, adminPrincipal));
        assertTrue(evaluator.canUpdateTaskStatus(100L, adminPrincipal));
        assertTrue(evaluator.canDeleteTask(100L, adminPrincipal));
    }

    @Test
    @DisplayName("Assigned user can view task and update status, but cannot modify details or delete")
    void userCanViewAndAdvanceStatusOfAssignedTask() {
        when(taskRepository.findById(100L)).thenReturn(Optional.of(teamTask));

        assertTrue(evaluator.canViewTask(100L, userPrincipal));
        assertTrue(evaluator.canUpdateTaskStatus(100L, userPrincipal));
        assertFalse(evaluator.canModifyTask(100L, userPrincipal));
        assertFalse(evaluator.canDeleteTask(100L, userPrincipal));
    }

    @Test
    @DisplayName("Negative Security / IDOR: Unrelated user cannot view, update status, modify, or delete task")
    void unrelatedUserCannotAccessOrManipulateTask() {
        when(taskRepository.findById(100L)).thenReturn(Optional.of(teamTask));

        assertFalse(evaluator.canViewTask(100L, otherUserPrincipal));
        assertFalse(evaluator.canUpdateTaskStatus(100L, otherUserPrincipal));
        assertFalse(evaluator.canModifyTask(100L, otherUserPrincipal));
        assertFalse(evaluator.canDeleteTask(100L, otherUserPrincipal));
    }

    @Test
    @DisplayName("Team Manager can view, modify, update status, and delete task in their team")
    void managerCanManageTheirTeamTask() {
        when(taskRepository.findById(100L)).thenReturn(Optional.of(teamTask));

        assertTrue(evaluator.canViewTask(100L, managerPrincipal));
        assertTrue(evaluator.canModifyTask(100L, managerPrincipal));
        assertTrue(evaluator.canUpdateTaskStatus(100L, managerPrincipal));
        assertTrue(evaluator.canDeleteTask(100L, managerPrincipal));
    }
}
