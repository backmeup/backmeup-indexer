package org.backmeup.index.error;

public abstract class IndexManagerCoreException extends RuntimeException {

    public IndexManagerCoreException(String message, Throwable cause) {
        super(message, cause);
    }

    public IndexManagerCoreException(String message) {
        super(message);
    }

    public IndexManagerCoreException(Throwable cause) {
        super(cause);
    }

}
