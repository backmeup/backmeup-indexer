package org.backmeup.index.client.rest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;

/**
 * Wrapper around http-commons for the call of the index server.
 * 
 * @author <a href="http://www.code-cop.org/">Peter Kofler</a>
 */
public class HttpMethods {

    private final HttpClient httpClient = HttpClientBuilder.create().build();

    public String get(URI url, int expectedCode) throws IOException {
        HttpGet method = new HttpGet(url);
        return invoke(method, expectedCode);
    }

    public String delete(URI url, int expectedCode) throws IOException {
        HttpDelete method = new HttpDelete(url);
        return invoke(method, expectedCode);
    }

    public String post(URI url, String jsonDocument, int expectedCode) throws IOException {
        HttpPost method = new HttpPost(url);
        method.setEntity(new StringEntity(jsonDocument, ContentType.APPLICATION_JSON));
        return invoke(method, expectedCode);
    }

    public String invoke(HttpRequestBase method, int expectedCode) throws IOException, ClientProtocolException {
        HttpResponse response = this.httpClient.execute(method);
        String body = getBodyOf(response);
        checkStatusIs(expectedCode, response, body);
        return body;
    }

    private void checkStatusIs(int expectedCode, HttpResponse response, String body) {
        int responseCode = response.getStatusLine().getStatusCode();
        if (responseCode != expectedCode) {
            throw new IllegalArgumentException("expected HTTP response code " + expectedCode + " but was "
                    + responseCode + "\nbody: " + body);
        }
    }

    private String getBodyOf(HttpResponse response) throws IOException {
        HttpEntity entity = response.getEntity();
        String encoding = getEncodingFrom(entity);
        return read(entity, encoding);
    }

    private String getEncodingFrom(HttpEntity entity) {
        Header contentEncodingHeader = entity.getContentEncoding();
        return contentEncodingHeader != null ? contentEncodingHeader.getValue() : null;
    }

    private String read(HttpEntity entity, String encoding) throws IOException {
        try (InputStream content = entity.getContent()) {
            final String body;
            if (encoding == null) {
                body = IOUtils.toString(content);
            } else {
                body = IOUtils.toString(content, encoding);
            }
            System.out.println(body);
            return body;
        }
    }

}
