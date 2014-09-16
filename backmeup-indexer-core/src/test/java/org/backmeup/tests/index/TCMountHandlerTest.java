package org.backmeup.tests.index;

import java.io.File;
import java.io.IOException;

import org.backmeup.index.TCMountHandler;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class TCMountHandlerTest {

	// The Password for the TestTCVol1.tc file is 12345

	@Before
	public void unmountAllDrives() {
		try {
			TCMountHandler.unmountAll();
		} catch (IOException e) {
			System.out.println(e);
		} catch (InterruptedException e) {
			System.out.println(e);
		}
	}

	@Test
	public void truecryptAvailable() {
		// how to get the current working dir:
		// System.out.println(System.getProperty("user.dir"));
		boolean b = TCMountHandler.checkTrueCryptAvailable();
		Assert.assertEquals(
				"check TrueCrypt available: TC not available or properly configured on the system",
				true, b);
	}

	@Test
	public void mountAndUnmountTestContainer() {
		File tcTestFile = new File("src/main/resources/tests/TestTCVol1.tc");
		try {
			TCMountHandler.mount(tcTestFile, "12345", "I");
			Assert.assertEquals("TrueCrypt Testvolume did not get mounted",
					true, TCMountHandler.isDriveMounted("I"));
			TCMountHandler.unmount("I");
			Assert.assertEquals(
					"TrueCrypt Testvolume did not get unmounted properly",
					false, TCMountHandler.isDriveMounted("I"));
		} catch (Exception e) {
			// should not happen in this test case
			Assert.fail("should never fail mounting this scenario");
		}
	}

	@Test
	public void mountVolumeTwiceAtSameTime() {
		// mounting the same TrueCrypt Volume file twice at the same time is not
		// possible
		File tcTestFile = new File("src/main/resources/tests/TestTCVol1.tc");
		try {
			TCMountHandler.mount(tcTestFile, "12345", "I");
			Assert.assertEquals("TrueCrypt Testvolume did not get mounted",
					true, TCMountHandler.isDriveMounted("I"));

			TCMountHandler.mount(tcTestFile, "12345", "J");
			Assert.fail("IOException should have been thronw. TestFile "
					+ tcTestFile.getAbsolutePath());

		} catch (IOException e) {
			// in this case we should have gotten an IOException and no other
			Assert.assertTrue(true);
		} catch (Exception e) {
			// should not happen in this test case
			Assert.fail("should never fail mounting this scenario");
		}
	}

	@Test
	public void mountTwiceOnSameDrive() {
		File tcTestFile = new File("src/main/resources/tests/TestTCVol1.tc");
		File tcTestFile2 = new File("src/main/resources/tests/TestTCVol2.tc");
		try {
			String d1 = "I";
			TCMountHandler.mount(tcTestFile, "12345", d1);
			Assert.assertEquals("TrueCrypt Testvolume did not get mounted",
					true, TCMountHandler.isDriveMounted(d1));

			String drive = TCMountHandler.mount(tcTestFile2, "12345", d1);
			Assert.assertFalse(
					"Volume can't get mounted twice on the same drive",
					d1.equals(drive));
			Assert.assertEquals("TrueCrypt Testvolume did not get mounted",
					true, TCMountHandler.isDriveMounted(drive));
		} catch (Exception e) {
			// should not happen in this test case
			Assert.fail("should never fail mounting this scenario");
		}
	}

	@Test
	@Ignore
	public void mountUnsupportedDrive() {
		Assert.fail("need to write this testcase");

	}

}
