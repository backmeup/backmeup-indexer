package org.backmeup.index.core.truecrypt;

public class EncryptionProviderException extends RuntimeException {

    public EncryptionProviderException(String message, Throwable cause) {
        super(message, cause);
    }

    public EncryptionProviderException(String message) {
        super(message);
    }

}
