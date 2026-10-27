package com.taskmanagement.secure.service;

import com.taskmanagement.secure.dto.comment.CommentResponse;
import com.taskmanagement.secure.dto.comment.CreateCommentRequest;
import com.taskmanagement.secure.entity.Comment;
import com.taskmanagement.secure.entity.Task;
import com.taskmanagement.secure.entity.User;
import com.taskmanagement.secure.exception.ForbiddenException;
import com.taskmanagement.secure.exception.ResourceNotFoundException;
import com.taskmanagement.secure.repository.CommentRepository;
import com.taskmanagement.secure.repository.TaskRepository;
import com.taskmanagement.secure.repository.UserRepository;
import com.taskmanagement.secure.security.TaskSecurityEvaluator;
import com.taskmanagement.secure.security.UserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TaskSecurityEvaluator taskSecurityEvaluator;

    public CommentService(
            CommentRepository commentRepository,
            TaskRepository taskRepository,
            UserRepository userRepository,
            TaskSecurityEvaluator taskSecurityEvaluator) {
        this.commentRepository = commentRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.taskSecurityEvaluator = taskSecurityEvaluator;
    }

    @Transactional
    public CommentResponse addComment(Long taskId, CreateCommentRequest request, UserPrincipal principal) {
        if (!taskSecurityEvaluator.canViewTask(taskId, principal)) {
            throw new ForbiddenException("You are not authorized to comment on this task.");
        }

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));

        User author = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", principal.getId()));

        Comment comment = new Comment(task, author, request.getContent().trim());
        Comment saved = commentRepository.save(comment);

        return CommentResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(Long taskId, UserPrincipal principal) {
        if (!taskSecurityEvaluator.canViewTask(taskId, principal)) {
            throw new ForbiddenException("You are not authorized to view comments on this task.");
        }

        return commentRepository.findByTaskIdWithAuthor(taskId).stream()
                .map(CommentResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
