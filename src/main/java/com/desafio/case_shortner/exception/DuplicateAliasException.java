package com.desafio.case_shortner.exception;

public class DuplicateAliasException extends  RuntimeException {
    public DuplicateAliasException(String alias) {
        super("Alias already in use: " + alias);
    }
}
