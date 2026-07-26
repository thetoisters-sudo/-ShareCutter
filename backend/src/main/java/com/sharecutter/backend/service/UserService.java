package com.sharecutter.backend.service;

import com.sharecutter.backend.domain.entity.UserEntity;
import com.sharecutter.backend.domain.enums.UserRole;
import com.sharecutter.backend.domain.enums.UserStatus;
import com.sharecutter.backend.exception.UserAlreadyExistsException;
import com.sharecutter.backend.exception.UserNotFoundException;
import com.sharecutter.backend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserEntity createUser(
            String email,
            String rawPassword,
            String firstName,
            String lastName
    ) {
        String normalizedEmail = normalizeEmail(email);

        if (userRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull(normalizedEmail)) {
            throw new UserAlreadyExistsException(normalizedEmail);
        }

        String passwordHash = passwordEncoder.encode(rawPassword);

        UserEntity user = new UserEntity(
                normalizedEmail,
                passwordHash,
                firstName.trim(),
                lastName.trim()
        );

        return userRepository.save(user);
    }

    public UserEntity getUserById(UUID userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    public UserEntity getUserByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);

        return userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(normalizedEmail)
                .orElseThrow(() -> new UserNotFoundException(normalizedEmail));
    }

    public boolean emailExists(String email) {
        return userRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull(
                normalizeEmail(email)
        );
    }

    @Transactional
    public UserEntity updateCurrentUser(
            UserEntity user,
            String firstName,
            String lastName
    ) {
        user.setFirstName(firstName.trim());
        user.setLastName(lastName.trim());

        return userRepository.save(user);
    }

    @Transactional
    public UserEntity updateUserByAdmin(
            UUID userId,
            String firstName,
            String lastName,
            UserRole role,
            UserStatus status
    ) {
        UserEntity user = getUserById(userId);

        user.setFirstName(firstName.trim());
        user.setLastName(lastName.trim());
        user.setRole(role);
        user.setStatus(status);

        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(UUID userId) {
        UserEntity user = getUserById(userId);

        user.softDelete();

        userRepository.save(user);
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }

        return email.trim().toLowerCase(Locale.ROOT);
    }
}