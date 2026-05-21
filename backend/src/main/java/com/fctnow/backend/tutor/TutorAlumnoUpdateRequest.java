package com.fctnow.backend.tutor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TutorAlumnoUpdateRequest(
    @NotBlank
    @Size(max = 150)
    String displayName,

    @NotBlank
    @Size(max = 180)
    String username) {
}
