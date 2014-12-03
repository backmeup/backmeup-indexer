package org.backmeup.index.error;

public abstract class IndexManagerCoreException extends RuntimeException {

    protected IndexManagerCoreException(String message, Throwable cause) {
        super(message, cause);
    }

    protected IndexManagerCoreException(String message) {
        super(message);
    }

}
