package com.desafio.case_shortner.service;

import com.desafio.case_shortner.dataTransferObject.CreateUrlRequestDto;
import com.desafio.case_shortner.dataTransferObject.UrlResponseDTO;
import com.desafio.case_shortner.entity.Url;
import com.desafio.case_shortner.exception.DuplicateAliasException;
import com.desafio.case_shortner.exception.InvalidUrlException;
import com.desafio.case_shortner.exception.UrlExpiredException;
import com.desafio.case_shortner.exception.UrlNotFoundException;
import com.desafio.case_shortner.repository.UrlRepository;
import java.net.URI;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UrlService {

  private static final Logger log = LoggerFactory.getLogger(UrlService.class);

  private final UrlRepository urlRepository;
  private final IdGeneratorService idGeneratorService;
  private final String baseUrl;

  public UrlService(
      UrlRepository urlRepository,
      IdGeneratorService idGeneratorService,
      @Value("${app.base-url}") String baseUrl) {
    this.urlRepository = urlRepository;
    this.idGeneratorService = idGeneratorService;
    this.baseUrl = baseUrl;
  }

  @Transactional
  public UrlResponseDTO createUrl(CreateUrlRequestDto request) {
    validateUrl(request.originalUrl());

    String id = resolveId(request);
    String shortUrl = baseUrl + "/" + id;

    Url entity = new Url();
    entity.setId(id);
    entity.setOriginalUrl(request.originalUrl());
    entity.setShortUrl(shortUrl);
    entity.setCreated_date(Instant.now());
    entity.setExpiration_date(request.expiration_date());
    urlRepository.save(entity);

    log.info("Created short URL: id={}, originalUrl={}", id, request.originalUrl());
    return UrlResponseDTO.from(entity);
  }

  @Transactional
  public String resolveRedirect(String id) {
    Url entity = findByIdOrThrow(id);
    checkExpiration(entity);

    urlRepository.incrementClicks(id);

    log.info("Redirecting: id={} -> {}", id, entity.getOriginalUrl());
    return entity.getOriginalUrl();
  }

  @Transactional(readOnly = true)
  public UrlResponseDTO getUrl(String id) {
    Url entity = findByIdOrThrow(id);
    return UrlResponseDTO.from(entity);
  }

  @Transactional(readOnly = true)
  public Page<UrlResponseDTO> listUrls(Pageable pageable) {
    return urlRepository.findAll(pageable).map(UrlResponseDTO::from);
  }

  private String resolveId(CreateUrlRequestDto request) {
    if (request.customFormat() != null && !request.customFormat().isBlank()) {
      String alias = request.customFormat().trim();
      if (!idGeneratorService.isValidFormat(alias)) {
        throw new InvalidUrlException(
            "customAlias must be 3-50 characters (letters, digits, hyphens, underscores)");
      }
      if (urlRepository.existsById(alias)) {
        throw new DuplicateAliasException(alias);
      }
      return alias;
    }
    return idGeneratorService.generateUniqueId();
  }

  private Url findByIdOrThrow(String id) {
    return urlRepository
        .findById(id)
        .orElseThrow(
            () -> {
              log.warn("URL not found: id={}", id);
              return new UrlNotFoundException(id);
            });
  }

  private void checkExpiration(Url entity) {
    if (entity.getExpiration_date() != null && Instant.now().isAfter(entity.getExpiration_date())) {
      throw new UrlExpiredException(entity.getId());
    }
  }

  void validateUrl(String url) {
    if (url == null || url.isBlank()) {
      throw new InvalidUrlException("originalUrl must not be blank");
    }
    try {
      URI uri = URI.create(url);
      String scheme = uri.getScheme();
      if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
        throw new InvalidUrlException("originalUrl must use http or https protocol");
      }
      if (uri.getHost() == null || uri.getHost().isBlank()) {
        throw new InvalidUrlException("originalUrl has no valid host");
      }
    } catch (IllegalArgumentException e) {
      throw new InvalidUrlException("originalUrl is not a valid URL: " + url);
    }
  }
}
