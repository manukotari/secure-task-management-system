package com.taskmanagement.secure.dto.auth;

import com.taskmanagement.secure.entity.User;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

public class UserResponse {

    private Long id;
    private String email;
    private String fullName;
    private boolean active;
    private Set<String> roles;
    private Instant createdAt;

    public UserResponse() {}

    public UserResponse(Long id, String email, String fullName, boolean active, Set<String> roles, Instant createdAt) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.active = active;
        this.roles = roles;
        this.createdAt = createdAt;
    }

    public static UserResponse fromEntity(User user) {
        Set<String> roleNames = user.getRoles().stream()
                .map(r -> r.getName().name())
                .collect(Collectors.toSet());

        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.isActive(),
                roleNames,
                user.getCreatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
