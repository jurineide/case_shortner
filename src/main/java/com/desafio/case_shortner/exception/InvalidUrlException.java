package com.desafio.case_shortner.exception;

public class InvalidUrlException extends RuntimeException {

  public InvalidUrlException(String message) {
    super(message);
  }
}
