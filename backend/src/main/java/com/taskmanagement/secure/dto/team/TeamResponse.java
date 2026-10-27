package com.taskmanagement.secure.dto.team;

import com.taskmanagement.secure.dto.auth.UserResponse;
import com.taskmanagement.secure.entity.Team;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public class TeamResponse {

    private Long id;
    private String name;
    private String description;
    private UserResponse manager;
    private int memberCount;
    private List<UserResponse> members;
    private Instant createdAt;

    public TeamResponse() {}

    public TeamResponse(Long id, String name, String description, UserResponse manager, int memberCount, List<UserResponse> members, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.manager = manager;
        this.memberCount = memberCount;
        this.members = members;
        this.createdAt = createdAt;
    }

    public static TeamResponse fromEntity(Team team, boolean includeMembers) {
        UserResponse managerResponse = team.getManager() != null ? UserResponse.fromEntity(team.getManager()) : null;
        List<UserResponse> memberList = null;
        if (includeMembers && team.getMembers() != null) {
            memberList = team.getMembers().stream()
                    .map(UserResponse::fromEntity)
                    .collect(Collectors.toList());
        }

        int count = team.getMembers() != null ? team.getMembers().size() : 0;

        return new TeamResponse(
                team.getId(),
                team.getName(),
                team.getDescription(),
                managerResponse,
                count,
                memberList,
                team.getCreatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public UserResponse getManager() {
        return manager;
    }

    public void setManager(UserResponse manager) {
        this.manager = manager;
    }

    public int getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(int memberCount) {
        this.memberCount = memberCount;
    }

    public List<UserResponse> getMembers() {
        return members;
    }

    public void setMembers(List<UserResponse> members) {
        this.members = members;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
