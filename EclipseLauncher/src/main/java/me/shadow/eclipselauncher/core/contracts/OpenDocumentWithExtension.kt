package me.shadow.eclipselauncher.core.contracts

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap

import androidx.activity.result.contract.ActivityResultContract

import java.util.ArrayList

// Android's OpenDocument contract is the basicmost crap that doesn't allow
// you to specify practically anything. So i made this instead.
/**
 * Create a new OpenDocumentWithExtension contract.
 * If the extension provided to the constructor is not available in the device's MIME
 * type database, the filter will default to "all types"
 * @param extension the extension to filter by
 * @param allowMultiple whether or not to allow multiple file selections
 */
class OpenDocumentWithExtension @JvmOverloads constructor(
    extension: String?,
    allowMultiple: Boolean = false
) : ActivityResultContract<Any, List<Uri>?>() {
    private val mimeType: String
    private val allowMultiple: Boolean

    init {
        var extensionMimeType: String? = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        if (extensionMimeType == null) extensionMimeType = "*/*"
        mimeType = extensionMimeType
        this.allowMultiple = allowMultiple
    }

    override fun createIntent(context: Context, input: Any): Intent {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = mimeType

        //Decide whether multiple selection is allowed based on the constructor argument
        if (allowMultiple) {
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }

        return intent
    }

    final override fun getSynchronousResult(
        context: Context,
        input: Any
    ): ActivityResultContract.SynchronousResult<List<Uri>?>? {
        return null
    }

    final override fun parseResult(resultCode: Int, intent: Intent?): List<Uri>? {
        if (intent == null || resultCode != Activity.RESULT_OK) return null

        val uris = ArrayList<Uri>()
        val clipData = intent.clipData
        if (clipData != null) {
            //Multiple items were selected
            for (i in 0 until clipData.itemCount) {
                uris.add(clipData.getItemAt(i).uri)
            }
        } else {
            // The java twin allowed a null data element here; !! keeps the NPE parity
            // on the (never observed) no-data-and-no-clip-data result.
            uris.add(intent.data!!)
        }

        return uris
    }
}
