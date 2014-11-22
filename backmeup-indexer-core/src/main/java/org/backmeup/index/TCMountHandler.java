package org.backmeup.index;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.SystemUtils;
import org.backmeup.index.config.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//Note: There are windows specific elements used within this class
//TODO solve issue windows restricted to 24 drive letters
//TODO add linux support 

public class TCMountHandler {

    private static final Logger log = LoggerFactory.getLogger(TCMountHandler.class);

    /**
     * @param tcVolume
     *            the truecrypt volume
     * @param password
     *            the volumes password
     * @param driveLetter
     *            suggestion where to mount the volume, if already in use the volume will get mounted on a different
     *            drive
     * @return returns the drive where the partition has been mounted
     */
    public static String mount(File tcVolume, String password, String driveLetter) throws IOException,
            InterruptedException, ExceptionInInitializerError, IllegalArgumentException {

        // 1. check if a driveLetter is given and if it's allowed according to
        // config
        boolean allowed = isAllowedDriveLetter(driveLetter);
        if (!allowed) {
            // get an allowed drive letter
            driveLetter = getSupportedDriveLetters().get(0);
        }

        // 2. check if the drive is already mounted
        boolean mounted = isDriveMounted(driveLetter);
        if (mounted) {
            boolean bFoundOne = false;
            List<String> suppDrives = getSupportedDriveLetters();
            for (String drive : suppDrives) {
                // iterate over all supported drives and check their
                // availability
                boolean m = isDriveMounted(drive);
                if ((!m) && (bFoundOne == false)) {
                    driveLetter = drive;
                    bFoundOne = true;
                }
            }

            // if we still haven't found one we have a problem - we don't have
            // any more
            if (bFoundOne == false) {
                throw new ExceptionInInitializerError("Cannot initialize mount process, no more unmounted drives left");
            }
        }

        // 3. Now mount the container with Truecrypt
        // If you want to specify a drive letter yourself use /l {drive letter}
        // instead of /a
        // @see http://andryou.com/truecrypt/docs/command-line-usage.php
        // The executed command should looks something like this
        // TrueCrypt.exe /q background /v
        // "D:/temp/themis/userspace1/index-es/TestTCVol1.tc" /a /p 12345
        // or
        // String command =
        // "/usr/bin/truecrypt /home/themis/themis-truecrypt/elasticsearch_userdata_template_TC_150MB.tc /hmedia/truecrypt3/ --password=12345";
        
        // 
        String command = null;
        if (SystemUtils.IS_OS_LINUX) {
            // creating mountpoint 
            command = "mkdir -p " + driveLetter;
            executeCmd(command);
            //    throw new IOException("Failed to create mount point: " + driveLetter);
        }
        
        command = createMountCommand(tcVolume, driveLetter, password);
        executeCmd(command);
        //    throw new IOException("Failed to mount tc voulume: " + tcVolume.getAbsolutePath());

        

        // now check if the drive got properly mounted
        if (!isDriveMounted(driveLetter)) {
            throw new IOException("Executing TrueCrypt on " + command + " did not mount the volume "
                    + tcVolume.getAbsolutePath() + " on " + driveLetter);
        }

        // finally return the drive letter which has been used to mount the
        // volume
        return driveLetter;
    }
    
    private static void executeCmd(String command) throws IOException, InterruptedException {
        try {
            // Execute the call
            log.debug("executing: " + command);
            Process process = Runtime.getRuntime().exec(command);
            Reader r = new InputStreamReader(process.getInputStream());
            BufferedReader in = new BufferedReader(r);
            String line;
            String result = "";
            
            process.waitFor();
           
            while ((line = in.readLine())!=null) result+=line;
            log.debug(result);
            // cause this process to stop until process p is terminated
            // TODO process.wait() - is an issue in headless mode, waits for
            // human interaction! get rid!

        } catch (IOException | InterruptedException e) {
            log.error("Error executing: " + command + " " + e.toString());
            throw e;
        }
    }
    
    private static String createMountCommand(File tcVolume, String driveLetter, String password) {

        String command = null;
        if (SystemUtils.IS_OS_LINUX) {
            command = /*"sudo " + */getTrueCryptExe() + " --password=" + password + " --non-interactive " + tcVolume.getAbsolutePath() + " " + driveLetter;
            //command =  getTrueCryptExe() + " " + tcVolume.getAbsolutePath() + " " + driveLetter + " --password=" + password + " --non-interactive";
        }
        if (SystemUtils.IS_OS_WINDOWS) {
            command = "\"" + getTrueCryptExe() + "\"" + " " + "/q background " + "/s " + "/v " + "\""
                    + tcVolume.getAbsolutePath() + "\" " + "/l " + driveLetter + " /p " + password;
        }

        return command;
    }

    /**
     * Checks if a given drive Letter is allowed as configured within the properties file
     * 
     * @param driveLetter
     *            without any special chars, just e.g. H or K
     */
    private static boolean isAllowedDriveLetter(String driveLetter) {
        List<String> allowed = getSupportedDriveLetters();
        if (!allowed.isEmpty()) {
            if (allowed.contains(driveLetter)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a list of all supported drive letters which have been configured to be used for mounting TrueCrypt files.
     * See backmeup.indexer.properties This does not state that a given drive or volume is already in use or not
     */
    public static List<String> getSupportedDriveLetters() {
        if (SystemUtils.IS_OS_LINUX) {
            return generateSupportedDrivesForLinux();
        }

        if (SystemUtils.IS_OS_WINDOWS) {
            return Configuration.getPropertyList("truecrypt.mountable.drives");
        }

        return new ArrayList<>();
    }

    private static List<String> generateSupportedDrivesForLinux() {
        List<String> r = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            r.add("/media/themis/volume" + i);
        }
        return r;
    }

    /**
     * Checks if a given drive exists.
     * 
     * @param driveLetter
     *            without any special chars, just e.g. H or K
     */
    public static boolean isDriveMounted(String driveLetter) throws IllegalArgumentException {

        if (driveLetter == null) {
            throw new IllegalArgumentException("provided drive " + driveLetter + "not valid");
        }

        File f = null;

        if (SystemUtils.IS_OS_WINDOWS) {
            // note: File.listRoots() to list drives is windows specific
            if (driveLetter.contains(":")) {
                f = new File(driveLetter);
            } else {
                f = new File(driveLetter + ":");
            }
        }

        if (SystemUtils.IS_OS_LINUX) {
            f = new File(driveLetter);
        }

        if ((f != null) && f.exists()) {
	    return true;
        }
        return false;

    }

    /**
     * Sends the command TrueCrypt.exe /q background /d to unmount all TrueCrypt volumes
     * 
     */
    public static void unmountAll() throws IOException, InterruptedException {
        String command = createUnmountAllCommand();
        log.debug("unmounting: " + command);

        Process p;
        try {
            p = Runtime.getRuntime().exec(command);
            p.waitFor();
        } catch (IOException | InterruptedException e) {
            log.error("Error executing: " + command + " " + e.toString());
            throw e;
        }
    }

    private static String createUnmountAllCommand() {
        String command = null;
        if (SystemUtils.IS_OS_LINUX) {
            // unmount with either mounting point or Truecrypt container file
            command = getTrueCryptExe() + " -d";

        }
        if (SystemUtils.IS_OS_WINDOWS) {
            command = getTrueCryptExe() + " " + "/q background" + " /s" + " /d" + " /f";
        }
        return command;
    }

    /**
     * 
     * @param driveLetter
     *            without any special chars, just e.g. H or K
     */
    public static void unmount(String driveLetter) throws IllegalArgumentException, ExceptionInInitializerError,
            IOException, InterruptedException {

        // check if drive is mounted, throw exception when illegal driveLetter
        if (!isAllowedDriveLetter(driveLetter)) {
            throw new IllegalArgumentException("drive letter " + driveLetter + " not in the range of supported drives:"
                    + getSupportedDriveLetters());
        }

        if (isDriveMounted(driveLetter)) {
            String command = createUnmountCommand(driveLetter);
            log.debug("unmounting: " + command);
            executeCmd(command);
            
            if (SystemUtils.IS_OS_LINUX) {
                command = "rmdir " + driveLetter;
                executeCmd(command);
            }
        }
    }

    private static String createUnmountCommand(String driveLetter) {

        String command = null;
        if (SystemUtils.IS_OS_LINUX) {
            // unmount with either mounting point or Truecrypt container file
            command = getTrueCryptExe() + " -d " + driveLetter;
        }
        if (SystemUtils.IS_OS_WINDOWS) {
            // use /f to force dismount
            // when we use /q we have to use /s or /silent which suppresses all the warnings or popup windows.
            command = getTrueCryptExe() + " " + "/q background " + "/s " + "/d " + driveLetter + " /f";
        }

        return command;
    }

    public static boolean checkTrueCryptAvailable() {
        try {
            getTrueCryptExe();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static String getTrueCryptExe() throws ExceptionInInitializerError {
        if (SystemUtils.IS_OS_WINDOWS) {
            String s = Configuration.getProperty("truecrypt.home.dir");
            if (s != null && s.length() > 0 && !s.contains("\"")) {
                File f = new File(s);
                if (f.isDirectory() && f.exists()) {
                    String tcexe = f.getAbsolutePath() + "/TrueCrypt.exe";
                    File tc = new File(tcexe);
                    if (tc.isFile() && tc.exists()) {
                        return tc.getAbsolutePath();
                    }
                }
            }
            throw new ExceptionInInitializerError("Error finding TrueCrypt in " + s);
        }
        if (SystemUtils.IS_OS_LINUX) {
            String tc = "/usr/bin/truecrypt";
            File f = new File(tc);
            if (f.exists()) {
                return "/usr/bin/truecrypt";
            }
            throw new ExceptionInInitializerError("Error finding TrueCrypt in /usr/bin/");
        }
        throw new ExceptionInInitializerError("Unsupported operating system");
    }
}
