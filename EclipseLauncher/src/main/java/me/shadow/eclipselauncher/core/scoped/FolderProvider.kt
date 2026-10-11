package me.shadow.eclipselauncher.core.scoped

import android.content.ContentResolver
import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.database.MatrixCursor
import android.graphics.Point
import android.net.Uri
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import android.provider.DocumentsContract.Root
import android.provider.DocumentsProvider
import android.webkit.MimeTypeMap

import me.shadow.eclipselauncher.BuildConfig
import me.shadow.eclipselauncher.InfoDistributor
import me.shadow.eclipselauncher.R
import me.shadow.eclipselauncher.feature.log.Logging
import me.shadow.eclipselauncher.utils.path.PathManager

import me.shadow.eclipselauncher.core.Tools

import me.shadow.eclipselauncher.utils.io.FileUtils

import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.LinkedList
import java.util.Locale

/**
 * A document provider for the Storage Access Framework which exposes the files in the
 * $HOME/ directory to other apps.
 * <p/>
 * Note that this replaces providing an activity matching the ACTION_GET_CONTENT intent:
 * <p/>
 * "A document provider and ACTION_GET_CONTENT should be considered mutually exclusive. If you
 * support both of them simultaneously, your app will appear twice in the system picker UI,
 * offering two different ways of accessing your stored data. This would be confusing for users."
 * - <a href="http://developer.android.com/guide/topics/providers/document-provider.html#43">...</a>
 */
class FolderProvider : DocumentsProvider() {

    private lateinit var BASE_DIR: File

    private lateinit var mContentResolver: ContentResolver
    private lateinit var mStorageProviderAuthortiy: String

    override fun queryRoots(projection: Array<String>?): Cursor {
        val result = MatrixCursor(projection ?: DEFAULT_ROOT_PROJECTION)

        var summary = InfoDistributor.APP_NAME
        if (BuildConfig.DEBUG) {
            summary = "(${context.getString(R.string.generic_debug)}) $summary"
        }

        val row = result.newRow()
        row.add(Root.COLUMN_ROOT_ID, getDocIdForFile(BASE_DIR))
        row.add(Root.COLUMN_DOCUMENT_ID, getDocIdForFile(BASE_DIR))
        row.add(Root.COLUMN_SUMMARY, summary)
        row.add(Root.COLUMN_FLAGS, Root.FLAG_SUPPORTS_CREATE or Root.FLAG_SUPPORTS_SEARCH or Root.FLAG_SUPPORTS_IS_CHILD)
        row.add(Root.COLUMN_TITLE, InfoDistributor.APP_NAME)
        row.add(Root.COLUMN_MIME_TYPES, ALL_MIME_TYPES)
        row.add(Root.COLUMN_AVAILABLE_BYTES, BASE_DIR.freeSpace)
        row.add(Root.COLUMN_ICON, R.mipmap.ic_launcher)
        return result
    }

    @Throws(FileNotFoundException::class)
    override fun queryDocument(documentId: String, projection: Array<String>?): Cursor {
        val result = MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION)
        // Future-proofing in case if we implement realtime file watching
        result.setNotificationUri(mContentResolver, createUriForDocId(documentId))
        includeFile(result, documentId, null)
        return result
    }

    @Throws(FileNotFoundException::class)
    override fun queryChildDocuments(parentDocumentId: String, projection: Array<String>?, sortOrder: String?): Cursor {
        val result = MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION)
        val parent = getFileForDocId(parentDocumentId)
        val children = parent.listFiles()
        if (children == null) throw FileNotFoundException("Unable to list files in " + parent.absolutePath)
        for (file in children) {
            includeFile(result, null, file)
        }
        // Set the notification URI as that's what the "Files" app will be listening to in case of file deletion
        result.setNotificationUri(mContentResolver, createUriForDocId(parentDocumentId))
        return result
    }

    @Throws(FileNotFoundException::class)
    override fun openDocument(documentId: String, mode: String, signal: CancellationSignal?): ParcelFileDescriptor {
        val file = getFileForDocId(documentId)
        val accessMode = ParcelFileDescriptor.parseMode(mode)
        return ParcelFileDescriptor.open(file, accessMode)
    }

    @Throws(FileNotFoundException::class)
    override fun openDocumentThumbnail(documentId: String, sizeHint: Point, signal: CancellationSignal?): AssetFileDescriptor {
        val file = getFileForDocId(documentId)
        val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        return AssetFileDescriptor(pfd, 0L, file.length())
    }

    override fun onCreate(): Boolean {
        if (Tools.checkStorageRoot()) {
            PathManager.initContextConstants(context)
        } else {
            return false
        }
        BASE_DIR = File(PathManager.DIR_GAME_HOME)
        mContentResolver = context.contentResolver
        mStorageProviderAuthortiy = context.getString(R.string.storageProviderAuthorities)
        return true
    }

    @Throws(FileNotFoundException::class)
    override fun createDocument(parentDocumentId: String, mimeType: String, displayName: String): String {
        var newFile = File(parentDocumentId, displayName)
        var noConflictId = 2
        while (newFile.exists()) {
            newFile = File(parentDocumentId, displayName + " (" + noConflictId++ + ")")
        }
        try {
            val succeeded: Boolean
            if (Document.MIME_TYPE_DIR == mimeType) {
                succeeded = newFile.mkdir()
            } else {
                succeeded = newFile.createNewFile()
            }
            if (!succeeded) {
                throw FileNotFoundException("Failed to create document with id " + newFile.path)
            }
        } catch (e: IOException) {
            throw FileNotFoundException("Failed to create document with id " + newFile.path)
        }
        // Notify the file manager that the parent directory has changed
        notifyChange(createUriForDocId(parentDocumentId))
        return newFile.path
    }

    @Throws(FileNotFoundException::class)
    override fun renameDocument(documentId: String, displayName: String): String {
        val sourceFile = getFileForDocId(documentId)
        val sourceParent = sourceFile.parentFile
        if (sourceParent == null) throw FileNotFoundException("Cannot rename root")
        val targetFile = File(getDocIdForFile(sourceParent) + "/" + displayName)
        if (!sourceFile.renameTo(targetFile)) {
            throw FileNotFoundException("Couldn't rename the document with id" + documentId)
        }
        return getDocIdForFile(targetFile)
    }

    @Throws(FileNotFoundException::class)
    override fun moveDocument(sourceDocumentId: String, sourceParentDocumentId: String, targetParentDocumentId: String): String {
        val sourceFile = getFileForDocId(sourceParentDocumentId + sourceDocumentId)
        val targetFile = File(targetParentDocumentId + sourceDocumentId)
        if (!sourceFile.renameTo(targetFile)) {
            throw FileNotFoundException("Failed to move the document with id " + sourceFile.path)
        }
        return getDocIdForFile(targetFile)
    }

    @Throws(FileNotFoundException::class)
    override fun removeDocument(documentId: String, parentDocumentId: String) {
        deleteDocument(parentDocumentId + "/" + documentId)
    }

    @Throws(FileNotFoundException::class)
    override fun deleteDocument(documentId: String) {
        val file = getFileForDocId(documentId)
        if (file.isDirectory) {
            try {
                FileUtils.deleteDirectory(file)
            } catch (e: IOException) {
                throw FileNotFoundException("Failed to delete document with id " + documentId)
            }
        } else {
            if (!file.delete()) {
                throw FileNotFoundException("Failed to delete document with id " + documentId)
            }
        }
        // Notify the file manager that the parent directory has changed
        notifyChange(createUriForFile(file.parentFile!!))
    }

    @Throws(FileNotFoundException::class)
    override fun getDocumentType(documentId: String): String {
        Logging.i("FolderPRovider", "getDocumentType($documentId)")
        val file = getFileForDocId(documentId)
        return getMimeType(file)
    }

    @Throws(FileNotFoundException::class)
    override fun querySearchDocuments(rootId: String, query: String, projection: Array<String>?): Cursor {
        val result = MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION)
        val parent = getFileForDocId(rootId)

        // This example implementation searches file names for the query and doesn't rank search
        // results, so we can stop as soon as we find a sufficient number of matches.  Other
        // implementations might rank results and use other data about files, rather than the file
        // name, to produce a match.
        val pending = LinkedList<File>()
        pending.add(parent)

        val MAX_SEARCH_RESULTS = 50
        while (!pending.isEmpty() && result.count < MAX_SEARCH_RESULTS) {
            val file = pending.removeFirst()
            // Avoid directories outside the $HOME directory linked with symlinks (to avoid e.g. search
            // through the whole SD card).
            var isInsideHome: Boolean
            try {
                isInsideHome = file.canonicalPath.startsWith(PathManager.DIR_GAME_HOME)
            } catch (e: IOException) {
                isInsideHome = true
            }
            if (isInsideHome) {
                if (file.isDirectory) {
                    val listing = file.listFiles()
                    if (listing != null) pending.addAll(listing)
                } else {
                    if (file.name.lowercase(Locale.getDefault()).contains(query)) {
                        includeFile(result, null, file)
                    }
                }
            }
        }

        return result
    }

    override fun isChildDocument(parentDocumentId: String, documentId: String): Boolean {
        return documentId.startsWith(parentDocumentId)
    }

    /**
     * Add a representation of a file to a cursor.
     *
     * @param result the cursor to modify
     * @param docId  the document ID representing the desired file (may be null if given file)
     * @param file   the File object representing the desired file (may be null if given docID)
     */
    private fun includeFile(result: MatrixCursor, docId: String?, file: File?) {
        // The java twin reassigns both parameters; resolved* mirrors that without
        // mutating kotlin parameters (null file + null docId still NPEs like java).
        var resolvedDocId: String
        var resolvedFile: File
        if (docId == null) {
            resolvedFile = file!!
            resolvedDocId = getDocIdForFile(resolvedFile)
        } else {
            resolvedDocId = docId
            resolvedFile = getFileForDocId(docId)
        }

        var flags = 0
        if (resolvedFile.isDirectory) {
            if (resolvedFile.canWrite) flags = flags or Document.FLAG_DIR_SUPPORTS_CREATE
        } else if (resolvedFile.canWrite) {
            flags = flags or Document.FLAG_SUPPORTS_WRITE
        }
        val parent = resolvedFile.parentFile
        if (parent != null) { // Only fails in one case: when the parent is /, which you can't delete.
            if (parent.canWrite) flags = flags or Document.FLAG_SUPPORTS_DELETE
        }

        val displayName = resolvedFile.name
        val mimeType = getMimeType(resolvedFile)
        if (mimeType.startsWith("image/")) flags = flags or Document.FLAG_SUPPORTS_THUMBNAIL

        val row = result.newRow()
        row.add(Document.COLUMN_DOCUMENT_ID, resolvedDocId)
        row.add(Document.COLUMN_DISPLAY_NAME, displayName)
        row.add(Document.COLUMN_SIZE, resolvedFile.length())
        row.add(Document.COLUMN_MIME_TYPE, mimeType)
        row.add(Document.COLUMN_LAST_MODIFIED, resolvedFile.lastModified())
        row.add(Document.COLUMN_FLAGS, flags)
        row.add(Document.COLUMN_ICON, R.mipmap.ic_launcher)
    }

    @Throws(FileNotFoundException::class)
    override fun findDocumentPath(parentDocumentId: String?, childDocumentId: String): DocumentsContract.Path? {
        var source = BASE_DIR
        if (parentDocumentId != null) source = getFileForDocId(parentDocumentId)
        var destination: File? = getFileForDocId(childDocumentId)
        val pathIds = ArrayList<String>()
        while (!source.equals(destination) && destination != null) {
            pathIds.add(getDocIdForFile(destination))
            destination = destination.getParentFile()
        }
        pathIds.add(getDocIdForFile(source))
        pathIds.reverse()
        Logging.i("FolderProvider", pathIds.toString())
        return DocumentsContract.Path(getDocIdForFile(source), pathIds)
    }

    private fun createUriForDocId(documentId: String): Uri {
        return createUriForFile(getFileForDocId(documentId))
    }

    private fun createUriForFile(file: File): Uri {
        return DocumentsContract.buildDocumentUri(mStorageProviderAuthortiy, file.absolutePath)
    }

    private fun notifyChange(uri: Uri) {
        mContentResolver.notifyChange(uri, null)
    }

    companion object {
        private const val ALL_MIME_TYPES = "*/*"

        // The default columns to return information about a root if no specific
        // columns are requested in a query.
        private val DEFAULT_ROOT_PROJECTION = arrayOf(
            Root.COLUMN_ROOT_ID,
            Root.COLUMN_MIME_TYPES,
            Root.COLUMN_FLAGS,
            Root.COLUMN_ICON,
            Root.COLUMN_TITLE,
            Root.COLUMN_SUMMARY,
            Root.COLUMN_DOCUMENT_ID,
            Root.COLUMN_AVAILABLE_BYTES
        )

        // The default columns to return information about a document if no specific
        // columns are requested in a query.
        private val DEFAULT_DOCUMENT_PROJECTION = arrayOf(
            Document.COLUMN_DOCUMENT_ID,
            Document.COLUMN_MIME_TYPE,
            Document.COLUMN_DISPLAY_NAME,
            Document.COLUMN_LAST_MODIFIED,
            Document.COLUMN_FLAGS,
            Document.COLUMN_SIZE
        )

        /**
         * Get the document id given a file. This document id must be consistent across time as other
         * applications may save the ID and use it to reference documents later.
         * <p/>
         * The reverse of @{link #getFileForDocId}.
         */
        private fun getDocIdForFile(file: File): String {
            return file.absolutePath
        }

        /**
         * Get the file given a document id (the reverse of {@link #getDocIdForFile(File)}).
         */
        @Throws(FileNotFoundException::class)
        private fun getFileForDocId(docId: String): File {
            val f = File(docId)
            if (!f.exists()) throw FileNotFoundException(f.absolutePath + " not found")
            return f
        }

        private fun getMimeType(file: File): String {
            if (file.isDirectory) {
                return Document.MIME_TYPE_DIR
            } else {
                val name = file.name
                val lastDot = name.lastIndexOf('.')
                if (lastDot >= 0) {
                    val extension = name.substring(lastDot + 1).lowercase(Locale.getDefault())
                    val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
                    if (mime != null) return mime
                }
                return "application/octet-stream"
            }
        }
    }
}
