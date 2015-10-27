package org.backmeup.index.client.rest;

import java.io.IOException;

import org.backmeup.index.api.IndexDocumentUploadClient;
import org.backmeup.index.api.IndexDocumentUploadServer;
import org.backmeup.index.client.config.Configuration;
import org.backmeup.index.model.IndexDocument;
import org.backmeup.index.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adapts the local index document upload client to the remote index document upload server.
 * 
 */
public class RestApiIndexDocumentUploadClient implements IndexDocumentUploadClient {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final IndexDocumentUploadServer server;
    private final User currUser;

    public RestApiIndexDocumentUploadClient(User currUser) {
        this.currUser = currUser;
        this.server = new RestApiIndexDocumentUploadServerStub(getRESTServerEndpointLocation());
    }

    private RestApiConfig getRESTServerEndpointLocation() {
        RestApiConfig config;
        try {
            String host = Configuration.getProperty("backmeup.indexer.rest.host");
            String port = Configuration.getProperty("backmeup.indexer.rest.port");
            String baseurl = Configuration.getProperty("backmeup.indexer.rest.baseurl");
            //check if a configuration was provided or if we're using the default config
            if ((host != null) && (port != null) && (baseurl != null)) {
                config = new RestApiConfig(host, Integer.valueOf(port), baseurl);
            } else {
                config = RestApiConfig.DEFAULT;
            }
        } catch (Exception e) {
            this.logger
                    .info("not able to read host, port or baseurl from backmeup-index-client.properties for index-client REST endpoint location, defaulting to static configuration");
            config = RestApiConfig.DEFAULT;
        }
        return config;
    }

    @Override
    public String uploadForSharing(IndexDocument document) throws IOException {
        return this.server.uploadForSharing(this.currUser, document);
    }

    @Override
    public void close() {
    }

}
