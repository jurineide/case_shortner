package com.desafio.case_shortner.exception;

public class UrlExpiredException  extends RuntimeException {

    public UrlExpiredException(String id) {
        super("URL has expired for id: " + id);
    }
}