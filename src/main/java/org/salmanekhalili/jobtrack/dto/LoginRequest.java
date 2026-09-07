package org.salmanekhalili.jobtrack.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(@NotBlank(message = "Email cannot be empty.")
                           @Email(message = "Invalid email  format") String email,

                           @Size(min = 8, message = "Password must be at least 8 characters long.")
                           @NotBlank(message = "Password cannot be empty.") String password) {}
