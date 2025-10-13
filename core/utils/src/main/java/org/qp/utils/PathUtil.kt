package org.qp.utils

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.anggrayudi.storage.file.child
import com.anggrayudi.storage.file.getAbsolutePath
import org.qp.utils.FileUtil.isWritableDir
import org.qp.utils.FileUtil.isWritableFile
import java.io.File
import kotlin.contracts.ExperimentalContracts

object PathUtil {
    fun String.getFilename(): String {
        val idx = this.lastIndexOf('/')
        return if (idx == -1) this else this.substring(idx + 1)
    }

    fun String.absToRelPath(context: Context, rootDir: DocumentFile) =
        if (!this.startsWith('/')) {
            this
        } else {
            val rootDirPathAsFile = File(rootDir.getAbsolutePath(context))
            File(this).toRelativeString(rootDirPathAsFile)
        }

    fun String.pathToFile(context: Context, rootDir: DocumentFile) =
        if (!this.startsWith('/')) {
            rootDir.child(context, this)
        } else {
            val rootDirPathAsFile = File(rootDir.getAbsolutePath(context))
            val fullPathAsRelative = File(this).toRelativeString(rootDirPathAsFile)
            rootDir.child(context, fullPathAsRelative)
        }

    @OptIn(ExperimentalContracts::class)
    fun String.getImageUriFromPath(
        context: Context,
        rootDir: DocumentFile?
    ): Uri {
        if (!rootDir.isWritableDir(context)) return Uri.EMPTY
        val relPath = this.toUri().path
        if (relPath.isNullOrEmpty()) return Uri.EMPTY
        val imageFile = relPath.pathToFile(context, rootDir)
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
