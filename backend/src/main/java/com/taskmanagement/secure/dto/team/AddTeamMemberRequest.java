package com.taskmanagement.secure.dto.team;

import jakarta.validation.constraints.NotNull;

public class AddTeamMemberRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    public AddTeamMemberRequest() {}

    public AddTeamMemberRequest(Long userId) {
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
