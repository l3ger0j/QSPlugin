package org.qp.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.anggrayudi.storage.FileWrapper
import com.anggrayudi.storage.extension.openInputStream
import com.anggrayudi.storage.file.child
import com.anggrayudi.storage.file.isWritable
import org.qp.utils.StreamUtil.copy
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

object FileUtil {
    @ExperimentalContracts
    fun isWritableFile(context: Context, file: DocumentFile?): Boolean {
        contract {
            returns() implies (file != null)
        }
        if (file == null) return false
        return file.exists() && file.isFile && file.isWritable(context)
    }

    @ExperimentalContracts
    fun DocumentFile?.isWritableDir(context: Context): Boolean {
        contract {
            returns() implies (this@isWritableDir != null)
        }
        if (this == null) return false
        return this.exists() && this.isDirectory && this.isWritable(context)
    }

    fun getFileContents(
        context: Context,
        uriContent: Uri
    ): ByteArray? {
        try {
            uriContent.openInputStream(context).use { input ->
                ByteArrayOutputStream().use { out ->
                    if (input != null) copy(input, out) else throw NullPointerException()
                    return out.toByteArray()
                }
            }
        } catch (ex: Exception) {
            // TODO: 04.12.2024 Add logger
            Log.e(javaClass.simpleName, "Error reading file: $uriContent", ex)
            return null
        }
    }

    fun documentWrap(inputFile: DocumentFile): FileWrapper.Document {
        return FileWrapper.Document(inputFile)
    }

    fun writeFileContents(
        context: Context,
        uriContent: Uri,
        dataToWrite: ByteArray?
    ) {
        val resolver = context.contentResolver
        try {
            resolver.openOutputStream(uriContent, "wt").use { out ->
                if (out != null) {
                    out.write(dataToWrite)
                } else {
                    throw IOException("Input is NULL!")
                }
            }
        } catch (ex: IOException) {
            // TODO: 04.12.2024 Add logger
            Log.e(javaClass.simpleName, "Error reading file: $uriContent", ex)
        }
    }
}
