package com.taskmanagement.secure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanagement.secure.dto.task.CreateTaskRequest;
import com.taskmanagement.secure.dto.task.TaskResponse;
import com.taskmanagement.secure.entity.TaskPriority;
import com.taskmanagement.secure.entity.TaskStatus;
import com.taskmanagement.secure.exception.ForbiddenException;
import com.taskmanagement.secure.service.AuditService;
import com.taskmanagement.secure.service.TaskService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TaskControllerSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TaskService taskService;

    @MockBean
    private AuditService auditService;

    @Test
    @DisplayName("Unauthenticated request to protected endpoint should return 401 Unauthorized")
    void unauthenticatedRequestReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/tasks"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = {"USER"})
    @DisplayName("Regular USER role attempting to create a task should return 403 Forbidden")
    void userRoleCannotCreateTaskReturns403() throws Exception {
        CreateTaskRequest request = new CreateTaskRequest("New Task", "Desc", TaskPriority.LOW, TaskStatus.TODO, null, null, null);

        mockMvc.perform(post("/api/v1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = {"USER"})
    @DisplayName("Regular USER role attempting to access admin audit logs should return 403 Forbidden")
    void userRoleCannotAccessAdminAuditLogsReturns403() throws Exception {
        mockMvc.perform(get("/api/v1/audit-logs"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = {"USER"})
    @DisplayName("Negative Security / IDOR: Attempting to access another user's task returns 403 Forbidden")
    void idorAttemptReturns403Forbidden() throws Exception {
        when(taskService.getTaskById(eq(999L), any()))
                .thenThrow(new ForbiddenException("You are not authorized to access this task."));

        mockMvc.perform(get("/api/v1/tasks/999"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("You are not authorized to access this task."));
    }

    @Test
    @WithMockUser(username = "manager@test.com", roles = {"MANAGER"})
    @DisplayName("MANAGER role can access task creation endpoint")
    void managerCanCreateTask() throws Exception {
        CreateTaskRequest request = new CreateTaskRequest("Valid Task", "Description", TaskPriority.HIGH, TaskStatus.TODO, null, null, null);
        TaskResponse response = new TaskResponse();
        response.setId(101L);
        response.setTitle("Valid Task");

        when(taskService.createTask(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(101))
                .andExpect(jsonPath("$.title").value("Valid Task"));
    }
}
