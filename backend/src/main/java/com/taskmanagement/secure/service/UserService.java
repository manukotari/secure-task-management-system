package com.taskmanagement.secure.service;

import com.taskmanagement.secure.dto.auth.UserResponse;
import com.taskmanagement.secure.dto.common.PagedResponse;
import com.taskmanagement.secure.dto.user.UpdateUserRolesRequest;
import com.taskmanagement.secure.dto.user.UpdateUserStatusRequest;
import com.taskmanagement.secure.entity.Role;
import com.taskmanagement.secure.entity.RoleName;
import com.taskmanagement.secure.entity.User;
import com.taskmanagement.secure.exception.BadRequestException;
import com.taskmanagement.secure.exception.ForbiddenException;
import com.taskmanagement.secure.exception.ResourceNotFoundException;
import com.taskmanagement.secure.repository.RoleRepository;
import com.taskmanagement.secure.repository.UserRepository;
import com.taskmanagement.secure.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public UserService(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Transactional(readOnly = true)
    public PagedResponse<UserResponse> listUsers(String search, Pageable pageable) {
        Page<User> page;
        if (StringUtils.hasText(search)) {
            page = userRepository.searchUsers(search.trim(), pageable);
        } else {
            page = userRepository.findAll(pageable);
        }
        return PagedResponse.from(page.map(UserResponse::fromEntity));
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id, UserPrincipal principal) {
        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(RoleName.ROLE_ADMIN.name()));

        if (!isAdmin && !principal.getId().equals(id)) {
            throw new ForbiddenException("You are not authorized to view this user's details");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        return UserResponse.fromEntity(user);
    }

    @Transactional
    public UserResponse updateUserStatus(Long id, UpdateUserStatusRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        user.setActive(request.getActive());
        User updated = userRepository.save(user);
        return UserResponse.fromEntity(updated);
    }

    @Transactional
    public UserResponse updateUserRoles(Long id, UpdateUserRolesRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        Set<Role> roles = new HashSet<>();
        for (String roleStr : request.getRoles()) {
            try {
                RoleName roleName = RoleName.valueOf(roleStr);
                Role role = roleRepository.findByName(roleName)
                        .orElseThrow(() -> new BadRequestException("Role not found: " + roleStr));
                roles.add(role);
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid role name: " + roleStr);
            }
        }

        user.setRoles(roles);
        User updated = userRepository.save(user);
        return UserResponse.fromEntity(updated);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getTeamEligibleUsers(UserPrincipal principal) {
        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(RoleName.ROLE_ADMIN.name()));

        List<User> users;
        if (isAdmin) {
            users = userRepository.findAllActiveUsers();
        } else {
            // Manager gets active members of their managed teams
            users = userRepository.findActiveMembersByManagerId(principal.getId());
        }

        return users.stream().map(UserResponse::fromEntity).collect(Collectors.toList());
    }
}
