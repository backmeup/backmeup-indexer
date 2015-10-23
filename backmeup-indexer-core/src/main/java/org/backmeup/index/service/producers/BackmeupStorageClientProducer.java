package org.backmeup.index.service.producers;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import org.backmeup.index.config.Configuration;
import org.backmeup.storage.api.StorageClient;
import org.backmeup.storage.client.BackmeupStorageClient;

@ApplicationScoped
public class BackmeupStorageClientProducer {

    private String baseUrl = Configuration.getProperty("backmeup.storage.baseUrl");
    private StorageClient storageClient;

    @Produces
    @ApplicationScoped
    public StorageClient getStorageClient() {
        if (this.storageClient == null) {
            notNull(this.baseUrl, "Storage base url must not be null");
            this.storageClient = new BackmeupStorageClient(this.baseUrl);
        }
        return this.storageClient;
    }

    private static void notNull(Object object, String message) {
        if (object == null) {
            throw new IllegalArgumentException(message);
        }
    }

}
