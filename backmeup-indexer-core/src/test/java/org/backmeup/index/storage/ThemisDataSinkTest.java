package org.backmeup.index.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.backmeup.index.api.IndexFields;
import org.backmeup.index.model.IndexDocument;
import org.backmeup.index.model.User;
import org.backmeup.index.serializer.Json;
import org.backmeup.index.storage.ThemisDataSink.IndexFragmentType;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ThemisDataSinkTest {

    private static final User _99999L = new User(99999L);
    private static final User _99998L = new User(99998L);
    private static final User _99997L = new User(99997L);

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @After
    public void after() {
        try {
            ThemisDataSink.deleteIndexTrueCryptContainer(_99998L);
        } catch (IOException e) {
        }
        try {
            ThemisDataSink.deleteAllIndexFragments(_99997L, IndexFragmentType.TO_IMPORT_USER_OWNED);
        } catch (IOException e) {
        }
    }

    @AfterClass
    public static void afterclass() {
        try {
            ThemisDataSink.deleteDataSinkHome(_99997L);
        } catch (IllegalArgumentException e) {
        }
        try {
            ThemisDataSink.deleteDataSinkHome(_99998L);
        } catch (IllegalArgumentException e) {
        }
        try {
            ThemisDataSink.deleteDataSinkHome(_99999L);
        } catch (IllegalArgumentException e) {
        }
    }

    @Before
    public void before() throws IOException {
        this.tcTemplateFile = new File("src/main/resources/elasticsearch_userdata_template_TC_150MB.tc");
        // for Truecrypt container tests
        ThemisDataSink.saveIndexTrueCryptContainer(this.tcTemplateFile, _99998L);

        // for indexFragment tests
        File fIndexDocument = new File("src/test/resources/sampleIndexDocument.serindexdocument");
        String sampleFragment = FileUtils.readFileToString(fIndexDocument, "UTF-8");
        this.indexDoc = Json.deserialize(sampleFragment, IndexDocument.class);
    }

    public File tcTemplateFile;
    public IndexDocument indexDoc;

    @Test
    public void testStoreIndexTCContainerFileForUser() throws IOException {
        File templateFile = new File("src/main/resources/elasticsearch_userdata_template_TC_150MB.tc");

        ThemisDataSink.saveIndexTrueCryptContainer(templateFile, _99999L);
    }

    @Test
    public void testGetIndexTCContainerFileForUser() {

        File templateFile = new File("src/main/resources/elasticsearch_userdata_template_TC_150MB.tc");
        try {
            ThemisDataSink.saveIndexTrueCryptContainer(templateFile, _99999L);
        } catch (IOException e) {
            Assert.fail("Should not reach this part of the testcase " + e);
        }

    }

    @Test
    public void testDeleteIndexTCContainerFileForUser() {
        try {
            ThemisDataSink.deleteIndexTrueCryptContainer(_99998L);
        } catch (IOException e) {
            Assert.fail("Should not reach this part of the testcase ");
        }
        try {
            ThemisDataSink.getIndexTrueCryptContainer(_99998L);

            Assert.fail("Should not be able to fetch a index truecrypt container file for this user");
        } catch (IOException e) {
            Assert.assertTrue("Index Truecrypt container file for user properly deleted", true);
        }
    }

    @Test
    public void testPersistLoadAndDeleteIndexFragmentForUser() throws IOException {
        UUID testUUID = UUID.randomUUID();
        this.indexDoc.field(IndexFields.FIELD_INDEX_DOCUMENT_UUID, testUUID.toString());

        UUID fileID = ThemisDataSink.saveIndexFragment(this.indexDoc, _99998L, IndexFragmentType.TO_IMPORT_USER_OWNED);
        assertEquals(fileID, testUUID);

        List<UUID> lUUIDs = ThemisDataSink.getAllIndexFragmentUUIDs(_99998L, IndexFragmentType.TO_IMPORT_USER_OWNED);
        assertNotNull(lUUIDs);

        assertTrue(lUUIDs.contains(fileID));

        IndexDocument fragment = ThemisDataSink.getIndexFragment(fileID, _99998L, IndexFragmentType.TO_IMPORT_USER_OWNED);
        assertNotNull(fragment);

        ThemisDataSink.deleteIndexFragment(fileID, _99998L, IndexFragmentType.TO_IMPORT_USER_OWNED);
    }

    @Test
    public void testNoUUIDInRecordWhenPersisting() throws IOException {
        assertFalse("Document should not contain an UUID yet", this.indexDoc.getFields().containsKey(IndexFields.FIELD_INDEX_DOCUMENT_UUID));

        this.exception.expect(IOException.class); //Expecting exception when saving: IndexFragment must have a document UUID assigned
        ThemisDataSink.saveIndexFragment(this.indexDoc, _99997L, IndexFragmentType.TO_IMPORT_USER_OWNED);

        UUID testUUID = UUID.randomUUID();
        this.indexDoc.field(IndexFields.FIELD_INDEX_DOCUMENT_UUID, testUUID.toString());
        UUID fileID = ThemisDataSink.saveIndexFragment(this.indexDoc, _99997L, IndexFragmentType.TO_IMPORT_USER_OWNED);
        IndexDocument fragment = ThemisDataSink.getIndexFragment(fileID, _99997L, IndexFragmentType.TO_IMPORT_USER_OWNED);
        assertNotNull("The returned IndexDocument must not be null", fragment);
        assertTrue("The UUID should be written within the object", fragment.getFields().containsKey(IndexFields.FIELD_INDEX_DOCUMENT_UUID));
    }

    @Test
    public void testDeleteAndMoveFragment() throws IOException {
        UUID testUUID = UUID.randomUUID();
        this.indexDoc.field(IndexFields.FIELD_INDEX_DOCUMENT_UUID, testUUID.toString());
        testUUID = ThemisDataSink.saveIndexFragment(this.indexDoc, _99997L, IndexFragmentType.TO_IMPORT_USER_OWNED);

        ThemisDataSink.deleteIndexFragment(testUUID, _99997L, IndexFragmentType.TO_IMPORT_USER_OWNED);

        this.exception.expect(IOException.class);
        ThemisDataSink.getIndexFragment(testUUID, _99997L, IndexFragmentType.TO_IMPORT_USER_OWNED);
    }

}
