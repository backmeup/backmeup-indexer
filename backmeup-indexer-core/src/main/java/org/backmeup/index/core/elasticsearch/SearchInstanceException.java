package org.backmeup.index.core.elasticsearch;

public class SearchInstanceException extends RuntimeException {

    public SearchInstanceException(String message, Throwable cause) {
        super(message, cause);
    }

    public SearchInstanceException(String message) {
        super(message);
    }

}
