package com.sharecutter.backend.dto.portfolio;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PortfolioRenameRequest(

        @NotBlank(message = "Portfolio name must not be blank")
        @Size(
                max = 100,
                message = "Portfolio name must not exceed 100 characters"
        )
        String name

) {
}