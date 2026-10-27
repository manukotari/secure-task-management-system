package com.taskmanagement.secure.controller;

import com.taskmanagement.secure.dto.comment.CommentResponse;
import com.taskmanagement.secure.dto.comment.CreateCommentRequest;
import com.taskmanagement.secure.security.UserPrincipal;
import com.taskmanagement.secure.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tasks/{taskId}/comments")
@Tag(name = "Task Comments", description = "Endpoints for task comments and discussion")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping
    @Operation(summary = "Add a comment to an authorized task")
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable Long taskId,
            @Valid @RequestBody CreateCommentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(commentService.addComment(taskId, request, principal));
    }

    @GetMapping
    @Operation(summary = "List all comments on an authorized task")
    public ResponseEntity<List<CommentResponse>> getComments(
            @PathVariable Long taskId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(commentService.getComments(taskId, principal));
    }
}
