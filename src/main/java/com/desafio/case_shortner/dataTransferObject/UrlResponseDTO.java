package com.desafio.case_shortner.dataTransferObject;


import com.desafio.case_shortner.entity.Url;

public record UrlResponseDTO (
     String id,
     String originalUrl,
     String shortUrl,
     String created_date,
     String expiration_date,
     Integer clicks
) {
    public static UrlResponseDTO from(Url url) {
        return new UrlResponseDTO(
                url.getId(),
                url.getOriginalUrl(),
                url.getShortUrl(),
                url.getCreated_date(),
                url.getExpiration_date(),
                url.getClicks()
        );
    }
}

