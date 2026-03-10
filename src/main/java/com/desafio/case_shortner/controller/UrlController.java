package com.desafio.case_shortner.controller;

import com.desafio.case_shortner.dataTransferObject.CreateUrlRequestDto;
import com.desafio.case_shortner.dataTransferObject.UrlResponseDTO;
import com.desafio.case_shortner.service.UrlService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/urls")
public class UrlController {

  private final UrlService urlService;

  public UrlController(UrlService urlService) {
    this.urlService = urlService;
  }

  @PostMapping
  public ResponseEntity<UrlResponseDTO> create(@Valid @RequestBody CreateUrlRequestDto request) {
    UrlResponseDTO response = urlService.createUrl(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping("/{id}")
  public ResponseEntity<UrlResponseDTO> getById(@PathVariable String id) {
    return ResponseEntity.ok(urlService.getUrl(id));
  }

  @GetMapping
  public ResponseEntity<Page<UrlResponseDTO>> list(@PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(urlService.listUrls(pageable));
  }
}
