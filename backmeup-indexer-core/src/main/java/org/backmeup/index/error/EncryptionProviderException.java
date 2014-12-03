package org.backmeup.index.error;

public class EncryptionProviderException extends IndexManagerCoreException {

    public EncryptionProviderException(String message, Throwable cause) {
        super(message, cause);
    }

    public EncryptionProviderException(String message) {
        super(message);
    }

    public EncryptionProviderException(Throwable cause) {
        super(cause);
    }

}
