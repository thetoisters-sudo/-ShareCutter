package com.sharecutter.backend.service;

import com.sharecutter.backend.domain.entity.UserEntity;
import com.sharecutter.backend.exception.UserAlreadyExistsException;
import com.sharecutter.backend.exception.UserNotFoundException;
import com.sharecutter.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTests {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(
                userRepository,
                passwordEncoder
        );
    }

    @Test
    void shouldCreateUserWithNormalizedEmail() {
        when(passwordEncoder.encode("plain-password"))
                .thenReturn("hashed-password");

        when(userRepository.existsByEmailIgnoreCase(
                "test.user@example.com"
        )).thenReturn(false);

        when(userRepository.save(any(UserEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserEntity result = userService.createUser(
                "  Test.User@Example.com  ",
                "plain-password",
                "Test",
                "User"
        );

        ArgumentCaptor<UserEntity> userCaptor =
                ArgumentCaptor.forClass(UserEntity.class);

        verify(userRepository).save(userCaptor.capture());

        UserEntity savedUser = userCaptor.getValue();

        assertThat(result).isSameAs(savedUser);
        assertThat(savedUser.getEmail())
                .isEqualTo("test.user@example.com");
        assertThat(savedUser.getPasswordHash())
                .isEqualTo("hashed-password");
        assertThat(savedUser.getFirstName())
                .isEqualTo("Test");
        assertThat(savedUser.getLastName())
                .isEqualTo("User");

        verify(passwordEncoder).encode("plain-password");
    }

    @Test
    void shouldRejectDuplicateEmail() {
        when(userRepository.existsByEmailIgnoreCase(
                "existing@example.com"
        )).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(
                "Existing@Example.com",
                "plain-password",
                "Existing",
                "User"
        ))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("existing@example.com");

        verify(passwordEncoder, never())
                .encode(any(CharSequence.class));

        verify(userRepository, never())
                .save(any(UserEntity.class));
    }

    @Test
    void shouldReturnUserById() {
        UUID userId = UUID.randomUUID();

        UserEntity user = new UserEntity(
                "user@example.com",
                "hashed-password",
                "Test",
                "User"
        );

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        UserEntity result = userService.getUserById(userId);

        assertThat(result).isSameAs(user);
    }

    @Test
    void shouldThrowWhenUserIdDoesNotExist() {
        UUID userId = UUID.randomUUID();

        when(userRepository.findById(userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(userId))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining(userId.toString());
    }

    @Test
    void shouldDeleteExistingUser() {
        UUID userId = UUID.randomUUID();

        UserEntity user = new UserEntity(
                "delete@example.com",
                "hashed-password",
                "Delete",
                "User"
        );

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        userService.deleteUser(userId);

        verify(userRepository).delete(user);
    }
}