package com.taskmanagement.secure.service;

import com.taskmanagement.secure.dto.common.PagedResponse;
import com.taskmanagement.secure.dto.team.AddTeamMemberRequest;
import com.taskmanagement.secure.dto.team.CreateTeamRequest;
import com.taskmanagement.secure.dto.team.TeamResponse;
import com.taskmanagement.secure.entity.RoleName;
import com.taskmanagement.secure.entity.Team;
import com.taskmanagement.secure.entity.User;
import com.taskmanagement.secure.exception.ConflictException;
import com.taskmanagement.secure.exception.ForbiddenException;
import com.taskmanagement.secure.exception.ResourceNotFoundException;
import com.taskmanagement.secure.repository.TeamRepository;
import com.taskmanagement.secure.repository.UserRepository;
import com.taskmanagement.secure.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TeamService {

    private final TeamRepository teamRepository;
    private final UserRepository userRepository;

    public TeamService(TeamRepository teamRepository, UserRepository userRepository) {
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public TeamResponse createTeam(CreateTeamRequest request) {
        if (teamRepository.existsByNameIgnoreCase(request.getName())) {
            throw new ConflictException("A team with name '" + request.getName() + "' already exists.");
        }

        User manager = userRepository.findById(request.getManagerId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getManagerId()));

        Team team = new Team(request.getName().trim(), request.getDescription(), manager);
        Team saved = teamRepository.save(team);
        return TeamResponse.fromEntity(saved, true);
    }

    @Transactional(readOnly = true)
    public PagedResponse<TeamResponse> listTeams(UserPrincipal principal, Pageable pageable) {
        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(RoleName.ROLE_ADMIN.name()));

        if (isAdmin) {
            Page<Team> page = teamRepository.findAll(pageable);
            return PagedResponse.from(page.map(t -> TeamResponse.fromEntity(t, false)));
        }

        List<Team> teams = teamRepository.findAccessibleTeamsForUser(principal.getId());
        List<TeamResponse> responses = teams.stream()
                .map(t -> TeamResponse.fromEntity(t, false))
                .collect(Collectors.toList());

        // Simple manual paging for non-admin user list
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), responses.size());
        List<TeamResponse> pageContent = (start < responses.size()) ? responses.subList(start, end) : List.of();
        Page<TeamResponse> page = new PageImpl<>(pageContent, pageable, responses.size());

        return PagedResponse.from(page);
    }

    @Transactional(readOnly = true)
    public TeamResponse getTeamById(Long id, UserPrincipal principal) {
        Team team = teamRepository.findByIdWithMembers(id)
                .orElseThrow(() -> new ResourceNotFoundException("Team", "id", id));

        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(RoleName.ROLE_ADMIN.name()));

        boolean isManager = team.getManager() != null && team.getManager().getId().equals(principal.getId());
        boolean isMember = team.hasMember(principal.getId());

        if (!isAdmin && !isManager && !isMember) {
            throw new ForbiddenException("You are not authorized to view this team's details");
        }

        return TeamResponse.fromEntity(team, true);
    }

    @Transactional
    public TeamResponse addTeamMember(Long teamId, AddTeamMemberRequest request, UserPrincipal principal) {
        Team team = teamRepository.findByIdWithMembers(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team", "id", teamId));

        checkTeamManagementPermission(team, principal);

        User member = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getUserId()));

        team.addMember(member);
        Team updated = teamRepository.save(team);
        return TeamResponse.fromEntity(updated, true);
    }

    @Transactional
    public TeamResponse removeTeamMember(Long teamId, Long userId, UserPrincipal principal) {
        Team team = teamRepository.findByIdWithMembers(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team", "id", teamId));

        checkTeamManagementPermission(team, principal);

        User member = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        team.removeMember(member);
        Team updated = teamRepository.save(team);
        return TeamResponse.fromEntity(updated, true);
    }

    private void checkTeamManagementPermission(Team team, UserPrincipal principal) {
        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(RoleName.ROLE_ADMIN.name()));

        boolean isManager = team.getManager() != null && team.getManager().getId().equals(principal.getId());

        if (!isAdmin && !isManager) {
            throw new ForbiddenException("Only the designated team manager or an administrator can modify team members.");
        }
    }
}
