package org.backmeup.index.core.elasticsearch;

import org.backmeup.index.error.IndexManagerCoreException;

public class SearchProviderException extends IndexManagerCoreException {

    public SearchProviderException(String message, Throwable cause) {
        super(message, cause);
    }

    public SearchProviderException(String message) {
        super(message);
    }

}
