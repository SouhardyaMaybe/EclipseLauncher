package me.shadow.eclipselauncher.utils.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;

/**
 * File utilities used across the launcher: copying, moving, deleting, sizing
 * and extension-filtered listing. Semantics (thrown exceptions, parent
 * directory creation, date preservation) match the previously bundled
 * implementation exactly.
 */
public final class FileUtils {

    private static final int BUFFER_SIZE = 8192;

    private FileUtils() {
    }

    /**
     * Deletes a file or directory tree without reporting errors.
     *
     * @return whether the delete succeeded; deleting a non-existent file reports false
     */
    public static boolean deleteQuietly(File file) {
        if (file == null) return false;
        try {
            if (file.isDirectory()) cleanDirectory(file);
        } catch (Exception ignored) {
            // Continue: the directory delete below may still succeed
        }
        try {
            return file.delete();
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * Recursively deletes a directory tree.
     *
     * @throws IOException if the contents cannot be deleted or the directory itself cannot be removed
     */
    public static void deleteDirectory(File directory) throws IOException {
        if (!directory.exists()) return;
        cleanDirectory(directory);
        if (!directory.delete()) {
            throw new IOException("Unable to delete directory " + directory + ".");
        }
    }

    private static void cleanDirectory(File directory) throws IOException {
        File[] files = directory.listFiles();
        if (files == null) throw new IOException("Failed to list contents of " + directory + ".");
        IOException thrown = null;
        for (File file : files) {
            try {
                forceDelete(file);
            } catch (IOException e) {
                if (thrown == null) thrown = e;
                else thrown.addSuppressed(e);
            }
        }
        if (thrown != null) throw thrown;
    }

    private static void forceDelete(File file) throws IOException {
        if (file.isDirectory()) deleteDirectory(file);
        else if (!file.delete()) throw new IOException("Unable to delete file " + file + ".");
    }

    /** Copies a file, preserving the last modified date. */
    public static void copyFile(File sourceFile, File destinationFile) throws IOException {
        copyFile(sourceFile, destinationFile, true);
    }

    public static void copyFile(File sourceFile, File destinationFile, boolean preserveFileDate) throws IOException {
        if (sourceFile.isDirectory()) throw new FileNotFoundException("Source " + sourceFile + " exists but is a directory");
        if (!sourceFile.exists()) throw new FileNotFoundException("Source " + sourceFile + " does not exist");
        if (destinationFile.exists()) {
            if (destinationFile.isDirectory()) throw new IOException("Destination " + destinationFile + " exists but is a directory");
            if (!destinationFile.canWrite()) throw new IOException("Destination " + destinationFile + " exists but is read-only");
        }
        try (InputStream in = new FileInputStream(sourceFile);
             OutputStream out = openOutputStream(destinationFile)) {
            IOUtils.copy(in, out);
        }
        if (preserveFileDate && !destinationFile.setLastModified(sourceFile.lastModified())) {
            throw new IOException("Failed to preserve the last modified date of " + destinationFile);
        }
    }

    /** Recursively copies a directory tree, preserving file dates. */
    public static void copyDirectory(File sourceDirectory, File destinationDirectory) throws IOException {
        if (!sourceDirectory.isDirectory()) throw new IOException("Source " + sourceDirectory + " is not a directory");
        if (destinationDirectory.exists()) {
            if (!destinationDirectory.isDirectory()) throw new IOException("Destination " + destinationDirectory + " exists but is not a directory");
        } else if (!destinationDirectory.mkdirs()) {
            throw new IOException("Failed to create destination directory " + destinationDirectory);
        }
        File[] files = sourceDirectory.listFiles();
        if (files == null) throw new IOException("Failed to list contents of " + sourceDirectory + ".");
        for (File file : files) {
            File target = new File(destinationDirectory, file.getName());
            if (file.isDirectory()) copyDirectory(file, target);
            else copyFile(file, target, true);
        }
    }

    /**
     * Copies the whole stream into the file, creating parent directories as needed.
     * The input stream stays owned by the caller.
     */
    public static void copyInputStreamToFile(InputStream inputStream, File file) throws IOException {
        try (OutputStream out = openOutputStream(file)) {
            IOUtils.copy(inputStream, out);
        }
    }

    /** Writes the text with the platform default charset, overwriting the file. */
    public static void write(File file, String data) throws IOException {
        write(file, data, Charset.defaultCharset());
    }

    public static void write(File file, String data, Charset charset) throws IOException {
        try (OutputStream out = openOutputStream(file)) {
            out.write(data.getBytes(charset));
        }
    }

    /** Writes the raw bytes, creating parent directories as needed. */
    public static void writeByteArrayToFile(File file, byte[] data) throws IOException {
        try (OutputStream out = openOutputStream(file)) {
            out.write(data);
        }
    }

    /** Moves a file, falling back to copy + delete when a rename is not possible. */
    public static void moveFile(File sourceFile, File destinationFile) throws IOException {
        if (destinationFile.exists()) throw new IOException("Destination " + destinationFile + " already exists");
        if (sourceFile.isDirectory()) throw new IOException("Source " + sourceFile + " is a directory");
        if (!sourceFile.renameTo(destinationFile)) {
            copyFile(sourceFile, destinationFile, true);
            if (!sourceFile.delete()) throw new IOException("Failed to delete " + sourceFile + " after copying");
        }
    }

    /** Moves a directory, falling back to copy + delete when a rename is not possible. */
    public static void moveDirectory(File sourceDirectory, File destinationDirectory) throws IOException {
        if (destinationDirectory.exists()) throw new IOException("Destination " + destinationDirectory + " already exists");
        if (!sourceDirectory.isDirectory()) throw new IOException("Source " + sourceDirectory + " is not a directory");
        if (!sourceDirectory.renameTo(destinationDirectory)) {
            copyDirectory(sourceDirectory, destinationDirectory);
            deleteDirectory(sourceDirectory);
        }
    }

    /** Total size in bytes of a file, or of every file inside a directory tree. */
    public static long sizeOf(File file) {
        if (!file.exists()) return 0;
        if (file.isDirectory()) return sizeOfDirectory(file);
        return file.length();
    }

    private static long sizeOfDirectory(File directory) {
        long size = 0;
        File[] files = directory.listFiles();
        if (files == null) return 0;
        for (File file : files) {
            size += file.isDirectory() ? sizeOfDirectory(file) : file.length();
        }
        return size;
    }

    /**
     * Lists files (not directories) under the given directory whose name ends with
     * one of the extensions (case-insensitive, matched as {@code ".ext"}).
     *
     * @param extensions extensions to match; null or empty matches every file
     * @param recursive whether sub-directories are visited too
     */
    public static Collection<File> listFiles(File directory, String[] extensions, boolean recursive) {
        Collection<File> files = new ArrayList<>();
        collectFiles(directory, extensions, recursive, files);
        return files;
    }

    private static void collectFiles(File directory, String[] extensions, boolean recursive, Collection<File> files) {
        File[] children = directory.listFiles();
        if (children == null) return;
        for (File child : children) {
            if (child.isDirectory()) {
                if (recursive) collectFiles(child, extensions, true, files);
            } else if (hasExtension(child.getName(), extensions)) {
                files.add(child);
            }
        }
    }

    private static boolean hasExtension(String name, String[] extensions) {
        if (extensions == null || extensions.length == 0) return true;
        String lowerName = name.toLowerCase(Locale.ROOT);
        for (String extension : extensions) {
            if (lowerName.endsWith("." + extension.toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }

    private static FileOutputStream openOutputStream(File file) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Failed to create directory " + parent + " for " + file);
        }
        if (file.exists()) {
            if (file.isDirectory()) throw new IOException("File " + file + " exists but is a directory");
            if (!file.canWrite()) throw new IOException("File " + file + " exists but is read-only");
        }
        return new FileOutputStream(file);
    }
}
