package org.backmeup.index.client.rest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.backmeup.index.api.IndexServer;
import org.backmeup.index.client.IndexerException;
import org.backmeup.index.model.FileInfo;
import org.backmeup.index.model.FileItem;
import org.backmeup.index.model.IndexDocument;
import org.backmeup.index.model.SearchResultAccumulator;
import org.backmeup.index.serializer.JsonSerializer;

public class RestApiServerStub implements IndexServer {

    // TODO PK read values from config
    private String host = "127.0.0.1";
    private int port = 7654;
    private String basePath = "/index";

    private final HttpClient httpClient = HttpClientBuilder.create().build();

    @Override
    public SearchResultAccumulator query(Long userId, String query, String filterBySource, String filterByType, String filterByJob,
            String username) {
        try {

            URI url = buildQueryUrl(userId, query, filterBySource, filterByType, filterByJob, username);
            HttpGet method = new HttpGet(url);
            HttpResponse response = httpClient.execute(method);
            checkStatusIs(200, response);
            String body = getBodyOf(response);
            return JsonSerializer.deserialize(body, SearchResultAccumulator.class);

        } catch (IOException | URISyntaxException e) {
            throw new IndexerException("faled to contact index server", e);
        }

    }

    private URI buildQueryUrl(Long userId, String query, String filterBySource, String filterByType, String filterByJob, String username)
            throws URISyntaxException {
        URIBuilder urlBuilder = basicUrl(userId, "");
        addMandatoryParameter(urlBuilder, "query", query);
        addOptionalParameter(urlBuilder, "source", filterBySource);
        addOptionalParameter(urlBuilder, "type", filterByType);
        addOptionalParameter(urlBuilder, "job", filterByJob);
        addMandatoryParameter(urlBuilder, "username", username);
        URI url = urlBuilder.build();
        System.out.println(url);
        return url;
    }

    private URIBuilder basicUrl(Long userId, String path) throws URISyntaxException {
        return new URIBuilder("http://" + host + ":" + port + basePath + "/" + userId + "/" + path);
    }

    private void addMandatoryParameter(URIBuilder url, String key, String value) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("parameter " + key + " is mandatory");
        }
        url.addParameter(key, value);
    }

    private void addOptionalParameter(URIBuilder url, String key, String value) {
        if (value != null && !value.isEmpty()) {
            url.addParameter(key, value);
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

    private void checkStatusIs(int expectedCode, HttpResponse response) {
        int responseCode = response.getStatusLine().getStatusCode();
        if (responseCode != expectedCode) {
            throw new IllegalArgumentException(">TODO" + responseCode);
        }
    }

    @Override
    public Set<FileItem> filesForJob(Long userId, Long jobId) {
        throw new UnsupportedOperationException("Auto-generated method stub");
    }

    @Override
    public FileInfo fileInfoForFile(Long userId, String fileId) {
        throw new UnsupportedOperationException("Auto-generated method stub");
    }

    @Override
    public String thumbnailPathForFile(Long userId, String fileId) {
        throw new UnsupportedOperationException("Auto-generated method stub");
    }

    @Override
    public String delete(Long userId, Long jobId, Long timestamp) {
        throw new UnsupportedOperationException("Auto-generated method stub");
    }

    @Override
    public String index(Long userId, IndexDocument document) throws IOException {
        //      HttpPost method = new HttpPost(baseUrl);
        //
        //      String jsonDocument = "{\"fields\":{ \"name\":\"Peter\", \"size\":42}, \"largeFields\":{}}";
        //      method.setEntity(new StringEntity(jsonDocument, ContentType.APPLICATION_JSON));
        //      HttpResponse response = httpClient.execute(method);
        //
        //      assertStatusCode(201, response);
        //
        //      ArgumentCaptor<IndexDocument> argumentCaptor = ArgumentCaptor.forClass(IndexDocument.class);
        //      verify(indexClient).index(argumentCaptor.capture());
        //      IndexDocument document = argumentCaptor.getValue();
        //      assertEquals("Peter", document.getFields().get("name"));
        //      assertEquals(Integer.valueOf(42), document.getFields().get("size"));
        throw new UnsupportedOperationException("Auto-generated method stub");
    }

}
