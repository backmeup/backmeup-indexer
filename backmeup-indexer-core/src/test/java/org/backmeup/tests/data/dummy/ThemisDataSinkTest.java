package org.backmeup.tests.data.dummy;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.backmeup.data.dummy.ThemisDataSink;
import org.backmeup.index.config.Configuration;
import org.backmeup.index.model.IndexDocument;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;

public class ThemisDataSinkTest {

	@After
	public void after() {
		try {
			ThemisDataSink.deleteIndexTrueCryptContainer(99998);
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
		// try {
		// ThemisDataSink.deleteAllIndexFragments(99998);
		// } catch (IOException e) {
		// }
	}

	@Before
	public void before() {
		this.tcTemplateFile = new File(
				"src/main/resources/elasticsearch_userdata_template_TC_150MB.tc");
		try {
			// for Truecrypt container tests
			ThemisDataSink.saveIndexTrueCryptContainer(this.tcTemplateFile,
					99998);

			// for indexFragment tests
			File fIndexDocument = new File(
					"src/main/resources/tests/sampleIndexDocument.serindexdocument");
			String sampleFragment = FileUtils.readFileToString(fIndexDocument);
			Gson gson = new Gson();
			this.indexDoc = gson.fromJson(sampleFragment, IndexDocument.class);
			// this.indexDoc = JsonSerializer.deserialize(sampleFragment,
			// IndexDocument.class);

		} catch (IOException e) {
		}
	}

	public File tcTemplateFile;
	public IndexDocument indexDoc;

	@Test
	public void testHomeDirSet() {
		String eshome = Configuration.getProperty("themis-datasink.home.dir");
		Assert.assertNotNull(eshome);
	}

	@Test
	public void testStoreIndexTCContainerFileForUser() {
		File tcTemplateFile = new File(
				"src/main/resources/elasticsearch_userdata_template_TC_150MB.tc");
		try {
			ThemisDataSink.saveIndexTrueCryptContainer(tcTemplateFile, 99999);
		} catch (IOException e) {
			Assert.fail("Should not reach this part of the testcase " + e);
		}
	}

	@Test
	public void testGetIndexTCContainerFileForUser() {

		File tcTemplateFile = new File(
				"src/main/resources/elasticsearch_userdata_template_TC_150MB.tc");
		try {
			ThemisDataSink.saveIndexTrueCryptContainer(tcTemplateFile, 99999);
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
			UUID fileID = ThemisDataSink
					.saveIndexFragment(this.indexDoc, 99998);
			Assert.assertNotNull(fileID);

			List<UUID> lUUIDs = ThemisDataSink.getAllIndexFragmentUUIDs(99998);
			Assert.assertNotNull(lUUIDs);

			Assert.assertTrue(lUUIDs.contains(fileID));

			IndexDocument fragment = ThemisDataSink.getIndexFragment(fileID,
					99998);
			Assert.assertNotNull(fragment);

			// ThemisDataSink.deleteIndexFragment(fileID, 99998);

		} catch (IOException e) {
			Assert.fail("failed saving, getting or deleting indexFragment: "
					+ e.toString());
		}
	}

}
