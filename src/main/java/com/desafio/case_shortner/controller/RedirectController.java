package com.desafio.case_shortner.controller;

import com.desafio.case_shortner.service.UrlService;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RedirectController {

  private final UrlService urlService;

  public RedirectController(UrlService urlService) {
    this.urlService = urlService;
  }

  @GetMapping("/{id:[a-zA-Z0-9_-]+}")
  public ResponseEntity<Void> redirect(@PathVariable String id) {
    String originalUrl = urlService.resolveRedirect(id);
    return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(originalUrl)).build();
  }
}
