package org.backmeup.index.client.rest;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

import org.backmeup.index.api.IndexServer;
import org.backmeup.index.client.IndexerException;
import org.backmeup.index.model.FileInfo;
import org.backmeup.index.model.FileItem;
import org.backmeup.index.model.IndexDocument;
import org.backmeup.index.model.SearchResultAccumulator;
import org.backmeup.index.serializer.JsonSerializer;

import com.google.gson.reflect.TypeToken;

public class RestApiServerStub implements IndexServer {

    private final HttpMethods http = new HttpMethods();
    private final RestUrls urls;

    public RestApiServerStub(RestApiConfig config) {
        urls = new RestUrls(config);
    }

    @Override
    public SearchResultAccumulator query(Long userId, String query, String filterBySource, String filterByType, String filterByJob,
            String username) {
        try {

            URI url = urls.forQuery(userId, query, filterBySource, filterByType, filterByJob, username);
            String body = http.get(url, 200);
            return deserialize(body, SearchResultAccumulator.class);

        } catch (IOException | URISyntaxException e) {
            throw failedToContactServer(e);
        }
    }

    @Override
    public Set<FileItem> filesForJob(Long userId, Long jobId) {
        try {

            URI url = urls.forFilesOfJob(userId, jobId);
            String body = http.get(url, 200);
            return deserializeSetOfFileItems(body);

        } catch (IOException | URISyntaxException e) {
            throw failedToContactServer(e);
        }
    }

    @Override
    public FileInfo fileInfoForFile(Long userId, String fileId) {
        try {

            URI url = urls.forFileInfo(userId, fileId);
            String body = http.get(url, 200);
            return deserialize(body, FileInfo.class);

        } catch (IOException | URISyntaxException e) {
            throw failedToContactServer(e);
        }
    }

    @Override
    public String thumbnailPathForFile(Long userId, String fileId) {
        try {

            URI url = urls.forThumbnail(userId, fileId);
            String body = http.get(url, 200);
            return body;

        } catch (IOException | URISyntaxException e) {
            throw failedToContactServer(e);
        }
    }

    @Override
    public String delete(Long userId, Long jobId, Long timestamp) {
        try {

            URI url = urls.forDelete(userId, jobId, timestamp);
            String body = http.delete(url, 202);
            return body;

        } catch (IOException | URISyntaxException e) {
            throw failedToContactServer(e);
        }
    }

    @Override
    public String index(Long userId, IndexDocument document) throws IOException {
        try {

            URI url = urls.forNewDocument(userId);
            String jsonPayload = JsonSerializer.serialize(document);
            String body = http.post(url, jsonPayload, 201);
            return body;

        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }

    private <T> T deserialize(String body, Class<T> type) {
        return JsonSerializer.deserialize(body, type);
    }

    private Set<FileItem> deserializeSetOfFileItems(String body) {
        return JsonSerializer.deserialize(body, new TypeToken<Set<FileItem>>() {
        }.getType());
    }

    private IndexerException failedToContactServer(Exception problem) {
        return new IndexerException("faled to contact index server", problem);
    }

}
