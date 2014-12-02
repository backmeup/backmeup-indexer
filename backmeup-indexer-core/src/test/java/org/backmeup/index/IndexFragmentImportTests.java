package org.backmeup.index;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.backmeup.index.api.IndexFields;
import org.backmeup.index.model.IndexDocument;
import org.backmeup.index.serializer.JsonSerializer;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class IndexFragmentImportTests extends IndexManagerSetup {

    //TODO Datei händisch im Workspace anlegen oder mit UnitTest hin kopieren 

    @Test
    @Ignore
    public void importUserOwnedIndexFragmentsInES() {
        /*this.indexManager.startupInstance(999991);
        System.out.println("startup done");

        RunningIndexUserConfig conf = this.indexManager.getRunningIndexUserConfig(999991);

        int httpPort = conf.getHttpPort();
        URL host = conf.getHostAddress();
        Assert.assertTrue("ES instance up and running?",
                ESConfigurationHandler.isElasticSearchInstanceRunning(host, httpPort));*/
    }

    @Test
    public void serializationOfIndexDocument() throws IOException {

        IndexDocument document = deserialize();
        assertEquals(16384L, document.getFields().get(IndexFields.FIELD_OWNER_ID));
    }

    private IndexDocument deserialize() throws IOException {
        try (InputStream resource = getClass().getClassLoader().getResourceAsStream(
                "sampleIndexDocument.serindexdocument")) {
            String json = IOUtils.toString(resource);
            return JsonSerializer.deserialize(json, IndexDocument.class);
            // TODO PK deserialized does not look too good, has double instead of long, date correct?
        }
    }

}
