package org.salmanekhalili.jobtrack.dto;

import jakarta.validation.constraints.NotBlank;

public record AuthResponse(@NotBlank String token) {
}
