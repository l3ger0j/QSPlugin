package com.pixnpunk.dto

import android.graphics.Color
import android.graphics.Typeface
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler

@Parcelize
data class GameSettings(
    @TypeParceler<Typeface, TypefaceParceler>()
    val typeface: Typeface = Typeface.DEFAULT,
    val fontSize: Int = 16,
    val isUseGameFont: Boolean = false,
    val isUseGameTextColor: Boolean = true,
    val textColor: Int = Color.BLACK,
    val isUseGameBackgroundColor: Boolean = true,
    val backColor: Int = 0xFFBDBDBD.toInt(),
    val isUseGameLinkColor: Boolean = true,
    val linkColor: Int = Color.BLUE,
    val isImageDisabled: Boolean = false,
    val isUseAutoWidth: Boolean = true,
    val customWidthImage: Int = 400,
    val isUseAutoHeight: Boolean = true,
    val customHeightImage: Int = 400,
    val isUseFullscreenImages: Boolean = false,
    val isUseImageDebug: Boolean = false,
    val isSoundEnabled: Boolean = true,
    val isVideoMute: Boolean = true,
    val isUseMusicDebug: Boolean = false,
    val isUseAutoscroll: Boolean = false,
    val isUseExecString: Boolean = false,
    val isUseImmersiveMode: Boolean = true,
    val isUseHtml: Boolean = true,
    val nativeLibFrom: NativeLibAuthors = NativeLibAuthors.SEEDHARTHA,
    val countActsVis: Int = 3,
    val language: String? = "",
    val theme: String? = ""
) : Parcelable