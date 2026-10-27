package com.taskmanagement.secure.service;

import com.taskmanagement.secure.dto.auth.AuthResponse;
import com.taskmanagement.secure.dto.auth.LoginRequest;
import com.taskmanagement.secure.dto.auth.RegisterRequest;
import com.taskmanagement.secure.entity.RefreshToken;
import com.taskmanagement.secure.entity.Role;
import com.taskmanagement.secure.entity.RoleName;
import com.taskmanagement.secure.entity.User;
import com.taskmanagement.secure.exception.ConflictException;
import com.taskmanagement.secure.exception.UnauthorizedException;
import com.taskmanagement.secure.repository.RefreshTokenRepository;
import com.taskmanagement.secure.repository.RoleRepository;
import com.taskmanagement.secure.repository.UserRepository;
import com.taskmanagement.secure.security.JwtTokenProvider;
import com.taskmanagement.secure.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider tokenProvider;

    @InjectMocks
    private AuthService authService;

    private Role userRole;

    @BeforeEach
    void setUp() {
        userRole = new Role(RoleName.ROLE_USER);
        userRole.setId(1);
    }

    @Test
    @DisplayName("Should successfully register a new user with BCrypt hashed password and default ROLE_USER")
    void shouldRegisterNewUser() {
        RegisterRequest request = new RegisterRequest("Test User", "test@example.com", "Password@123");

        when(userRepository.existsByEmailIgnoreCase("test@example.com")).thenReturn(false);
        when(roleRepository.findByName(RoleName.ROLE_USER)).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("Password@123")).thenReturn("hashedPassword123");

        User savedUser = new User("test@example.com", "hashedPassword123", "Test User");
        savedUser.setId(10L);
        savedUser.addRole(userRole);

        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(tokenProvider.generateAccessToken(any(UserPrincipal.class))).thenReturn("mock-access-token");
        when(tokenProvider.getExpirationMs()).thenReturn(900000L);
        when(tokenProvider.getRefreshExpirationMs()).thenReturn(604800000L);

        RefreshToken refreshToken = new RefreshToken(savedUser, "mock-refresh-token", Instant.now().plusMillis(604800000L));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(refreshToken);

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("mock-access-token", response.getAccessToken());
        assertEquals("mock-refresh-token", response.getRefreshToken());
        assertEquals("test@example.com", response.getUser().getEmail());
        assertTrue(response.getUser().getRoles().contains("ROLE_USER"));

        verify(passwordEncoder).encode("Password@123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw ConflictException when registering with an existing email")
    void shouldThrowExceptionWhenEmailExists() {
        RegisterRequest request = new RegisterRequest("Test User", "existing@example.com", "Password@123");
        when(userRepository.existsByEmailIgnoreCase("existing@example.com")).thenReturn(true);

        assertThrows(ConflictException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should reject login if user account is deactivated")
    void shouldRejectDeactivatedUserLogin() {
        LoginRequest request = new LoginRequest("inactive@example.com", "Password@123");

        User inactiveUser = new User("inactive@example.com", "hashedPassword", "Inactive User");
        inactiveUser.setId(20L);
        inactiveUser.setActive(false);
        inactiveUser.addRole(userRole);

        UserPrincipal principal = UserPrincipal.create(inactiveUser);
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authToken);
        when(userRepository.findById(20L)).thenReturn(Optional.of(inactiveUser));

        assertThrows(UnauthorizedException.class, () -> authService.login(request));
    }
}
