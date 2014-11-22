package org.backmeup.index.client.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import org.apache.commons.io.IOUtils;
import org.junit.rules.ExternalResource;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * Start and stop an embedded web server that returns a single page for all
 * requests.
 * 
 * @see "https://docs.oracle.com/javase/6/docs/jre/api/net/httpserver/spec/index.html"
 * @author <a href="http://www.code-cop.org/">Peter Kofler</a>
 */
@SuppressWarnings("restriction")
public class EmbeddedJdkServer extends ExternalResource {

    private static final String HOST = "localhost";
    private static final int PORT = 7654;

    public final String host = HOST;
    public final int port = PORT;

    private int statusCode;
    private String resourceFileName;

    private HttpServer server;

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public void setResourceFileName(String resourceFileName) {
        this.resourceFileName = resourceFileName;
    }

    @Override
    protected void before() throws IOException {
        server = HttpServer.create(new InetSocketAddress(HOST, PORT), 10);
        server.createContext("/", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                serveGivenStatusAndResponseFileTo(exchange);
            }
        });
        server.start();
    }

    @Override
    protected void after() {
        server.stop(0);
    }

    private void serveGivenStatusAndResponseFileTo(HttpExchange exchange) throws IOException {
        setStatusCode(exchange);
        serveFile(exchange);
    }

    private void setStatusCode(HttpExchange exchange) throws IOException {
        int autoDetectContentLength = 0;
        exchange.sendResponseHeaders(statusCode, autoDetectContentLength);
    }

    private void serveFile(HttpExchange exchange) throws IOException {
        try (OutputStream httpResponse = exchange.getResponseBody()) {
            writeFileTo(httpResponse);
        }
    }

    private void writeFileTo(OutputStream httpResponse) throws IOException {
        try (InputStream resource = getClass().getClassLoader().getResourceAsStream(resourceFileName)) {
            IOUtils.copy(resource, httpResponse);
            httpResponse.flush();
        }
    }
}
