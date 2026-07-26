package com.sharecutter.backend.service;

import com.sharecutter.backend.domain.entity.UserEntity;
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

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
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
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    public UserEntity getUserByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);

        return userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new UserNotFoundException(normalizedEmail));
    }

    public boolean emailExists(String email) {
        return userRepository.existsByEmailIgnoreCase(
                normalizeEmail(email)
        );
    }

    @Transactional
    public UserEntity updateCurrentUser(
            UserEntity user,
            String firstName,
            String lastName
    ) {
        user.setFirstName(firstName);
        user.setLastName(lastName);

        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(UUID userId) {
        UserEntity user = getUserById(userId);
        userRepository.delete(user);
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }

        return email.trim().toLowerCase(Locale.ROOT);
    }
}