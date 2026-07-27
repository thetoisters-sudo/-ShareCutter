package com.sharecutter.backend.dto.user;

import com.sharecutter.backend.domain.enums.UserRole;
import com.sharecutter.backend.domain.enums.UserStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateUserByAdminRequest(

        @NotBlank
        @Size(max = 100)
        String firstName,

        @NotBlank
        @Size(max = 100)
        String lastName,

        @NotNull
        UserRole role,

        @NotNull
        UserStatus status

) {
}