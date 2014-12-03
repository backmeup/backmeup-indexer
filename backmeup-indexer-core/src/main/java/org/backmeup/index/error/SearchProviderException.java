package org.backmeup.index.error;

public class SearchProviderException extends IndexManagerCoreException {

    public SearchProviderException(String message, Throwable cause) {
        super(message, cause);
    }

    public SearchProviderException(String message) {
        super(message);
    }

}
