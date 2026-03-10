package com.desafio.case_shortner.dataTransferObject;

import java.time.Instant;

public record CreateUrlRequestDto(
    String originalUrl, Instant expiration_date, String customFormat) {}
