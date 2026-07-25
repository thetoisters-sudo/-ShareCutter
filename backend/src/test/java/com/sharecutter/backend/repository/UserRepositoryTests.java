package com.sharecutter.backend.repository;

import com.sharecutter.backend.domain.entity.UserEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest

@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)

class UserRepositoryTests {

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldSaveAndFindUserByEmailIgnoringCase() {
        UserEntity user = new UserEntity(
                "Test.User@Example.com",
                "hashed-password",
                "Test",
                "User"
        );

        UserEntity savedUser = userRepository.saveAndFlush(user);

        Optional<UserEntity> foundUser =
                userRepository.findByEmailIgnoreCase("TEST.USER@EXAMPLE.COM");

        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getCreatedAt()).isNotNull();
        assertThat(savedUser.getUpdatedAt()).isNotNull();

        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail())
                .isEqualTo("test.user@example.com");
    }

    @Test
    void shouldDetectExistingEmailIgnoringCase() {
        UserEntity user = new UserEntity(
                "existing@example.com",
                "hashed-password",
                "Existing",
                "User"
        );

        userRepository.saveAndFlush(user);

        boolean exists =
                userRepository.existsByEmailIgnoreCase("EXISTING@EXAMPLE.COM");

        assertThat(exists).isTrue();
    }
}