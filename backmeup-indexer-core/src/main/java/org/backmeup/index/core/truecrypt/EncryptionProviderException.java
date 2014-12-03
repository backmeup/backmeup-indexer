package org.backmeup.index.core.truecrypt;

import org.backmeup.index.error.IndexManagerCoreException;

public class EncryptionProviderException extends IndexManagerCoreException {

    public EncryptionProviderException(String message, Throwable cause) {
        super(message, cause);
    }

    public EncryptionProviderException(String message) {
        super(message);
    }

}
