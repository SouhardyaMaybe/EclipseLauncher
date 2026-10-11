package me.shadow.eclipselauncher.utils.io

import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset
import java.util.Locale

/**
 * File utilities used across the launcher: copying, moving, deleting, sizing
 * and extension-filtered listing. Semantics (thrown exceptions, parent
 * directory creation, date preservation) match the previously bundled
 * implementation exactly.
 */
object FileUtils {

    private const val BUFFER_SIZE = 8192

    /**
     * Deletes a file or directory tree without reporting errors.
     *
     * @return whether the delete succeeded; deleting a non-existent file reports false
     */
    @JvmStatic
    fun deleteQuietly(file: File?): Boolean {
        if (file == null) return false
        try {
            if (file.isDirectory) cleanDirectory(file)
        } catch (ignored: Exception) {
            // Continue: the directory delete below may still succeed
        }
        try {
            return file.delete()
        } catch (ignored: Exception) {
            return false
        }
    }

    /**
     * Recursively deletes a directory tree.
     *
     * @throws IOException if the contents cannot be deleted or the directory itself cannot be removed
     */
    @JvmStatic
    @Throws(IOException::class)
    fun deleteDirectory(directory: File) {
        if (!directory.exists()) return
        cleanDirectory(directory)
        if (!directory.delete()) {
            throw IOException("Unable to delete directory $directory.")
        }
    }

    @Throws(IOException::class)
    private fun cleanDirectory(directory: File) {
        val files = directory.listFiles() ?: throw IOException("Failed to list contents of $directory.")
        var thrown: IOException? = null
        for (file in files) {
            try {
                forceDelete(file)
            } catch (e: IOException) {
                if (thrown == null) thrown = e
                else thrown.addSuppressed(e)
            }
        }
        if (thrown != null) throw thrown
    }

    @Throws(IOException::class)
    private fun forceDelete(file: File) {
        if (file.isDirectory) deleteDirectory(file)
        else if (!file.delete()) throw IOException("Unable to delete file $file.")
    }

    /** Copies a file, preserving the last modified date. */
    @JvmStatic
    @Throws(IOException::class)
    fun copyFile(sourceFile: File, destinationFile: File) {
        copyFile(sourceFile, destinationFile, true)
    }

    @JvmStatic
    @Throws(IOException::class)
    fun copyFile(sourceFile: File, destinationFile: File, preserveFileDate: Boolean) {
        if (sourceFile.isDirectory) throw FileNotFoundException("Source $sourceFile exists but is a directory")
        if (!sourceFile.exists()) throw FileNotFoundException("Source $sourceFile does not exist")
        if (destinationFile.exists()) {
            if (destinationFile.isDirectory) throw IOException("Destination $destinationFile exists but is a directory")
            if (!destinationFile.canWrite()) throw IOException("Destination $destinationFile exists but is read-only")
        }
        FileInputStream(sourceFile).use { input ->
            openOutputStream(destinationFile).use { output ->
                IOUtils.copy(input, output)
            }
        }
        if (preserveFileDate && !destinationFile.setLastModified(sourceFile.lastModified())) {
            throw IOException("Failed to preserve the last modified date of $destinationFile")
        }
    }

    /** Recursively copies a directory tree, preserving file dates. */
    @JvmStatic
    @Throws(IOException::class)
    fun copyDirectory(sourceDirectory: File, destinationDirectory: File) {
        if (!sourceDirectory.isDirectory) throw IOException("Source $sourceDirectory is not a directory")
        if (destinationDirectory.exists()) {
            if (!destinationDirectory.isDirectory) throw IOException("Destination $destinationDirectory exists but is not a directory")
        } else if (!destinationDirectory.mkdirs()) {
            throw IOException("Failed to create destination directory $destinationDirectory")
        }
        val files = sourceDirectory.listFiles() ?: throw IOException("Failed to list contents of $sourceDirectory.")
        for (file in files) {
            val target = File(destinationDirectory, file.name)
            if (file.isDirectory) copyDirectory(file, target)
            else copyFile(file, target, true)
        }
    }

    /**
     * Copies the whole stream into the file, creating parent directories as needed.
     * The input stream stays owned by the caller.
     */
    @JvmStatic
    @Throws(IOException::class)
    fun copyInputStreamToFile(inputStream: InputStream, file: File) {
        openOutputStream(file).use { output ->
            IOUtils.copy(inputStream, output)
        }
    }

    /** Writes the text with the platform default charset, overwriting the file. */
    @JvmStatic
    @Throws(IOException::class)
    fun write(file: File, data: String) {
        write(file, data, Charset.defaultCharset())
    }

    @JvmStatic
    @Throws(IOException::class)
    fun write(file: File, data: String, charset: Charset) {
        openOutputStream(file).use { output ->
            output.write(data.toByteArray(charset))
        }
    }

    /** Writes the raw bytes, creating parent directories as needed. */
    @JvmStatic
    @Throws(IOException::class)
    fun writeByteArrayToFile(file: File, data: ByteArray) {
        openOutputStream(file).use { output ->
            output.write(data)
        }
    }

    /** Moves a file, falling back to copy + delete when a rename is not possible. */
    @JvmStatic
    @Throws(IOException::class)
    fun moveFile(sourceFile: File, destinationFile: File) {
        if (destinationFile.exists()) throw IOException("Destination $destinationFile already exists")
        if (sourceFile.isDirectory) throw IOException("Source $sourceFile is a directory")
        if (!sourceFile.renameTo(destinationFile)) {
            copyFile(sourceFile, destinationFile, true)
            if (!sourceFile.delete()) throw IOException("Failed to delete $sourceFile after copying")
        }
    }

    /** Moves a directory, falling back to copy + delete when a rename is not possible. */
    @JvmStatic
    @Throws(IOException::class)
    fun moveDirectory(sourceDirectory: File, destinationDirectory: File) {
        if (destinationDirectory.exists()) throw IOException("Destination $destinationDirectory already exists")
        if (!sourceDirectory.isDirectory) throw IOException("Source $sourceDirectory is not a directory")
        if (!sourceDirectory.renameTo(destinationDirectory)) {
            copyDirectory(sourceDirectory, destinationDirectory)
            deleteDirectory(sourceDirectory)
        }
    }

    /** Total size in bytes of a file, or of every file inside a directory tree. */
    @JvmStatic
    fun sizeOf(file: File): Long {
        if (!file.exists()) return 0
        if (file.isDirectory) return sizeOfDirectory(file)
        return file.length()
    }

    private fun sizeOfDirectory(directory: File): Long {
        var size = 0L
        val files = directory.listFiles() ?: return 0
        for (file in files) {
            size += if (file.isDirectory) sizeOfDirectory(file) else file.length()
        }
        return size
    }

    /**
     * Lists files (not directories) under the given directory whose name ends with
     * one of the extensions (case-insensitive, matched as ".ext").
     *
     * @param extensions extensions to match; null or empty matches every file
     * @param recursive whether sub-directories are visited too
     */
    @JvmStatic
    fun listFiles(directory: File, extensions: Array<String>?, recursive: Boolean): Collection<File> {
        val files = ArrayList<File>()
        collectFiles(directory, extensions, recursive, files)
        return files
    }

    private fun collectFiles(directory: File, extensions: Array<String>?, recursive: Boolean, files: MutableCollection<File>) {
        val children = directory.listFiles() ?: return
        for (child in children) {
            if (child.isDirectory) {
                if (recursive) collectFiles(child, extensions, true, files)
            } else if (hasExtension(child.name, extensions)) {
                files.add(child)
            }
        }
    }

    private fun hasExtension(name: String, extensions: Array<String>?): Boolean {
        if (extensions == null || extensions.isEmpty()) return true
        val lowerName = name.lowercase(Locale.ROOT)
        for (extension in extensions) {
            if (lowerName.endsWith("." + extension.lowercase(Locale.ROOT))) return true
        }
        return false
    }

    @Throws(IOException::class)
    private fun openOutputStream(file: File): FileOutputStream {
        val parent = file.parentFile
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw IOException("Failed to create directory $parent for $file")
        }
        if (file.exists()) {
            if (file.isDirectory) throw IOException("File $file exists but is a directory")
            if (!file.canWrite()) throw IOException("File $file exists but is read-only")
        }
        return FileOutputStream(file)
    }
}
