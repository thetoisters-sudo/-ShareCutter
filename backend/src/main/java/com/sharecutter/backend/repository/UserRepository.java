package com.sharecutter.backend.repository;

import com.sharecutter.backend.domain.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByIdAndDeletedAtIsNull(UUID userId);

    Optional<UserEntity> findByEmailIgnoreCaseAndDeletedAtIsNull(
            String email
    );

    boolean existsByEmailIgnoreCaseAndDeletedAtIsNull(
            String email
    );
}