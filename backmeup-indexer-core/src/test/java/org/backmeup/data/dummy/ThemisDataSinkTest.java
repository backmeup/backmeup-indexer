package org.backmeup.data.dummy;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.backmeup.data.dummy.ThemisDataSink.IndexFragmentType;
import org.backmeup.index.config.Configuration;
import org.backmeup.index.model.IndexDocument;
import org.backmeup.index.model.IndexFields;
import org.backmeup.index.utils.file.JsonSerializer;
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

	@Rule
	public ExpectedException exception = ExpectedException.none();

	@After
	public void after() {
		try {
			ThemisDataSink.deleteIndexTrueCryptContainer(99998);
		} catch (IOException e) {
		}
		try {
			ThemisDataSink.deleteAllIndexFragments(99997,
					IndexFragmentType.TO_IMPORT_USER_OWNED);
		} catch (IOException e) {
		}
	}

	@AfterClass
	public static void afterclass() {
		try {
			ThemisDataSink.deleteIndexTrueCryptContainer(99998);
		} catch (IOException e) {
		}
		try {
			ThemisDataSink.deleteIndexTrueCryptContainer(99999);
		} catch (IOException e) {
		}
		try {
			ThemisDataSink.deleteAllIndexFragments(99998,
					IndexFragmentType.TO_IMPORT_USER_OWNED);
		} catch (IOException e) {
		}
	}

	@Before
	public void before() throws IOException {
		this.tcTemplateFile = new File(
				"src/main/resources/elasticsearch_userdata_template_TC_150MB.tc");
		// for Truecrypt container tests
		ThemisDataSink.saveIndexTrueCryptContainer(this.tcTemplateFile,
				99998);

		// for indexFragment tests
		File fIndexDocument = new File(
				"src/main/resources/tests/sampleIndexDocument.serindexdocument");
		String sampleFragment = FileUtils.readFileToString(fIndexDocument,
				"UTF-8");
		this.indexDoc = JsonSerializer.deserialize(sampleFragment,
				IndexDocument.class);
	}

	public File tcTemplateFile;
	public IndexDocument indexDoc;

	@Test
	public void testHomeDirSet() {
		String eshome = Configuration.getProperty("themis-datasink.home.dir");
		Assert.assertNotNull(eshome);
	}

	@Test
	public void testStoreIndexTCContainerFileForUser() throws IOException {
		File templateFile = new File(
				"src/main/resources/elasticsearch_userdata_template_TC_150MB.tc");

		ThemisDataSink.saveIndexTrueCryptContainer(templateFile, 99999);
	}

	@Test
	public void testGetIndexTCContainerFileForUser() {

		File templateFile = new File(
				"src/main/resources/elasticsearch_userdata_template_TC_150MB.tc");
		try {
			ThemisDataSink.saveIndexTrueCryptContainer(templateFile, 99999);
		} catch (IOException e) {
			Assert.fail("Should not reach this part of the testcase " + e);
		}

	}

	@Test
	public void testDeleteIndexTCContainerFileForUser() {
		try {
			ThemisDataSink.deleteIndexTrueCryptContainer(99998);
		} catch (IOException e) {
			Assert.fail("Should not reach this part of the testcase ");
		}
		try {
			ThemisDataSink.getIndexTrueCryptContainer(99998);

			Assert.fail("Should not be able to fetch a index truecrypt container file for this user");
		} catch (IOException e) {
			Assert.assertTrue(
					"Index Truecrypt container file for user properly deleted",
					true);
		}
	}

	@Test
	public void testPersistLoadAndDeleteIndexFragmentForUser() {

		try {
			UUID fileID = ThemisDataSink.saveIndexFragment(this.indexDoc,
					99998, IndexFragmentType.TO_IMPORT_USER_OWNED);
			Assert.assertNotNull(fileID);

			List<UUID> lUUIDs = ThemisDataSink.getAllIndexFragmentUUIDs(99998,
					IndexFragmentType.TO_IMPORT_USER_OWNED);
			Assert.assertNotNull(lUUIDs);

			Assert.assertTrue(lUUIDs.contains(fileID));

			IndexDocument fragment = ThemisDataSink.getIndexFragment(fileID,
					99998, IndexFragmentType.TO_IMPORT_USER_OWNED);
			Assert.assertNotNull(fragment);

			ThemisDataSink.deleteIndexFragment(fileID, 99998,
					IndexFragmentType.TO_IMPORT_USER_OWNED);

		} catch (IOException e) {
			Assert.fail("failed saving, getting or deleting indexFragment: "
					+ e.toString());
		}
	}

	@Test
	public void testAddUUIDtoRecordWhenPersisting() {

		try {
			Assert.assertFalse(
					"Document should not contain an UUID yet",
					this.indexDoc.getFields().containsKey(
							IndexFields.FIELD_INDEX_UUID));
			UUID fileID = ThemisDataSink.saveIndexFragment(this.indexDoc,
					99997, IndexFragmentType.TO_IMPORT_USER_OWNED);

			IndexDocument fragment = ThemisDataSink.getIndexFragment(fileID,
					99997, IndexFragmentType.TO_IMPORT_USER_OWNED);
			Assert.assertNotNull("The returned IndexDocument must not be null",
					fragment);
			Assert.assertTrue(
					"The UUID should be written within the object",
					fragment.getFields().containsKey(
							IndexFields.FIELD_INDEX_UUID));

		} catch (IOException e) {
			Assert.fail("failed saving, getting or deleting indexFragment: "
					+ e.toString());
		}
	}

	@Test
	public void testDeleteAndMoveFragment() throws IOException {
		UUID fileID = null;
		fileID = ThemisDataSink.saveIndexFragment(this.indexDoc, 99997,
				IndexFragmentType.TO_IMPORT_USER_OWNED);

		ThemisDataSink.deleteIndexFragment(fileID, 99997,
				IndexFragmentType.TO_IMPORT_USER_OWNED);

		this.exception.expect(IOException.class);
		IndexDocument fragment = ThemisDataSink.getIndexFragment(fileID, 99997,
				IndexFragmentType.TO_IMPORT_USER_OWNED);

	}

}
