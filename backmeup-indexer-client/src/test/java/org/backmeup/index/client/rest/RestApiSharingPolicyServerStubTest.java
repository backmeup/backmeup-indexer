package org.backmeup.index.client.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.backmeup.index.model.User;
import org.backmeup.index.model.sharing.SharingPolicyEntry;
import org.junit.Rule;
import org.junit.Test;

public class RestApiSharingPolicyServerStubTest {

    @Rule
    public final EmbeddedJdkServer server = new EmbeddedJdkServer();

    @Test
    public void getAllOwnedPolicies() {
        this.server.setStatusCode(200);
        this.server.setResourceFileName("SharingPolicyEntries.json");

        Set<SharingPolicyEntry> sharingEntry = new RestApiSharingPolicyServerStub(testConfig())
                .getAllOwnedSharingPolicies(new User(1L));
        assertNotNull(sharingEntry);
        assertEquals(1, sharingEntry.size());
        assertTrue(sharingEntry.iterator().next().getWithUserID() == 2);
    }

    private RestApiConfig testConfig() {
        return new RestApiConfig("127.0.0.1", 7654, "");
    }

}
