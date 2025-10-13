package com.pixnpunk.dto

import android.net.Uri
import kotlinx.serialization.Serializable

@Serializable
data class LibReturnValue(
    val dialogTextValue: String = "",
    val dialogNumValue: Int = 0,
    val playFileState: Boolean = false,
    @Serializable(with = URISerializer::class)
    val fileUri: Uri = Uri.EMPTY
)