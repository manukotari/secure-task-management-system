package com.taskmanagement.secure.repository;

import com.taskmanagement.secure.entity.TaskAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskAuditLogRepository extends JpaRepository<TaskAuditLog, Long> {

    @Query("SELECT l FROM TaskAuditLog l LEFT JOIN FETCH l.actor WHERE l.task.id = :taskId ORDER BY l.createdAt DESC")
    List<TaskAuditLog> findByTaskIdWithActor(@Param("taskId") Long taskId);

    @Query(value = "SELECT l FROM TaskAuditLog l LEFT JOIN FETCH l.task LEFT JOIN FETCH l.actor ORDER BY l.createdAt DESC",
           countQuery = "SELECT COUNT(l) FROM TaskAuditLog l")
    Page<TaskAuditLog> findAllWithDetails(Pageable pageable);
}
