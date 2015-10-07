package org.backmeup.index.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.backmeup.index.api.IndexFields;
import org.backmeup.index.model.IndexDocument;
import org.backmeup.index.model.User;
import org.backmeup.index.serializer.Json;
import org.backmeup.index.storage.ThemisDataSink.IndexFragmentType;
import org.backmeup.keyserver.crypto.AsymmetricEncryptionProvider;
import org.backmeup.keyserver.crypto.impl.RSAEncryptionProvider;
import org.backmeup.keyserver.fileencryption.EncryptionInputStream;
import org.backmeup.keyserver.fileencryption.EncryptionOutputStream;
import org.backmeup.keyserver.model.CryptoException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ThemisDataSinkIndexFragmentEncryptionTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private static final int RSA_KEY_LENGTH = 2048;
    private static final String TEST_MSG = "this is a secret test message";
    private static final String INDEXFRAGMENT = "indexDocWithTikaMetadataAndUUID.serindexdocument";
    private static final User USER1_99997L = new User(99997L);
    private IndexDocument INDEXDOC;
    private KeyServerDummy ks;
    private File testFileIndexFragmentEncrypted;
    private EncryptionOutputStream out;

    private void createEncryptedTestfile() throws IOException {
        this.testFileIndexFragmentEncrypted = File.createTempFile("themis_IndexFragmentEncrypted", ".dat");
        this.out = new EncryptionOutputStream(this.testFileIndexFragmentEncrypted, USER1_99997L.id().toString(), this.ks.getPublicKey());
        this.out.write(TEST_MSG.getBytes());
        this.out.close();
    }

    @Test
    public void encryptFile() throws IOException {
        createEncryptedTestfile();
        assertEquals(1, this.out.getKeystore().listReceivers().size());
        assertTrue(this.out.getKeystore().hasReceiver(USER1_99997L.id().toString()));
        assertNotEquals(TEST_MSG, Files.readAllBytes(this.testFileIndexFragmentEncrypted.toPath()));
    }

    private String readEncryptedTestFile() throws IOException {
        EncryptionInputStream ein = new EncryptionInputStream(this.testFileIndexFragmentEncrypted, USER1_99997L.id().toString(),
                this.ks.getPrivateKey());
        byte[] block = new byte[8];
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int read = 0;
        while ((read = ein.read(block)) != -1) {
            buffer.write(block, 0, read);
        }
        ein.close();

        return buffer.toString();
    }

    @Test
    public void decryptFile() throws IOException {
        createEncryptedTestfile();

        String message = readEncryptedTestFile();
        assertEquals(TEST_MSG, message);
    }

    public void saveAndFetchEncryptedIndexDocument() throws IOException {

        //TODO implement functionality for EncryptedThemisDatasink
    }

    @Test
    public void testPersistLoadAndDeleteIndexFragmentForUser() throws IOException {
        UUID fileID = ThemisDataSink.saveIndexFragment(this.INDEXDOC, USER1_99997L, IndexFragmentType.TO_IMPORT_USER_OWNED,
                this.ks.getPublicKey());
        assertEquals(fileID.toString(), this.INDEXDOC.getFields().get(IndexFields.FIELD_INDEX_DOCUMENT_UUID));

        List<UUID> lUUIDs = ThemisDataSink.getAllIndexFragmentUUIDs(USER1_99997L, IndexFragmentType.TO_IMPORT_USER_OWNED);
        assertNotNull(lUUIDs);
        assertTrue(lUUIDs.contains(fileID));

        IndexDocument fragment = ThemisDataSink.getIndexFragment(fileID, USER1_99997L, IndexFragmentType.TO_IMPORT_USER_OWNED,
                this.ks.getPrivateKey());
        assertNotNull(fragment);

        ThemisDataSink.deleteIndexFragment(fileID, USER1_99997L, IndexFragmentType.TO_IMPORT_USER_OWNED);
        lUUIDs = ThemisDataSink.getAllIndexFragmentUUIDs(USER1_99997L, IndexFragmentType.TO_IMPORT_USER_OWNED);
        assertNotNull(lUUIDs);
        assertTrue(!lUUIDs.contains(fileID));
    }

    private IndexDocument loadSampleIndexDocument() throws IOException {
        return deserialize(load());
    }

    private IndexDocument deserialize(String json) {
        return Json.deserialize(json, IndexDocument.class);
    }

    private String load() throws IOException {
        try (InputStream resource = getClass().getClassLoader().getResourceAsStream(INDEXFRAGMENT)) {
            return IOUtils.toString(resource);
        }
    }

    @Before
    public void before() {
        try {
            this.ks = new KeyServerDummy();
        } catch (CryptoException e) {
        }
        try {
            this.INDEXDOC = loadSampleIndexDocument();
        } catch (IOException e) {
        }
    }

    @After
    public void after() {
        try {
            if (this.out != null) {
                this.out.close();
            }
        } catch (Exception e) {
        }
        try {
            if (this.testFileIndexFragmentEncrypted != null) {
                this.testFileIndexFragmentEncrypted.delete();
            }
        } catch (Exception e) {
        }
        try {
            ThemisDataSink.deleteAllIndexFragments(USER1_99997L, IndexFragmentType.TO_IMPORT_USER_OWNED);
        } catch (IOException e) {
        }
    }

    @AfterClass
    public static void afterclass() {
        try {
            ThemisDataSink.deleteDataSinkHome(USER1_99997L);
        } catch (IllegalArgumentException e) {
        }
    }

    private class KeyServerDummy {
        public KeyPair kp;

        public KeyServerDummy() throws CryptoException {
            AsymmetricEncryptionProvider asymmetricEncryption = new RSAEncryptionProvider();
            this.kp = asymmetricEncryption.generateKey(RSA_KEY_LENGTH);
        }

        public PublicKey getPublicKey() {
            return this.kp.getPublic();
        }

        public PrivateKey getPrivateKey() {
            return this.kp.getPrivate();
        }
    }

}
