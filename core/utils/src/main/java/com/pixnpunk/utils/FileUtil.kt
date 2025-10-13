package com.pixnpunk.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.anggrayudi.storage.extension.openInputStream
import com.anggrayudi.storage.file.isWritable
import java.io.IOException
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

object FileUtil {
    @ExperimentalContracts
    fun DocumentFile?.isWritableFile(context: Context): Boolean {
        contract {
            returns() implies (this@isWritableFile != null)
        }
        if (this == null) return false
        return this.exists() && this.isFile && this.isWritable(context)
    }

    @ExperimentalContracts
    fun DocumentFile?.isWritableDir(context: Context): Boolean {
        contract {
            returns() implies (this@isWritableDir != null)
        }
        if (this == null) return false
        return this.exists() && this.isDirectory && this.isWritable(context)
    }

    fun Uri.readFileContents(context: Context): ByteArray? {
        return runCatching {
            this.openInputStream(context).use { it?.readBytes() ?: throw NullPointerException() }
        }.getOrElse {
            Log.e(javaClass.simpleName, "Error reading file: $this", it)
            null
        }
    }

    fun Uri.writeFileContents(
        context: Context,
        dataToWrite: ByteArray
    ) {
        context.contentResolver
            .runCatching {
                openOutputStream(this@writeFileContents).use { out ->
                    out?.write(dataToWrite) ?: throw IOException("Input is NULL!")
                }
            }
            .onFailure {
                Log.e(javaClass.simpleName, "Error reading file: $this", it)
            }
    }
}
