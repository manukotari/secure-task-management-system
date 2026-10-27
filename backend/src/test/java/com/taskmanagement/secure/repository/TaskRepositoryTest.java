package com.taskmanagement.secure.repository;

import com.taskmanagement.secure.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class TaskRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TaskRepository taskRepository;

    private User manager;
    private User developer;
    private Team team;
    private Task activeTask;
    private Task overdueTask;
    private Task completedTask;

    @BeforeEach
    void setUp() {
        Role userRole = new Role(RoleName.ROLE_USER);
        entityManager.persist(userRole);

        Role managerRole = new Role(RoleName.ROLE_MANAGER);
        entityManager.persist(managerRole);

        manager = new User("manager.repo@test.com", "hash", "Manager Repo");
        manager.setRoles(Set.of(managerRole));
        entityManager.persist(manager);

        developer = new User("dev.repo@test.com", "hash", "Dev Repo");
        developer.setRoles(Set.of(userRole));
        entityManager.persist(developer);

        team = new Team("Alpha Team", "Alpha", manager);
        team.addMember(developer);
        entityManager.persist(team);

        // Active task due in 3 days
        activeTask = new Task("Active Task", "Description", TaskPriority.HIGH, TaskStatus.IN_PROGRESS,
                manager, developer, team, Instant.now().plus(3, ChronoUnit.DAYS));
        entityManager.persist(activeTask);

        // Overdue task due 2 days ago
        overdueTask = new Task("Overdue Task", "Urgent bug", TaskPriority.CRITICAL, TaskStatus.TODO,
                manager, developer, team, Instant.now().minus(2, ChronoUnit.DAYS));
        entityManager.persist(overdueTask);

        // Completed task
        completedTask = new Task("Completed Task", "Done task", TaskPriority.LOW, TaskStatus.COMPLETED,
                manager, developer, team, Instant.now().minus(1, ChronoUnit.DAYS));
        completedTask.setCompletedAt(Instant.now().minus(1, ChronoUnit.DAYS));
        entityManager.persist(completedTask);

        entityManager.flush();
    }

    @Test
    @DisplayName("Should find task by ID with joined details")
    void shouldFindByIdWithDetails() {
        Optional<Task> found = taskRepository.findByIdWithDetails(activeTask.getId());
        assertTrue(found.isPresent());
        assertEquals("Active Task", found.get().getTitle());
        assertEquals(developer.getId(), found.get().getAssignee().getId());
        assertEquals(team.getId(), found.get().getTeam().getId());
    }

    @Test
    @DisplayName("Should count tasks by status and priority accurately")
    void shouldCountByStatusAndPriority() {
        assertEquals(1, taskRepository.countByStatus(TaskStatus.IN_PROGRESS));
        assertEquals(1, taskRepository.countByStatus(TaskStatus.TODO));
        assertEquals(1, taskRepository.countByStatus(TaskStatus.COMPLETED));

        assertEquals(1, taskRepository.countByPriority(TaskPriority.HIGH));
        assertEquals(1, taskRepository.countByPriority(TaskPriority.CRITICAL));
        assertEquals(1, taskRepository.countByPriority(TaskPriority.LOW));
    }

    @Test
    @DisplayName("Should calculate overdue tasks accurately excluding completed tasks")
    void shouldCountOverdueTasks() {
        Instant now = Instant.now();
        long overdueGlobal = taskRepository.countOverdueTasks(now);
        assertEquals(1, overdueGlobal);

        long overdueTeam = taskRepository.countOverdueTasksForTeams(List.of(team.getId()), now);
        assertEquals(1, overdueTeam);

        long overdueAssignee = taskRepository.countOverdueTasksForAssignee(developer.getId(), now);
        assertEquals(1, overdueAssignee);
    }

    @Test
    @DisplayName("Should query tasks by team IDs and assignee with pagination")
    void shouldQueryByTeamAndAssignee() {
        Page<Task> teamTasks = taskRepository.findByTeamIdIn(List.of(team.getId()), PageRequest.of(0, 10));
        assertEquals(3, teamTasks.getTotalElements());

        Page<Task> userTasks = taskRepository.findByAssigneeId(developer.getId(), PageRequest.of(0, 10));
        assertEquals(3, userTasks.getTotalElements());
    }

    @Test
    @DisplayName("Should filter tasks using JPA dynamic specifications")
    void shouldFilterWithSpecifications() {
        // Search by keyword "urgent"
        Specification<Task> keywordSpec = (root, query, cb) ->
                cb.like(cb.lower(root.get("description")), "%urgent%");

        List<Task> results = taskRepository.findAll(keywordSpec);
        assertEquals(1, results.size());
        assertEquals("Overdue Task", results.get(0).getTitle());
    }
}
