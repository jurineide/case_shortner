package com.desafio.case_shortner.service;

import com.desafio.case_shortner.repository.UrlRepository;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

import static com.desafio.case_shortner.util.UtilsConstants.*;
import static com.desafio.case_shortner.util.UtilsMessage.FAILED_ATTEMPTS;

@Service
public class IdGeneratorService {

    private final SecureRandom random = new SecureRandom();
    private final UrlRepository urlRepository;

    public IdGeneratorService(UrlRepository urlRepository) {
        this.urlRepository = urlRepository;
    }

    public String generateUniqueId() {
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            String id = generateId();
            if (!urlRepository.existsById(id)) {
                return id;
            }
        }
        throw new IllegalStateException(
                FAILED_ATTEMPTS.formatted(MAX_RETRIES)
        );
    }

    String generateId() {
        StringBuilder stringBuilder = new StringBuilder(ID_LENGTH);
        for (int i = 0; i < ID_LENGTH; i++) {
            stringBuilder.append(BASE62.charAt(random.nextInt(BASE62.length())));
        }
        return stringBuilder.toString();
    }

    public boolean isValidFormat(String format) {
        if (format == null || format.isBlank()) return false;
        return format.matches("[a-zA-Z0-9]{3,50}");
    }
}
