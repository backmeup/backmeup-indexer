package org.backmeup.index.client.rest;

public class RestApiConfig {

    public static final RestApiConfig DEFAULT = new RestApiConfig("127.0.0.1", 8080, "/backmeup-indexer-rest");

    public final String host;
    public final int port;
    public final String basepath;

    public RestApiConfig(String host, int port, String basepath) {
        this.host = host;
        this.port = port;
        this.basepath = basepath;
    }

}
