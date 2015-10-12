package org.backmeup.index.utils.cmd;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.lang.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT;

/**
 * Utility class to unify execution of shell commands, retrieval/termination of Processes (via PID)
 *
 */
public class CommandLineUtils {

    private static final Logger log = LoggerFactory.getLogger(CommandLineUtils.class);

    /**
     * Checks if a Windows/Linux Process is still running.
     * 
     * @param pid
     * @param timeout
     * @param timeunit
     * @return
     * @throws java.io.IOException
     */
    public static boolean isProcessRunning(int pid, int timeout, TimeUnit timeunit) throws IOException {
        String line;
        if (SystemUtils.IS_OS_WINDOWS) {
            //tasklist exit code is always 0. Parse output
            //findstr exit code 0 if found pid, 1 if it doesn't
            line = "cmd /c \"tasklist /FI \"PID eq " + pid + "\" | findstr " + pid + "\"";
        } else {
            //ps exit code 0 if process exists, 1 if it doesn't
            line = "ps -p " + pid;
        }
        int exitValue = executeCommandLine(line, timeout, timeunit);
        // 0 is the default exit code which means the process exists
        return exitValue == 0;
    }

    /**
     * Kills a running process in Linux or Windows via PID
     * 
     * @param pid
     * @param timeout
     * @param timeunit
     * @return
     * @throws IOException
     */
    public static int killProcess(int pid, int timeout, TimeUnit timeunit) throws IOException {
        String line;
        if (SystemUtils.IS_OS_WINDOWS) {
            //tasklist exit code is always 0. Parse output
            line = "cmd /c \"taskkill /F /PID " + pid + "\"";
        } else {
            //ps exit code 0 if process exists, 1 if it doesn't
            line = "sudo kill -9 " + pid;
        }
        int exitValue = executeCommandLine(line, timeout, timeunit);
        System.out.println("executing command: " + line + " returned exitValue: " + exitValue);
        return exitValue;
    }

    public static int executeCommandLine(String command, int timeout, TimeUnit timeunit) throws IOException {
        log.debug("executing: " + command);
        CommandLine cmdLine = CommandLine.parse(command);
        DefaultExecutor executor = new DefaultExecutor();
        // disable logging of stdout/strderr
        executor.setStreamHandler(new PumpStreamHandler(null, null, null));
        // disable exception for valid exit values
        executor.setExitValues(new int[] { 0, 1 });
        // set timer for zombie process
        ExecuteWatchdog timeoutWatchdog = new ExecuteWatchdog(timeunit.toMillis(timeout));
        executor.setWatchdog(timeoutWatchdog);
        int exitValue = executor.execute(cmdLine);
        return exitValue;
    }

    /**
     * Returns the PID of runtime.exec Process both for Windows and Linux
     * 
     * @param p
     * @return
     */
    public static int getExecPID(Process p) {
        int pid = -1;
        if (SystemUtils.IS_OS_LINUX) {
            try {
                Field f = p.getClass().getDeclaredField("pid");
                f.setAccessible(true);
                pid = f.getInt(p);

            } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
                log.debug("Issue getting PID of CommandLine call " + e.toString());
            }
        }
        if (SystemUtils.IS_OS_WINDOWS) {
            try {
                Field f = p.getClass().getDeclaredField("handle");
                f.setAccessible(true);
                long handLong = f.getLong(p);
                Kernel32 kernel = Kernel32.INSTANCE;
                WinNT.HANDLE handle = new WinNT.HANDLE();
                handle.setPointer(Pointer.createConstant(handLong));
                pid = kernel.GetProcessId(handle);
            } catch (Exception e) {
                log.debug("Issue getting PID of CommandLine call " + e.toString());
            }
        }
        log.debug("Executing CommandLine call with PID of " + pid);
        return pid;
    }

    public static void chownRTomcat7(File f) {
        if (SystemUtils.IS_OS_LINUX) {
            String command = "sudo chown -R tomcat7:tomcat7 " + f.getAbsolutePath();
            try {
                int exitVal = CommandLineUtils.executeCommandLine(command, 2, TimeUnit.SECONDS);
                if (exitVal != 0) {
                    throw new IllegalArgumentException("error executing command " + command + " exit value: " + exitVal);
                }
            } catch (IOException e) {
                throw new IllegalArgumentException("error executing command " + command, e);
            }
        }
    }

    public static void chmod755(File f) {
        if (SystemUtils.IS_OS_LINUX) {
            String command = "sudo chmod 755 " + f.getAbsolutePath();
            try {
                int exitVal = CommandLineUtils.executeCommandLine(command, 2, TimeUnit.SECONDS);
                if (exitVal != 0) {
                    throw new IllegalArgumentException("error executing command " + command + " exit value: " + exitVal);
                }
            } catch (IOException e) {
                throw new IllegalArgumentException("error executing command " + command, e);
            }
        }
    }

}
