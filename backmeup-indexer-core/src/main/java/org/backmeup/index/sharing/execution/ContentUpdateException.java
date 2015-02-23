package org.backmeup.index.sharing.execution;

public class ContentUpdateException extends RuntimeException {

    public ContentUpdateException(String message, Throwable cause) {
        super(message, cause);
    }

    public ContentUpdateException(String message) {
        super(message);
    }

}
