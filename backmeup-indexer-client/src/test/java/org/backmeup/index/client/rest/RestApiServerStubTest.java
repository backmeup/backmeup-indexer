package org.backmeup.index.client.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.backmeup.index.model.SearchResultAccumulator;
import org.backmeup.index.model.User;
import org.junit.Rule;
import org.junit.Test;

public class RestApiServerStubTest {

    @Rule
    public final EmbeddedJdkServer server = new EmbeddedJdkServer();

    @Test
    public void testQueryBackup() {
        this.server.setStatusCode(200);
        this.server.setResourceFileName("searchresult.json");

        SearchResultAccumulator searchResult = new RestApiIndexServerStub(testConfig()).query(new User(1L), "find_me",
                null, null, "", null, null, "peter", null, null);
        assertNotNull(searchResult);
        assertEquals(2, searchResult.getBySource().size());
    }

    private RestApiConfig testConfig() {
        return new RestApiConfig("127.0.0.1", 7654, "");
    }

}
