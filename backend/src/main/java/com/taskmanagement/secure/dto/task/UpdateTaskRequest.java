package com.taskmanagement.secure.dto.task;

import com.taskmanagement.secure.entity.TaskPriority;
import com.taskmanagement.secure.entity.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public class UpdateTaskRequest {

    @NotBlank(message = "Task title is required")
    @Size(min = 2, max = 200, message = "Task title must be between 2 and 200 characters")
    private String title;

    private String description;

    private TaskPriority priority;

    private TaskStatus status;

    private Long assigneeId;

    private Long teamId;

    private Instant dueDate;

    public UpdateTaskRequest() {}

    public UpdateTaskRequest(String title, String description, TaskPriority priority, TaskStatus status, Long assigneeId, Long teamId, Instant dueDate) {
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.status = status;
        this.assigneeId = assigneeId;
        this.teamId = teamId;
        this.dueDate = dueDate;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TaskPriority getPriority() {
        return priority;
    }

    public void setPriority(TaskPriority priority) {
        this.priority = priority;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public Long getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(Long assigneeId) {
        this.assigneeId = assigneeId;
    }

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public Instant getDueDate() {
        return dueDate;
    }

    public void setDueDate(Instant dueDate) {
        this.dueDate = dueDate;
    }
}
