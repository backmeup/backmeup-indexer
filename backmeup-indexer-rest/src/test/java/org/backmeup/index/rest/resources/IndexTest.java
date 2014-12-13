package org.backmeup.index.rest.resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.backmeup.index.api.IndexClient;
import org.backmeup.index.model.CountedEntry;
import org.backmeup.index.model.IndexDocument;
import org.backmeup.index.model.SearchEntry;
import org.backmeup.index.model.SearchResultAccumulator;
import org.backmeup.index.model.User;
import org.backmeup.index.query.ElasticSearchSetup;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.internal.util.reflection.Whitebox;

public class IndexTest {

    private static final Long USER = 1L;

    @Rule
    public final EmbeddedRestServer server = new EmbeddedRestServer(IndexWithMockedFactory.class);

    private final String baseUrl = server.host + server.port + "/index/" + USER;
    private final HttpClient httpClient = HttpClientBuilder.create().build();

    private static IndexClient indexClient;

    public static class IndexWithMockedFactory extends Index {
        public IndexWithMockedFactory() {
            indexClient = mock(IndexClient.class);
            when(indexClient.queryBackup("find_me", null, null, null, "peter")).thenReturn(oneFile());

            ElasticSearchSetup clientFactory = mock(ElasticSearchSetup.class);
            when(clientFactory.createIndexClient(new User(USER))).thenReturn(indexClient);
            
            // Note: this depends on private field clientFactory in Index class.
            Whitebox.setInternalState(this, "clientFactory", clientFactory);
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
        HttpGet method = new HttpGet(baseUrl + "?query=find_me&username=peter");

        HttpResponse response = httpClient.execute(method);

        assertStatusCode(200, response);

        HttpEntity entity = response.getEntity();
        String body = IOUtils.toString(entity.getContent());
        // System.out.println(body);
        assertTrue(body.indexOf("\"byJob\":[{\"title\":\"first Job\",\"count\":1},{\"title\":\"next Job\",\"count\":1}]") >= 0);
        assertTrue(body.indexOf("\"byType\":[{\"title\":\"Type\",\"count\":3}]") >= 0);
        assertTrue(body.indexOf("\"bySource\":[{\"title\":\"Dropbox\",\"count\":2},{\"title\":\"Facebook\",\"count\":2}]") >= 0);
        assertTrue(body.indexOf("\"files\":[{\"fileId\":\"fileId\",") >= 0);
    }

    @Test
    public void shouldGetBadRequestForMissingQuery() throws IOException {
        HttpGet method = new HttpGet(baseUrl + "?username=peter");

        HttpResponse response = httpClient.execute(method);

        assertStatusCode(400, response);
    }

    @Test
    public void shouldIndexDocument() throws IOException {
        HttpPost method = new HttpPost(baseUrl);

        String jsonDocument = "{\"fields\":{ \"name\":\"Peter\", \"size\":42}, \"largeFields\":{}}";
        method.setEntity(new StringEntity(jsonDocument, ContentType.APPLICATION_JSON));
        HttpResponse response = httpClient.execute(method);

        assertStatusCode(201, response);

        ArgumentCaptor<IndexDocument> argumentCaptor = ArgumentCaptor.forClass(IndexDocument.class);
        verify(indexClient).index(argumentCaptor.capture());
        IndexDocument document = argumentCaptor.getValue();
        assertEquals("Peter", document.getFields().get("name"));
        assertEquals(Integer.valueOf(42), document.getFields().get("size"));
    }

    private void assertStatusCode(int expectedStatus, HttpResponse response) {
        int responseCode = response.getStatusLine().getStatusCode();
        assertEquals(expectedStatus, responseCode);
    }
}
