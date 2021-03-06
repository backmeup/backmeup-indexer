package org.backmeup.index.client.rest;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

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
        this.urls = new RestUrlsIndex(config);
    }

    @Override
    public SearchResultAccumulator query(User user, String query, String filterBySource, String filterByType, String filterByJob,
            String filterByOwner, String filterByTag, String username, Long queryOffSetStart, Long queryMaxResults) {
        try {
            URI url = this.urls.forQuery(user, query, filterBySource, filterByType, filterByJob, filterByOwner, filterByTag, username,
                    queryOffSetStart, queryMaxResults);
            String body = this.http.get(url, 200);
            return Json.deserialize(body, SearchResultAccumulator.class);

        } catch (IOException | URISyntaxException e) {
            throw failedToContactServer(e);
        }
    }

    @Override
    public Set<FileItem> filesForJob(User user, Long jobId) {
        try {
            URI url = this.urls.forFilesOfJob(user, jobId);
            String body = this.http.get(url, 200);
            return Json.deserializeSetOfFileItems(body);

        } catch (IOException | URISyntaxException e) {
            throw failedToContactServer(e);
        }
    }

    @Override
    public FileInfo fileInfoForFile(User user, String fileId) {
        try {

            URI url = this.urls.forFileInfo(user, fileId);
            String body = this.http.get(url, 200);
            return Json.deserialize(body, FileInfo.class);

        } catch (IOException | URISyntaxException e) {
            throw failedToContactServer(e);
        }
    }

    @Override
    public String thumbnailPathForFile(User user, String fileId) {
        try {

            URI url = this.urls.forThumbnail(user, fileId);
            String body = this.http.get(url, 200);
            return body;

        } catch (IOException | URISyntaxException e) {
            throw failedToContactServer(e);
        }
    }

    @Override
    public String delete(User user, Long jobId, Date timestamp) {
        try {

            URI url = this.urls.forDelete(user, jobId, timestamp);
            String body = this.http.delete(url, 202);
            return body;

        } catch (IOException | URISyntaxException e) {
            throw failedToContactServer(e);
        }
    }

    @Override
    public String delete(User user, UUID indexFragmentUUID) {
        try {
            URI url = this.urls.forDelete(user, indexFragmentUUID);
            String body = this.http.delete(url, 202);
            return body;

        } catch (IOException | URISyntaxException e) {
            throw failedToContactServer(e);
        }
    }

    @Override
    public String index(User user, IndexDocument document) throws IOException {
        try {

            URI url = this.urls.forNewDocument(user);
            String jsonPayload = Json.serialize(document);
            String body = this.http.post(url, jsonPayload, 201);
            return body;

        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }

    private IndexClientException failedToContactServer(Exception problem) {
        return new IndexClientException("faled to contact index server", problem);
    }

}
