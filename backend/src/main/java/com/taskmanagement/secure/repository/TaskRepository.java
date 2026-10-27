package com.taskmanagement.secure.repository;

import com.taskmanagement.secure.entity.Task;
import com.taskmanagement.secure.entity.TaskPriority;
import com.taskmanagement.secure.entity.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    @Query("SELECT t FROM Task t " +
           "LEFT JOIN FETCH t.creator " +
           "LEFT JOIN FETCH t.assignee " +
           "LEFT JOIN FETCH t.team " +
           "WHERE t.id = :id")
    Optional<Task> findByIdWithDetails(@Param("id") Long id);

    // User scope: tasks assigned to user
    Page<Task> findByAssigneeId(Long assigneeId, Pageable pageable);

    // Manager scope: tasks belonging to team IDs
    Page<Task> findByTeamIdIn(Collection<Long> teamIds, Pageable pageable);

    // Aggregations for Admin Dashboard
    long countByStatus(TaskStatus status);
    long countByPriority(TaskPriority priority);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.dueDate < :now AND t.status != com.taskmanagement.secure.entity.TaskStatus.COMPLETED")
    long countOverdueTasks(@Param("now") Instant now);

    // Aggregations for Manager Dashboard
    long countByTeamIdIn(Collection<Long> teamIds);
    long countByTeamIdInAndStatus(Collection<Long> teamIds, TaskStatus status);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.team.id IN :teamIds AND t.dueDate < :now AND t.status != com.taskmanagement.secure.entity.TaskStatus.COMPLETED")
    long countOverdueTasksForTeams(@Param("teamIds") Collection<Long> teamIds, @Param("now") Instant now);

    // Aggregations for User Dashboard
    long countByAssigneeId(Long assigneeId);
    long countByAssigneeIdAndStatus(Long assigneeId, TaskStatus status);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.assignee.id = :assigneeId AND t.dueDate < :now AND t.status != com.taskmanagement.secure.entity.TaskStatus.COMPLETED")
    long countOverdueTasksForAssignee(@Param("assigneeId") Long assigneeId, @Param("now") Instant now);

    // Priority breakdown queries
    long countByAssigneeIdAndPriority(Long assigneeId, TaskPriority priority);
    long countByTeamIdInAndPriority(Collection<Long> teamIds, TaskPriority priority);

    // Recent tasks
    List<Task> findTop5ByOrderByCreatedAtDesc();
    List<Task> findTop5ByAssigneeIdOrderByCreatedAtDesc(Long assigneeId);
    List<Task> findTop5ByTeamIdInOrderByCreatedAtDesc(Collection<Long> teamIds);
}
