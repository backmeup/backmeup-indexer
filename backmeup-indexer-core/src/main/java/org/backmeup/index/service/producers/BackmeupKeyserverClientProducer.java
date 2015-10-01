package org.backmeup.index.service.producers;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import org.backmeup.index.config.Configuration;
import org.backmeup.keyserver.client.KeyserverClient;

@ApplicationScoped
public class BackmeupKeyserverClientProducer {

    private String baseUrl = Configuration.getProperty("backmeup.keyserver.baseUrl");
    private String appId = Configuration.getProperty("backmeup.indexer.appId");
    private String appSecret = Configuration.getProperty("backmeup.indexer.appSecret");

    private KeyserverClient keyserverClient;

    @Produces
    @ApplicationScoped
    public KeyserverClient getKeyserverClient() {
        if (this.keyserverClient == null) {
            notNull(this.baseUrl, "Keyserver base url must not be null");
            notNull(this.appId, "Keyserver app id must not be null");
            notNull(this.appSecret, "Keyserver app secret must not be null");

            this.keyserverClient = new KeyserverClient(this.baseUrl, this.appId, this.appSecret);
        }
        return this.keyserverClient;
    }

    private static void notNull(Object object, String message) {
        if (object == null) {
            throw new IllegalArgumentException(message);
        }
    }

}
