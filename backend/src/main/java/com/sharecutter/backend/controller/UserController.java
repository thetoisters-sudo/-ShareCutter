package com.sharecutter.backend.controller;

import com.sharecutter.backend.domain.entity.UserEntity;
import com.sharecutter.backend.dto.user.CreateUserRequest;
import com.sharecutter.backend.dto.user.UpdateCurrentUserRequest;
import com.sharecutter.backend.dto.user.UpdateUserByAdminRequest;
import com.sharecutter.backend.dto.user.UserResponse;
import com.sharecutter.backend.mapper.UserMapper;
import com.sharecutter.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    public UserController(
            UserService userService,
            UserMapper userMapper
    ) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody CreateUserRequest request
    ) {
        UserEntity createdUser = userService.createUser(
                request.email(),
                request.password(),
                request.firstName(),
                request.lastName()
        );

        UserResponse response =
                userMapper.toResponse(createdUser);

        URI location = URI.create(
                "/api/v1/users/" + response.id()
        );

        return ResponseEntity
                .created(location)
                .body(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(
            @AuthenticationPrincipal UserEntity authenticatedUser
    ) {
        UserResponse response =
                userMapper.toResponse(authenticatedUser);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/me")
    public ResponseEntity<UserResponse> updateCurrentUser(
            @AuthenticationPrincipal UserEntity authenticatedUser,
            @Valid @RequestBody UpdateCurrentUserRequest request
    ) {
        UserEntity updatedUser =
                userService.updateCurrentUser(
                        authenticatedUser,
                        request.firstName(),
                        request.lastName()
                );

        UserResponse response =
                userMapper.toResponse(updatedUser);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserById(
            @PathVariable UUID userId
    ) {
        UserEntity user =
                userService.getUserById(userId);

        UserResponse response =
                userMapper.toResponse(user);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateUserByAdmin(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserByAdminRequest request
    ) {
        UserEntity updatedUser =
                userService.updateUserByAdmin(
                        userId,
                        request.firstName(),
                        request.lastName(),
                        request.role(),
                        request.status()
                );

        UserResponse response =
                userMapper.toResponse(updatedUser);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(
            @PathVariable UUID userId
    ) {
        userService.deleteUser(userId);

        return ResponseEntity.noContent().build();
    }
}