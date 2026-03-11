package com.desafio.case_shortner.dataTransferObject;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public record CreateUrlRequestDto(
        @NotBlank(message = "originalUrl must not be blank") String originalUrl, Instant expiration_date, String customFormat) {}
