package org.backmeup.index.utils.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

public class FileUtils {

	/**
	 * Copies a file from source to dest using FileChannels and creates the file
	 * and directory if it does not already exist.
	 */
	public static File copyFileUsingChannel(File source, File dest)
			throws IOException {

		FileChannel sourceChannel = null;
		FileChannel destChannel = null;
		try {
			sourceChannel = new FileInputStream(source).getChannel();
			if (dest.exists()) {
				dest.delete();
			}
			dest.getParentFile().mkdirs();
			dest.createNewFile();

			destChannel = new FileOutputStream(dest).getChannel();
			destChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
			return dest;
		} finally {
			if (sourceChannel != null)
				sourceChannel.close();
			if (destChannel != null)
				destChannel.close();
		}
	}

	/**
	 * Recursively deletes a directory including all files.
	 */
	static public boolean deleteDirectory(File path) {
		if (path.exists()) {
			File[] files = path.listFiles();
			for (int i = 0; i < files.length; i++) {
				if (files[i].isDirectory()) {
					deleteDirectory(files[i]);
				} else {
					files[i].delete();
				}
			}
		}
		return (path.delete());
	}

}
