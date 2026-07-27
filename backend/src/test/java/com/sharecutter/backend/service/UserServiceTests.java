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
        when(userRepository
                .existsByEmailIgnoreCaseAndDeletedAtIsNull(
                        "test.user@example.com"
                ))
                .thenReturn(false);

        when(passwordEncoder.encode("plain-password"))
                .thenReturn("hashed-password");

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
        assertThat(savedUser.getDeletedAt()).isNull();
        assertThat(savedUser.isDeleted()).isFalse();

        verify(userRepository)
                .existsByEmailIgnoreCaseAndDeletedAtIsNull(
                        "test.user@example.com"
                );

        verify(passwordEncoder).encode("plain-password");
    }

    @Test
    void shouldRejectDuplicateActiveEmail() {
        when(userRepository
                .existsByEmailIgnoreCaseAndDeletedAtIsNull(
                        "existing@example.com"
                ))
                .thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(
                "Existing@Example.com",
                "plain-password",
                "Existing",
                "User"
        ))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("existing@example.com");

        verify(userRepository)
                .existsByEmailIgnoreCaseAndDeletedAtIsNull(
                        "existing@example.com"
                );

        verify(passwordEncoder, never())
                .encode(any(CharSequence.class));

        verify(userRepository, never())
                .save(any(UserEntity.class));
    }

    @Test
    void shouldReturnActiveUserById() {
        UUID userId = UUID.randomUUID();

        UserEntity user = new UserEntity(
                "user@example.com",
                "hashed-password",
                "Test",
                "User"
        );

        when(userRepository.findByIdAndDeletedAtIsNull(userId))
                .thenReturn(Optional.of(user));

        UserEntity result = userService.getUserById(userId);

        assertThat(result).isSameAs(user);

        verify(userRepository)
                .findByIdAndDeletedAtIsNull(userId);
    }

    @Test
    void shouldThrowWhenActiveUserIdDoesNotExist() {
        UUID userId = UUID.randomUUID();

        when(userRepository.findByIdAndDeletedAtIsNull(userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(userId))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining(userId.toString());

        verify(userRepository)
                .findByIdAndDeletedAtIsNull(userId);
    }

    @Test
    void shouldUpdateCurrentUserNames() {
        UserEntity user = new UserEntity(
                "user@example.com",
                "hashed-password",
                "Old",
                "Name"
        );

        when(userRepository.save(user))
                .thenReturn(user);

        UserEntity result = userService.updateCurrentUser(
                user,
                "  New  ",
                "  Name  "
        );

        assertThat(result).isSameAs(user);
        assertThat(user.getFirstName()).isEqualTo("New");
        assertThat(user.getLastName()).isEqualTo("Name");

        verify(userRepository).save(user);

        verify(passwordEncoder, never())
                .encode(any(CharSequence.class));
    }

    @Test
    void shouldSoftDeleteExistingActiveUser() {
        UUID userId = UUID.randomUUID();

        UserEntity user = new UserEntity(
                "delete@example.com",
                "hashed-password",
                "Delete",
                "User"
        );

        when(userRepository.findByIdAndDeletedAtIsNull(userId))
                .thenReturn(Optional.of(user));

        when(userRepository.save(user))
                .thenReturn(user);

        assertThat(user.getDeletedAt()).isNull();
        assertThat(user.isDeleted()).isFalse();

        userService.deleteUser(userId);

        assertThat(user.getDeletedAt()).isNotNull();
        assertThat(user.isDeleted()).isTrue();

        verify(userRepository)
                .findByIdAndDeletedAtIsNull(userId);

        verify(userRepository).save(user);

        verify(userRepository, never())
                .delete(user);
    }

    @Test
    void shouldThrowWhenDeletingActiveUserThatDoesNotExist() {
        UUID userId = UUID.randomUUID();

        when(userRepository.findByIdAndDeletedAtIsNull(userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUser(userId))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining(userId.toString());

        verify(userRepository)
                .findByIdAndDeletedAtIsNull(userId);

        verify(userRepository, never())
                .save(any(UserEntity.class));

        verify(userRepository, never())
                .delete(any(UserEntity.class));
    }
}