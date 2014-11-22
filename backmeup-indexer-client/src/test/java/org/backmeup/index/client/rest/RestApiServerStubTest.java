package org.backmeup.index.client.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.backmeup.index.model.SearchResultAccumulator;
import org.junit.Rule;
import org.junit.Test;

public class RestApiServerStubTest {

    @Rule
    public final EmbeddedJdkServer server = new EmbeddedJdkServer();

    @Test
    public void testQueryBackup() {
        server.setStatusCode(200);
        server.setResourceFileName("query.json");

        SearchResultAccumulator searchResult = new RestApiServerStub().query(1L, "find_me", null, null, "", "peter");
        assertNotNull(searchResult);
        assertEquals(2, searchResult.getBySource().size());
    }

    @Test
    public void testSearchAllFileItemsForJob() {
    }

    @Test
    public void testGetFileInfoForFile() {
    }

    @Test
    public void testGetThumbnailPathForFile() {
    }

    @Test
    public void testDeleteRecordsForUser() {
    }

    @Test
    public void testDeleteRecordsForJobAndTimestamp() {
    }

    @Test
    public void testIndex() {
    }
}
