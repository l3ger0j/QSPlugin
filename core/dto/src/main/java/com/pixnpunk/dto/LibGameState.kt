package com.pixnpunk.dto

import android.net.Uri
import kotlinx.serialization.Serializable

@Serializable
data class LibGameState(
    val gameRunning: Boolean = false,
    val gameId: Long = 0L,
    val gameTitle: String = "",
    @Serializable(with = URISerializer::class)
    val gameDirUri: Uri = Uri.EMPTY,
    @Serializable(with = URISerializer::class)
    val gameFileUri: Uri = Uri.EMPTY,
    val mainDesc: String = "",
    val varsDesc: String = "",
    val actionsList: List<LibGenItem> = listOf(),
    val objectsList: List<LibGenItem> = listOf(),
)