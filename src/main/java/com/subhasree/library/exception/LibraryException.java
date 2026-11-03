package com.subhasree.library.exception;

/** Thrown when a library rule is broken (e.g. no copies left, loan limit reached). */
public class LibraryException extends Exception {

    public LibraryException(String message) {
        super(message);
    }

    public LibraryException(String message, Throwable cause) {
        super(message, cause);
    }
}
