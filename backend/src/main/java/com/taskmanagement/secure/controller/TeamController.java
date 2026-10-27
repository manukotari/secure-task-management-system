package com.taskmanagement.secure.controller;

import com.taskmanagement.secure.dto.common.PagedResponse;
import com.taskmanagement.secure.dto.team.AddTeamMemberRequest;
import com.taskmanagement.secure.dto.team.CreateTeamRequest;
import com.taskmanagement.secure.dto.team.TeamResponse;
import com.taskmanagement.secure.security.UserPrincipal;
import com.taskmanagement.secure.service.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/teams")
@Tag(name = "Team Management", description = "Endpoints for managing teams and memberships")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new team (Admin only)")
    public ResponseEntity<TeamResponse> createTeam(@Valid @RequestBody CreateTeamRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(teamService.createTeam(request));
    }

    @GetMapping
    @Operation(summary = "List accessible teams based on user role")
    public ResponseEntity<PagedResponse<TeamResponse>> listTeams(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(teamService.listTeams(principal, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get team details and members by team ID")
    public ResponseEntity<TeamResponse> getTeamById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(teamService.getTeamById(id, principal));
    }

    @PostMapping("/{id}/members")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Add a member to a team (Admin or team manager)")
    public ResponseEntity<TeamResponse> addTeamMember(
            @PathVariable Long id,
            @Valid @RequestBody AddTeamMemberRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(teamService.addTeamMember(id, request, principal));
    }

    @DeleteMapping("/{id}/members/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Remove a member from a team (Admin or team manager)")
    public ResponseEntity<TeamResponse> removeTeamMember(
            @PathVariable Long id,
            @PathVariable Long userId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(teamService.removeTeamMember(id, userId, principal));
    }
}
