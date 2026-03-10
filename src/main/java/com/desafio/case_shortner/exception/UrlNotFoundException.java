package com.desafio.case_shortner.exception;

public class UrlNotFoundException extends RuntimeException {

  public UrlNotFoundException(String id) {
    super("URL not found for id: " + id);
  }
}
