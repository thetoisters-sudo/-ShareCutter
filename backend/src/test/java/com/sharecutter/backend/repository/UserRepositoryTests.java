package com.sharecutter.backend.repository;

import com.sharecutter.backend.domain.entity.UserEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
class UserRepositoryTests {

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldSaveAndFindActiveUserByEmailIgnoringCase() {
        UserEntity user = new UserEntity(
                "Test.User@Example.com",
                "hashed-password",
                "Test",
                "User"
        );

        UserEntity savedUser = userRepository.saveAndFlush(user);

        Optional<UserEntity> foundUser =
                userRepository
                        .findByEmailIgnoreCaseAndDeletedAtIsNull(
                                "TEST.USER@EXAMPLE.COM"
                        );

        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getCreatedAt()).isNotNull();
        assertThat(savedUser.getUpdatedAt()).isNotNull();
        assertThat(savedUser.getDeletedAt()).isNull();

        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail())
                .isEqualTo("test.user@example.com");
    }

    @Test
    void shouldDetectExistingActiveEmailIgnoringCase() {
        UserEntity user = new UserEntity(
                "existing@example.com",
                "hashed-password",
                "Existing",
                "User"
        );

        userRepository.saveAndFlush(user);

        boolean exists = userRepository
                .existsByEmailIgnoreCaseAndDeletedAtIsNull(
                        "EXISTING@EXAMPLE.COM"
                );

        assertThat(exists).isTrue();
    }

    @Test
    void shouldNotFindSoftDeletedUserByEmail() {
        UserEntity user = new UserEntity(
                "deleted.email@example.com",
                "hashed-password",
                "Deleted",
                "User"
        );

        UserEntity savedUser = userRepository.saveAndFlush(user);

        savedUser.softDelete();
        userRepository.saveAndFlush(savedUser);

        Optional<UserEntity> foundUser = userRepository
                .findByEmailIgnoreCaseAndDeletedAtIsNull(
                        "DELETED.EMAIL@EXAMPLE.COM"
                );

        assertThat(savedUser.getDeletedAt()).isNotNull();
        assertThat(savedUser.isDeleted()).isTrue();
        assertThat(foundUser).isEmpty();
    }

    @Test
    void shouldNotFindSoftDeletedUserById() {
        UserEntity user = new UserEntity(
                "deleted.id@example.com",
                "hashed-password",
                "Deleted",
                "User"
        );

        UserEntity savedUser = userRepository.saveAndFlush(user);
        UUID userId = savedUser.getId();

        savedUser.softDelete();
        userRepository.saveAndFlush(savedUser);

        Optional<UserEntity> foundUser =
                userRepository.findByIdAndDeletedAtIsNull(userId);

        assertThat(savedUser.getDeletedAt()).isNotNull();
        assertThat(foundUser).isEmpty();
    }

    @Test
    void shouldTreatSoftDeletedEmailAsAvailable() {
        UserEntity user = new UserEntity(
                "available@example.com",
                "hashed-password",
                "Available",
                "User"
        );

        UserEntity savedUser = userRepository.saveAndFlush(user);

        savedUser.softDelete();
        userRepository.saveAndFlush(savedUser);

        boolean exists = userRepository
                .existsByEmailIgnoreCaseAndDeletedAtIsNull(
                        "AVAILABLE@EXAMPLE.COM"
                );

        assertThat(exists).isFalse();
    }
}