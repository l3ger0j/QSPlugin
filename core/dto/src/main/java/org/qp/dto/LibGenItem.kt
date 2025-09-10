package org.qp.dto

import kotlinx.serialization.Serializable

@Serializable
data class LibGenItem(
    val text: String = "",
    val imagePath: String = ""
)