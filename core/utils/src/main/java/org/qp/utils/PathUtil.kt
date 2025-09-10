package org.qp.utils

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.anggrayudi.storage.file.child
import org.qp.utils.FileUtil.isWritableDir
import org.qp.utils.FileUtil.isWritableFile
import kotlin.contracts.ExperimentalContracts

object PathUtil {
    fun String.getFilename(): String {
        val idx = this.lastIndexOf('/')
        return if (idx == -1) this else this.substring(idx + 1)
    }

    @OptIn(ExperimentalContracts::class)
    fun String.getImageUriFromPath(
        context: Context,
        rootDir: DocumentFile?
    ): Uri {
        if (!rootDir.isWritableDir(context)) return Uri.EMPTY
        val relPath = this.toUri().path
        if (relPath.isNullOrEmpty()) return Uri.EMPTY
        val imageFile = rootDir.child(context, relPath)
        if (!imageFile.isWritableFile(context)) return Uri.EMPTY
        return imageFile.uri
    }

    /**
     * Leads to the normal form of the path to the game resource (melodies, images).
     *
     * @implNote Removes "./" from the beginning of the path, replaces all occurrences of "\" with "/".
     */
    fun String.normalizeContentPath(): String {
        var result: String = this
        if (result.startsWith("./")) {
            result = result.substring(2)
        }
        return result.replace("\\", "/")
    }
}
