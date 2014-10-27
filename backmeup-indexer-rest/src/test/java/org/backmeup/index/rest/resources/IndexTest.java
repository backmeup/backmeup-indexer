package org.backmeup.index.rest.resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.backmeup.index.client.IndexClient;
import org.backmeup.index.model.CountedEntry;
import org.backmeup.index.model.SearchEntry;
import org.backmeup.index.model.SearchResultAccumulator;
import org.junit.Rule;
import org.junit.Test;

public class IndexTest {

    private static final Long USER = 1L;

    @Rule
    public final EmbeddedRestServer server = new EmbeddedRestServer(IndexWithMockedFactory.class);
    private final String HOST = server.host;
    private final int PORT = server.port;

    private HttpClient client = HttpClientBuilder.create().build();

    public static class IndexWithMockedFactory extends Index {
        @Override
        protected IndexClient getIndexClient(Long userId) {
            assertEquals(USER, userId);

            IndexClient client = mock(IndexClient.class);
            SearchResultAccumulator sr = oneFile();
            when(client.queryBackup("find_me", null, null, null, "peter")).thenReturn(sr);
            return client;
        }
    }

    public static SearchResultAccumulator oneFile() {
        SearchResultAccumulator searchResponse = new SearchResultAccumulator();
        searchResponse.setByJob(Arrays.asList(new CountedEntry("first Job", 1), new CountedEntry("next Job", 1)));
        searchResponse.setBySource(Arrays.asList(new CountedEntry("Dropbox", 2), new CountedEntry("Facebook", 2)));
        searchResponse.setByType(Arrays.asList(new CountedEntry("Type", 3)));
        searchResponse.setFiles(Arrays.asList(new SearchEntry("fileId", new Date(), "type", "A wonderful file (title)", "thmbnailUrl",
                "Dropbox", "first Job")));
        return searchResponse;
    }

    @Test
    public void shouldGetSearchResultForUserAndQuery() throws IOException {
        // GET index/{userid}/?query=...&username=
        // SearchResultAccumulator queryBackup(String query, String source, String type, String job, String username);

        HttpGet method = new HttpGet(HOST + PORT + "/index/" + USER + "?query=find_me&username=peter");

        HttpResponse response = client.execute(method);

        assertStatusCode(200, response);

        HttpEntity entity = response.getEntity();
        String body = IOUtils.toString(entity.getContent());
        System.out.println(body);
        assertTrue(body.indexOf("\"byJob\":[{\"title\":\"first Job\",\"count\":1},{\"title\":\"next Job\",\"count\":1}]") >= 0);
        assertTrue(body.indexOf("\"byType\":[{\"title\":\"Type\",\"count\":3}]") >= 0);
        assertTrue(body.indexOf("\"bySource\":[{\"title\":\"Dropbox\",\"count\":2},{\"title\":\"Facebook\",\"count\":2}]") >= 0);
        assertTrue(body.indexOf("\"files\":[{\"fileId\":\"fileId\",") >= 0);
    }

    private void assertStatusCode(int expectedStatus, HttpResponse response) {
        int responseCode = response.getStatusLine().getStatusCode();
        assertEquals(expectedStatus, responseCode);
    }
}

//index/{userid}/
//
//GET
//index/{userid}/files/?job=id
//    Set<FileItem> searchAllFileItemsForJob(Long jobId);
//
//GET
//index/{userid}/files/{fileid}/info
//    FileInfo getFileInfoForFile(String fileId);
//
//GET
//index/{userid}/files/{fileid}/thumbnail
//    String getThumbnailPathForFile(String fileId);
//
//DELETE
//    void deleteRecordsForUser();
//
//DELETE
//index/{userid}/?job=id&timestamp=tsmp
//    void deleteRecordsForJobAndTimestamp(Long jobId, Long timestamp);
//
//PUT/POST
//index/
//    void index(IndexDocument document) throws IOException;
//
