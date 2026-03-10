package com.desafio.case_shortner.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.desafio.case_shortner.dataTransferObject.CreateUrlRequestDto;
import com.desafio.case_shortner.dataTransferObject.UrlResponseDTO;
import com.desafio.case_shortner.entity.Url;
import com.desafio.case_shortner.exception.DuplicateAliasException;
import com.desafio.case_shortner.exception.InvalidUrlException;
import com.desafio.case_shortner.exception.UrlExpiredException;
import com.desafio.case_shortner.exception.UrlNotFoundException;
import com.desafio.case_shortner.repository.UrlRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UrlServiceTest {
  private UrlRepository urlRepository;
  private IdGeneratorService idGeneratorService;
  private UrlService urlService;

  @BeforeEach
  void setUp() {
    urlRepository = mock(UrlRepository.class);
    idGeneratorService = mock(IdGeneratorService.class);
    urlService = new UrlService(urlRepository, idGeneratorService, "http://localhost:8080");
  }

  // --- createUrl ---

  @Test
  void createUrl_shouldSaveAndReturnResponse() {
    when(idGeneratorService.generateUniqueId()).thenReturn("abc123");
    when(urlRepository.save(any())).thenAnswer(i -> i.getArgument(0));

    CreateUrlRequestDto request = new CreateUrlRequestDto("https://www.itau.com.br", null, null);
    UrlResponseDTO response = urlService.createUrl(request);

    assertThat(response.id()).isEqualTo("abc123");
    assertThat(response.shortUrl()).isEqualTo("http://localhost:8080/abc123");
    assertThat(response.originalUrl()).isEqualTo("https://www.itau.com.br");
    verify(urlRepository).save(any(Url.class));
  }

  @Test
  void createUrl_shouldUseCustomAlias_whenProvided() {
    when(idGeneratorService.isValidFormat("my-link")).thenReturn(true);
    when(urlRepository.existsById("my-link")).thenReturn(false);
    when(urlRepository.save(any())).thenAnswer(i -> i.getArgument(0));

    CreateUrlRequestDto request = new CreateUrlRequestDto("https://itau.com.br", null, "my-link");
    UrlResponseDTO response = urlService.createUrl(request);

    assertThat(response.id()).isEqualTo("mylink");
    verify(idGeneratorService, never()).generateUniqueId();
  }

  @Test
  void createUrl_shouldThrow_whenCustomAliasDuplicated() {
    when(idGeneratorService.isValidFormat("taken")).thenReturn(true);
    when(urlRepository.existsById("taken")).thenReturn(true);

    CreateUrlRequestDto request = new CreateUrlRequestDto("https://itau.com.br", null, "taken");
    assertThatThrownBy(() -> urlService.createUrl(request))
        .isInstanceOf(DuplicateAliasException.class);
  }

  @Test
  void createUrl_shouldThrow_whenUrlIsInvalid() {
    assertThatThrownBy(() -> urlService.createUrl(new CreateUrlRequestDto("not-a-url", null, null)))
        .isInstanceOf(InvalidUrlException.class);

    assertThatThrownBy(
            () -> urlService.createUrl(new CreateUrlRequestDto("ftp://file.txt", null, null)))
        .isInstanceOf(InvalidUrlException.class)
        .hasMessageContaining("http or https");
  }

  // --- resolveRedirect ---

  @Test
  void resolveRedirect_shouldReturnOriginalUrl_andIncrementClick() {
    Url entity =
        new Url(
            "abc123",
            "https://itau.com.br",
            "http://localhost:8080/abc123",
            Instant.now(),
            null,
            0);
    when(urlRepository.findById("abc123")).thenReturn(Optional.of(entity));

    String result = urlService.resolveRedirect("abc123");

    assertThat(result).isEqualTo("https://itau.com.br");
    verify(urlRepository).incrementClicks("abc123");
  }

  @Test
  void resolveRedirect_shouldThrow_whenIdNotFound() {
    when(urlRepository.findById("unknown")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> urlService.resolveRedirect("unknown"))
        .isInstanceOf(UrlNotFoundException.class);
  }

  @Test
  void resolveRedirect_shouldThrow_whenUrlExpired() {
    Instant pastDate = Instant.now().minus(1, ChronoUnit.DAYS);
    Url entity =
        new Url(
            "abc123",
            "https://itau.com.br",
            "http://localhost:8080/abc123",
            Instant.now(),
            pastDate,
            0);
    when(urlRepository.findById("abc123")).thenReturn(Optional.of(entity));

    assertThatThrownBy(() -> urlService.resolveRedirect("abc123"))
        .isInstanceOf(UrlExpiredException.class);

    verify(urlRepository, never()).incrementClicks(anyString());
  }

  @Test
  void resolveRedirect_shouldNotThrow_whenExpirationIsNull() {
    Url entity =
        new Url(
            "abc123",
            "https://itau.com.br",
            "http://localhost:8080/abc123",
            Instant.now(),
            null,
            0);
    when(urlRepository.findById("abc123")).thenReturn(Optional.of(entity));

    String result = urlService.resolveRedirect("abc123");
    assertThat(result).isEqualTo("https://itau.com.br");
  }

  @Test
  void resolveRedirect_shouldNotThrow_whenNotYetExpired() {
    Instant future = Instant.now().plus(1, ChronoUnit.DAYS);
    Url entity =
        new Url(
            "abc123",
            "https://itau.com.br",
            "http://localhost:8080/abc123",
            Instant.now(),
            future,
            0);
    when(urlRepository.findById("abc123")).thenReturn(Optional.of(entity));

    String result = urlService.resolveRedirect("abc123");
    assertThat(result).isEqualTo("https://itau.com.br");
  }

  // --- validateUrl ---

  @Test
  void validateUrl_shouldAcceptHttpAndHttps() {
    urlService.validateUrl("http://example.com");
    urlService.validateUrl("https://example.com/path?q=1");
  }

  @Test
  void validateUrl_shouldRejectFtpAndOtherSchemes() {
    assertThatThrownBy(() -> urlService.validateUrl("ftp://files.example.com"))
        .isInstanceOf(InvalidUrlException.class);
  }

  @Test
  void validateUrl_shouldRejectBlankUrl() {
    assertThatThrownBy(() -> urlService.validateUrl("")).isInstanceOf(InvalidUrlException.class);
    assertThatThrownBy(() -> urlService.validateUrl(null)).isInstanceOf(InvalidUrlException.class);
  }
}
