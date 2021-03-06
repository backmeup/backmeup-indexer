package org.backmeup.index.core.truecrypt;

import java.io.File;
import java.io.IOException;

import org.apache.commons.lang.SystemUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class TCMountHandlerTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();
    private File tempFile = null;

    // The Password for the TestTCVol1.tc file is 12345

    @Before
    @After
    public void testSetup() {
        unmountAllDrives();
        cleanupTempFiles();
    }

    @Test
    public void truecryptAvailable() {
        // how to get the current working dir:
        // System.out.println(System.getProperty("user.dir"));
        boolean b = TCMountHandler.checkTrueCryptAvailable();
        Assert.assertEquals("check TrueCrypt available: TC not available or properly configured on the system", true, b);
    }

    @Test
    public void mountAndUnmountTestContainerWindows() throws IOException, InterruptedException {
        Assume.assumeTrue(SystemUtils.IS_OS_WINDOWS);
        File tcTestFile = new File("src/test/resources/tests/TestTCVol1.tc");

        String drive = TCMountHandler.mount(tcTestFile, "12345", "J");
        Assert.assertEquals("TrueCrypt Testvolume did not get mounted", true, TCMountHandler.isDriveMounted(drive));
        TCMountHandler.unmount(drive);
        Assert.assertEquals("TrueCrypt Testvolume did not get unmounted properly", false, TCMountHandler.isDriveMounted(drive));

    }

    @Test
    public void mountAndUnmountTestContainerLinux() {
        Assume.assumeTrue(SystemUtils.IS_OS_LINUX);
        File tcTestFile = new File("src/test/resources/tests/TestTCVol1.tc");
        try {
            String mountingPoint = TCMountHandler.mount(tcTestFile, "12345", "/media/themis/volume0");
            System.out.println(mountingPoint);

            Assert.assertEquals("TrueCrypt Testvolume did not get mounted", true, TCMountHandler.isDriveMounted(mountingPoint));

            TCMountHandler.unmount(mountingPoint);
            Assert.assertEquals("TrueCrypt Testvolume did not get unmounted properly", false, TCMountHandler.isDriveMounted(mountingPoint));

        } catch (Exception e) {
            System.out.println(e.toString());
            // should not happen in this test case
            Assert.fail("should never fail mounting this scenario." + e.toString());
        }
    }

    @Test
    public void mountVolumeTwiceAtSameTimeLinux() {
        Assume.assumeTrue(SystemUtils.IS_OS_LINUX);
        // mounting the same TrueCrypt Volume file twice at the same time is not
        // possible
        File tcTestFile = new File("src/test/resources/tests/TestTCVol1.tc");
        try {
            String mountingPoint = TCMountHandler.mount(tcTestFile, "12345", "/media/themis/volume0");

            Assert.assertEquals("TrueCrypt Testvolume did not get mounted", true, TCMountHandler.isDriveMounted(mountingPoint));

            TCMountHandler.mount(tcTestFile, "12345", mountingPoint);
            Assert.fail("IOException should have been thronw. TestFile " + tcTestFile.getAbsolutePath());

        } catch (IOException e) {
            // in this case we should have gotten an IOException and no other
            Assert.assertTrue(true);
        } catch (Exception e) {
            // should not happen in this test case
            Assert.fail("should never fail mounting this scenario");
        }
    }

    @Test
    public void mountVolumeTwiceAtSameTimeWindows() throws IOException, InterruptedException {
        Assume.assumeTrue(SystemUtils.IS_OS_WINDOWS);
        // mounting the same TrueCrypt Volume file twice at the same time is not
        // possible
        File tcTestFile = new File("src/test/resources/tests/TestTCVol1.tc");
        String drive1 = "J";
        drive1 = TCMountHandler.mount(tcTestFile, "12345", drive1);
        Assert.assertEquals("TrueCrypt Testvolume did not get mounted", true, TCMountHandler.isDriveMounted(drive1));

        String drive2 = "K";
        this.thrown.expect(IOException.class); //expecting IOException to be thrown by mount
        TCMountHandler.mount(tcTestFile, "12345", drive2);
        Assert.fail("IOException should have been thrown. TestFile " + tcTestFile.getAbsolutePath());
    }

    @Test
    public void mountTwiceOnSameDrive() throws IOException, InterruptedException {
        Assume.assumeTrue(SystemUtils.IS_OS_WINDOWS);
        File tcTestFile = new File("src/test/resources/tests/TestTCVol1.tc");
        File tcTestFile2 = new File("src/test/resources/tests/TestTCVol2.tc");
        String drive1 = "J";
        drive1 = TCMountHandler.mount(tcTestFile, "12345", drive1);
        Assert.assertEquals("TrueCrypt Testvolume did not get mounted", true, TCMountHandler.isDriveMounted(drive1));

        String drive2 = TCMountHandler.mount(tcTestFile2, "12345", drive1);
        Assert.assertFalse("Volume can't get mounted twice on the same drive", drive1.equals(drive2));
        Assert.assertEquals("TrueCrypt Testvolume did not get mounted", true, TCMountHandler.isDriveMounted(drive2));
    }

    @Test
    public void unmountAllDrivesTest() throws Exception {
        Assume.assumeTrue(SystemUtils.IS_OS_WINDOWS);
        File tcTestFile = new File("src/test/resources/tests/TestTCVol1.tc");
        File tcTestFile2 = new File("src/test/resources/tests/TestTCVol2.tc");

        String d1 = "I";
        d1 = TCMountHandler.mount(tcTestFile, "12345", d1);
        Assert.assertTrue("TrueCrypt Testvolume did not get mounted", TCMountHandler.isDriveMounted(d1));

        String d2 = "J";
        d2 = TCMountHandler.mount(tcTestFile2, "12345", d2);
        Assert.assertTrue("TrueCrypt Testvolume did not get mounted", TCMountHandler.isDriveMounted(d2));

        TCMountHandler.unmountAll();
        Assert.assertFalse("TrueCrypt Testvolume did not get mounted", TCMountHandler.isDriveMounted(d1));
        Assert.assertFalse("TrueCrypt Testvolume did not get mounted", TCMountHandler.isDriveMounted(d2));

    }

    @Test
    public void generateTCVolume() throws IOException, InterruptedException {
        Assume.assumeTrue(SystemUtils.IS_OS_LINUX);
        //try to generate a new truecrypt volume
        File tempFile = TCMountHandler.generateTrueCryptVolume(10, "ABCD");
        Assert.assertTrue(tempFile.exists());
        Assert.assertTrue(tempFile.canRead());
        String d1 = "I";
        d1 = TCMountHandler.mount(tempFile, "ABCD", d1);
        Assert.assertTrue("TrueCrypt Testvolume did not get mounted", TCMountHandler.isDriveMounted(d1));
    }

    public void unmountAllDrives() {
        try {
            TCMountHandler.unmountAll();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public void cleanupTempFiles() {
        try {
            if (this.tempFile != null) {
                this.tempFile.delete();
            }
        } catch (Exception ex) {
        }
    }

}
