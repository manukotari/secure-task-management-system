package com.taskmanagement.secure.dto.audit;

import com.taskmanagement.secure.entity.TaskAuditLog;

import java.time.Instant;

public class TaskAuditLogResponse {

    private Long id;
    private Long taskId;
    private String action;
    private String fieldChanged;
    private String oldValue;
    private String newValue;
    private Long actorId;
    private String actorEmail;
    private Instant createdAt;

    public TaskAuditLogResponse() {}

    public static TaskAuditLogResponse fromEntity(TaskAuditLog log) {
        TaskAuditLogResponse response = new TaskAuditLogResponse();
        response.setId(log.getId());
        response.setTaskId(log.getTask() != null ? log.getTask().getId() : null);
        response.setAction(log.getAction());
        response.setFieldChanged(log.getFieldChanged());
        response.setOldValue(log.getOldValue());
        response.setNewValue(log.getNewValue());
        response.setActorId(log.getActor() != null ? log.getActor().getId() : null);
        response.setActorEmail(log.getActorEmail());
        response.setCreatedAt(log.getCreatedAt());
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getFieldChanged() {
        return fieldChanged;
    }

    public void setFieldChanged(String fieldChanged) {
        this.fieldChanged = fieldChanged;
    }

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    public Long getActorId() {
        return actorId;
    }

    public void setActorId(Long actorId) {
        this.actorId = actorId;
    }

    public String getActorEmail() {
        return actorEmail;
    }

    public void setActorEmail(String actorEmail) {
        this.actorEmail = actorEmail;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
