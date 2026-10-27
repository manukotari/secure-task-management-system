package com.taskmanagement.secure.config;

import com.taskmanagement.secure.entity.*;
import com.taskmanagement.secure.repository.RoleRepository;
import com.taskmanagement.secure.repository.TaskRepository;
import com.taskmanagement.secure.repository.TeamRepository;
import com.taskmanagement.secure.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final TaskRepository taskRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(
            RoleRepository roleRepository,
            UserRepository userRepository,
            TeamRepository teamRepository,
            TaskRepository taskRepository,
            PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.taskRepository = taskRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // 1. Ensure Roles exist
        Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.ROLE_ADMIN)));
        Role managerRole = roleRepository.findByName(RoleName.ROLE_MANAGER)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.ROLE_MANAGER)));
        Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.ROLE_USER)));

        // 2. Ensure Admin User
        User admin = userRepository.findByEmailIgnoreCase("admin@tasksystem.com")
                .orElseGet(() -> {
                    User u = new User("admin@tasksystem.com", passwordEncoder.encode("Admin@123456"), "System Administrator");
                    u.setRoles(Set.of(adminRole, managerRole, userRole));
                    return userRepository.save(u);
                });

        // 3. Ensure Manager User
        User manager = userRepository.findByEmailIgnoreCase("manager@tasksystem.com")
                .orElseGet(() -> {
                    User u = new User("manager@tasksystem.com", passwordEncoder.encode("Manager@123456"), "Engineering Manager");
                    u.setRoles(Set.of(managerRole, userRole));
                    return userRepository.save(u);
                });

        // 4. Ensure Standard User
        User user = userRepository.findByEmailIgnoreCase("user@tasksystem.com")
                .orElseGet(() -> {
                    User u = new User("user@tasksystem.com", passwordEncoder.encode("User@123456"), "Alice Developer");
                    u.setRoles(Set.of(userRole));
                    return userRepository.save(u);
                });

        // 5. Ensure Default Team
        Team team = teamRepository.findByNameIgnoreCase("Core Engineering Team")
                .orElseGet(() -> {
                    Team t = new Team("Core Engineering Team", "Primary product engineering team", manager);
                    t.addMember(user);
                    return teamRepository.save(t);
                });

        // 6. Ensure initial seed tasks
        if (taskRepository.count() == 0) {
            Task task1 = new Task(
                    "Implement Role-Based Access Control",
                    "Configure Spring Security authorization matrix and fine-grained method security for all resource endpoints.",
                    TaskPriority.HIGH,
                    TaskStatus.IN_PROGRESS,
                    manager,
                    user,
                    team,
                    Instant.now().plus(5, ChronoUnit.DAYS)
            );
            taskRepository.save(task1);

            Task task2 = new Task(
                    "Database Schema Migration & Indexing",
                    "Design and execute Flyway migrations with optimized B-tree indexes for tasks, users, and comments.",
                    TaskPriority.CRITICAL,
                    TaskStatus.COMPLETED,
                    admin,
                    user,
                    team,
                    Instant.now().minus(1, ChronoUnit.DAYS)
            );
            task2.setCompletedAt(Instant.now().minus(1, ChronoUnit.DAYS));
            taskRepository.save(task2);

            Task task3 = new Task(
                    "Audit Log Retention & Security Hardening",
                    "Implement immutable audit trail for task status, assignee updates, and administrative role modifications.",
                    TaskPriority.MEDIUM,
                    TaskStatus.TODO,
                    manager,
                    user,
                    team,
                    Instant.now().plus(10, ChronoUnit.DAYS)
            );
            taskRepository.save(task3);

            Task task4 = new Task(
                    "Review Team Sprint Deliverables",
                    "Conduct architecture and code quality audit of newly merged features before the release milestone.",
                    TaskPriority.HIGH,
                    TaskStatus.IN_REVIEW,
                    admin,
                    manager,
                    team,
                    Instant.now().plus(2, ChronoUnit.DAYS)
            );
            taskRepository.save(task4);

            log.info("Initialized default seed data: roles, users, team, and sample tasks.");
        }
    }
}
