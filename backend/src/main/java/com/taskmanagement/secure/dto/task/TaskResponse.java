package com.taskmanagement.secure.dto.task;

import com.taskmanagement.secure.dto.auth.UserResponse;
import com.taskmanagement.secure.entity.Task;
import com.taskmanagement.secure.entity.TaskPriority;
import com.taskmanagement.secure.entity.TaskStatus;

import java.time.Instant;

public class TaskResponse {

    private Long id;
    private String title;
    private String description;
    private TaskPriority priority;
    private TaskStatus status;
    private UserResponse creator;
    private UserResponse assignee;
    private Long teamId;
    private String teamName;
    private Instant dueDate;
    private Instant completedAt;
    private boolean overdue;
    private Instant createdAt;
    private Instant updatedAt;
    private int commentCount;

    public TaskResponse() {}

    public static TaskResponse fromEntity(Task task) {
        TaskResponse response = new TaskResponse();
        response.setId(task.getId());
        response.setTitle(task.getTitle());
        response.setDescription(task.getDescription());
        response.setPriority(task.getPriority());
        response.setStatus(task.getStatus());
        if (task.getCreator() != null) {
            response.setCreator(UserResponse.fromEntity(task.getCreator()));
        }
        if (task.getAssignee() != null) {
            response.setAssignee(UserResponse.fromEntity(task.getAssignee()));
        }
        if (task.getTeam() != null) {
            response.setTeamId(task.getTeam().getId());
            response.setTeamName(task.getTeam().getName());
        }
        response.setDueDate(task.getDueDate());
        response.setCompletedAt(task.getCompletedAt());
        response.setCreatedAt(task.getCreatedAt());
        response.setUpdatedAt(task.getUpdatedAt());

        boolean isOverdue = task.getDueDate() != null &&
                Instant.now().isAfter(task.getDueDate()) &&
                task.getStatus() != TaskStatus.COMPLETED;
        response.setOverdue(isOverdue);

        if (task.getComments() != null) {
            response.setCommentCount(task.getComments().size());
        }

        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public UserResponse getCreator() {
        return creator;
    }

    public void setCreator(UserResponse creator) {
        this.creator = creator;
    }

    public UserResponse getAssignee() {
        return assignee;
    }

    public void setAssignee(UserResponse assignee) {
        this.assignee = assignee;
    }

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public Instant getDueDate() {
        return dueDate;
    }

    public void setDueDate(Instant dueDate) {
        this.dueDate = dueDate;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public boolean isOverdue() {
        return overdue;
    }

    public void setOverdue(boolean overdue) {
        this.overdue = overdue;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public int getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(int commentCount) {
        this.commentCount = commentCount;
    }
}
