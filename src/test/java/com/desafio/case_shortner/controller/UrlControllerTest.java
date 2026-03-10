package com.desafio.case_shortner.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.desafio.case_shortner.dataTransferObject.CreateUrlRequestDto;
import com.desafio.case_shortner.dataTransferObject.UrlResponseDTO;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class UrlControllerTest {
  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Value("${app.api-key}")
  private String apiKey;

  // --- POST /v1/urls ---

  @Test
  void createUrl_shouldReturn201WithValidResponse() throws Exception {
    CreateUrlRequestDto request =
        new CreateUrlRequestDto("https://www.itau.com.br/minha-conta", null, null);

    MvcResult result =
        mockMvc
            .perform(
                post("/v1/urls")
                    .header("X-API-Key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.shortUrl").isNotEmpty())
            .andExpect(jsonPath("$.originalUrl").value("https://www.itau.com.br/minha-conta"))
            .andExpect(jsonPath("$.createdAt").isNotEmpty())
            .andReturn();

    UrlResponseDTO response =
        objectMapper.readValue(result.getResponse().getContentAsString(), UrlResponseDTO.class);
    assertThat(response.id()).hasSize(6);
  }

  @Test
  void createUrl_shouldReturn401_whenApiKeyMissing() throws Exception {
    CreateUrlRequestDto request = new CreateUrlRequestDto("https://itau.com.br", null, null);

    mockMvc
        .perform(
            post("/v1/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
  }

  @Test
  void createUrl_shouldReturn400_whenOriginalUrlIsMissing() throws Exception {
    mockMvc
        .perform(
            post("/v1/urls")
                .header("X-API-Key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"originalUrl\": \"\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
  }

  @Test
  void createUrl_shouldReturn422_whenUrlSchemeIsInvalid() throws Exception {
    CreateUrlRequestDto request = new CreateUrlRequestDto("ftp://invalid.com", null, null);

    mockMvc
        .perform(
            post("/v1/urls")
                .header("X-API-Key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.error").value("INVALID_URL"));
  }

  @Test
  void createUrl_shouldReturn201WithCustomAlias() throws Exception {
    CreateUrlRequestDto request = new CreateUrlRequestDto("https://itau.com.br", null, "itau-home");

    mockMvc
        .perform(
            post("/v1/urls")
                .header("X-API-Key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value("itau-home"));
  }

  @Test
  void createUrl_shouldReturn409_whenCustomAliasDuplicated() throws Exception {
    CreateUrlRequestDto first = new CreateUrlRequestDto("https://itau.com.br", null, "duplicated");
    CreateUrlRequestDto second =
        new CreateUrlRequestDto("https://bradesco.com.br", null, "duplicated");

    mockMvc
        .perform(
            post("/v1/urls")
                .header("X-API-Key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(first)))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            post("/v1/urls")
                .header("X-API-Key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(second)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error").value("DUPLICATE_ALIAS"));
  }

  // --- GET /v1/urls/{id} ---

  @Test
  void getUrl_shouldReturn200WithDetails() throws Exception {
    String id = createUrl("https://itau.com.br/investimentos");

    mockMvc
        .perform(get("/v1/urls/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id))
        .andExpect(jsonPath("$.originalUrl").value("https://itau.com.br/investimentos"))
        .andExpect(jsonPath("$.clickCount").value(0));
  }

  @Test
  void getUrl_shouldReturn404_whenNotFound() throws Exception {
    mockMvc
        .perform(get("/v1/urls/nonexistent"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("NOT_FOUND"));
  }

  // --- GET /{id} (redirect) ---

  @Test
  void redirect_shouldReturn302_toOriginalUrl() throws Exception {
    String id = createUrl("https://itau.com.br/cartoes");

    mockMvc
        .perform(get("/" + id))
        .andExpect(status().isFound())
        .andExpect(header().string("Location", "https://itau.com.br/cartoes"));
  }

  @Test
  void redirect_shouldReturn404_whenIdNotFound() throws Exception {
    mockMvc.perform(get("/nonexistent")).andExpect(status().isNotFound());
  }

  @Test
  void redirect_shouldReturn410_whenUrlExpired() throws Exception {
    Instant pastDate = Instant.now().minus(1, ChronoUnit.DAYS);
    CreateUrlRequestDto request = new CreateUrlRequestDto("https://itau.com.br", pastDate, null);

    MvcResult result =
        mockMvc
            .perform(
                post("/v1/urls")
                    .header("X-API-Key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andReturn();

    UrlResponseDTO response =
        objectMapper.readValue(result.getResponse().getContentAsString(), UrlResponseDTO.class);

    mockMvc
        .perform(get("/" + response.id()))
        .andExpect(status().isGone())
        .andExpect(jsonPath("$.error").value("URL_EXPIRED"));
  }

  @Test
  void redirect_shouldIncrementClickCount() throws Exception {
    String id = createUrl("https://itau.com.br/saldo");

    mockMvc.perform(get("/" + id)).andExpect(status().isFound());
    mockMvc.perform(get("/" + id)).andExpect(status().isFound());

    mockMvc
        .perform(get("/v1/urls/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.clickCount").value(2));
  }

  // --- GET /v1/urls (list) ---

  @Test
  void listUrls_shouldReturnPage() throws Exception {
    createUrl("https://itau.com.br/a");
    createUrl("https://itau.com.br/b");

    mockMvc
        .perform(get("/v1/urls?page=0&size=10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray());
  }

  // Helper

  private String createUrl(String originalUrl) throws Exception {
    CreateUrlRequestDto request = new CreateUrlRequestDto(originalUrl, null, null);
    MvcResult result =
        mockMvc
            .perform(
                post("/v1/urls")
                    .header("X-API-Key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andReturn();
    return objectMapper
        .readValue(result.getResponse().getContentAsString(), UrlResponseDTO.class)
        .id();
  }
}
