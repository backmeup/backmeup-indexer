package org.backmeup.index.client.rest;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.Set;

import org.backmeup.index.api.IndexServer;
import org.backmeup.index.client.IndexClientException;
import org.backmeup.index.model.FileInfo;
import org.backmeup.index.model.FileItem;
import org.backmeup.index.model.IndexDocument;
import org.backmeup.index.model.SearchResultAccumulator;
import org.backmeup.index.model.User;
import org.backmeup.index.serializer.Json;

/**
 * Remote stub of the RESTful index server component.
 * 
 * @author <a href="http://www.code-cop.org/">Peter Kofler</a>
 */
public class RestApiIndexServerStub implements IndexServer {

    private final HttpMethods http = new HttpMethods();
    private final RestUrlsIndex urls;

    public RestApiIndexServerStub(RestApiConfig config) {
        urls = new RestUrlsIndex(config);
    }

    @Override
    public SearchResultAccumulator query(User userId, String query, String filterBySource, String filterByType, String filterByJob,
            String username) {
        try {

            URI url = urls.forQuery(userId, query, filterBySource, filterByType, filterByJob, username);
            String body = http.get(url, 200);
            return Json.deserialize(body, SearchResultAccumulator.class);

        } catch (IOException | URISyntaxException e) {
            throw failedToContactServer(e);
        }
    }

    @Override
    public Set<FileItem> filesForJob(User userId, Long jobId) {
        try {

            URI url = urls.forFilesOfJob(userId, jobId);
            String body = http.get(url, 200);
            return Json.deserializeSetOfFileItems(body);

        } catch (IOException | URISyntaxException e) {
            throw failedToContactServer(e);
        }
    }

    @Override
    public FileInfo fileInfoForFile(User userId, String fileId) {
        try {

            URI url = urls.forFileInfo(userId, fileId);
            String body = http.get(url, 200);
            return Json.deserialize(body, FileInfo.class);

        } catch (IOException | URISyntaxException e) {
            throw failedToContactServer(e);
        }
    }

    @Override
    public String thumbnailPathForFile(User userId, String fileId) {
        try {

            URI url = urls.forThumbnail(userId, fileId);
            String body = http.get(url, 200);
            return body;

        } catch (IOException | URISyntaxException e) {
            throw failedToContactServer(e);
        }
    }

    @Override
    public String delete(User userId, Long jobId, Date timestamp) {
        try {

            URI url = urls.forDelete(userId, jobId, timestamp);
            String body = http.delete(url, 202);
            return body;

        } catch (IOException | URISyntaxException e) {
            throw failedToContactServer(e);
        }
    }

    @Override
    public String index(User userId, IndexDocument document) throws IOException {
        try {

            URI url = urls.forNewDocument(userId);
            String jsonPayload = Json.serialize(document);
            String body = http.post(url, jsonPayload, 201);
            return body;

        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }

    private IndexClientException failedToContactServer(Exception problem) {
        return new IndexClientException("faled to contact index server", problem);
    }

}