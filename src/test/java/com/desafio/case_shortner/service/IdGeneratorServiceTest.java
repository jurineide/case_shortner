package com.desafio.case_shortner.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.desafio.case_shortner.repository.UrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class IdGeneratorServiceTest {
  private UrlRepository urlRepository;
  private IdGeneratorService idGeneratorService;

  @BeforeEach
  void setUp() {
    urlRepository = mock(UrlRepository.class);
    idGeneratorService = new IdGeneratorService(urlRepository);
  }

  @Test
  void generateId_shouldReturn6Characters() {
    String id = idGeneratorService.generateId();
    assertThat(id).hasSize(6);
  }

  @Test
  void generateId_shouldContainOnlyBase62Characters() {
    String id = idGeneratorService.generateId();
    assertThat(id).matches("[a-zA-Z0-9]{6}");
  }

  @Test
  void generateId_shouldProduceDifferentValuesOnConsecutiveCalls() {
    long distinctCount =
        java.util.stream.IntStream.range(0, 100)
            .mapToObj(i -> idGeneratorService.generateId())
            .distinct()
            .count();
    assertThat(distinctCount).isGreaterThan(95);
  }

  @Test
  void generateUniqueId_shouldReturnIdWhenNoCollision() {
    when(urlRepository.existsById(anyString())).thenReturn(false);
    String id = idGeneratorService.generateUniqueId();
    assertThat(id).isNotBlank().hasSize(6);
  }

  @Test
  void generateUniqueId_shouldRetryOnCollision() {
    when(urlRepository.existsById(anyString())).thenReturn(true).thenReturn(true).thenReturn(false);
    String id = idGeneratorService.generateUniqueId();
    assertThat(id).isNotBlank();
  }

  @Test
  void isValidAlias_shouldAcceptValidAlias() {
    assertThat(idGeneratorService.isValidFormat("itau-home")).isTrue();
    assertThat(idGeneratorService.isValidFormat("abc")).isTrue();
    assertThat(idGeneratorService.isValidFormat("my_alias_123")).isTrue();
  }

  @Test
  void isValidAlias_shouldRejectInvalidAlias() {
    assertThat(idGeneratorService.isValidFormat(null)).isFalse();
    assertThat(idGeneratorService.isValidFormat("")).isFalse();
    assertThat(idGeneratorService.isValidFormat("ab")).isFalse(); // too short
    assertThat(idGeneratorService.isValidFormat("has space")).isFalse();
    assertThat(idGeneratorService.isValidFormat("has@special")).isFalse();
  }
}
