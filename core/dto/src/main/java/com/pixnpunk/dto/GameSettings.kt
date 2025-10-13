package com.pixnpunk.dto

import android.graphics.Color
import android.graphics.Typeface
import android.os.Parcel
import android.os.Parcelable
import androidx.core.os.ParcelCompat
import com.pixnpunk.dto.TypefaceParceler.write

data class GameSettings(
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
    val nativeLibVersion: Int = 570,
    val countActsVis: Int = 3,
    val language: String? = "",
    val theme: String? = ""
) : Parcelable {

    private constructor(`in`: Parcel) : this(
        typeface = TypefaceParceler.create(`in`),
        fontSize = `in`.readInt(),
        backColor = `in`.readLong().toInt(),
        textColor = `in`.readLong().toInt(),
        linkColor = `in`.readLong().toInt(),
        customWidthImage = `in`.readInt(),
        customHeightImage = `in`.readInt(),
        nativeLibVersion = `in`.readInt(),
        countActsVis = `in`.readInt(),
        isSoundEnabled = ParcelCompat.readBoolean(`in`),
        isImageDisabled = ParcelCompat.readBoolean(`in`),
        isUseAutoWidth = ParcelCompat.readBoolean(`in`),
        isUseAutoHeight = ParcelCompat.readBoolean(`in`),
        isUseGameFont = ParcelCompat.readBoolean(`in`),
        isUseAutoscroll = ParcelCompat.readBoolean(`in`),
        isUseExecString = ParcelCompat.readBoolean(`in`),
        isUseImmersiveMode = ParcelCompat.readBoolean(`in`),
        isUseGameTextColor = ParcelCompat.readBoolean(`in`),
        isUseGameBackgroundColor = ParcelCompat.readBoolean(`in`),
        isUseGameLinkColor = ParcelCompat.readBoolean(`in`),
        isUseFullscreenImages = ParcelCompat.readBoolean(`in`),
        isUseImageDebug = ParcelCompat.readBoolean(`in`),
        isUseMusicDebug = ParcelCompat.readBoolean(`in`),
        isVideoMute = ParcelCompat.readBoolean(`in`),
        language = `in`.readString(),
        theme = `in`.readString()
    )

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.apply {
            typeface.write(this, flags)
            writeInt(fontSize)
            writeInt(backColor)
            writeInt(textColor)
            writeInt(linkColor)
            writeInt(customWidthImage)
            writeInt(customHeightImage)
            writeInt(nativeLibVersion)
            writeInt(countActsVis)
            ParcelCompat.writeBoolean(this, isSoundEnabled)
            ParcelCompat.writeBoolean(this, isImageDisabled)
            ParcelCompat.writeBoolean(this, isUseAutoWidth)
            ParcelCompat.writeBoolean(this, isUseAutoHeight)
            ParcelCompat.writeBoolean(this, isUseGameFont)
            ParcelCompat.writeBoolean(this, isUseAutoscroll)
            ParcelCompat.writeBoolean(this, isUseExecString)
            ParcelCompat.writeBoolean(this, isUseImmersiveMode)
            ParcelCompat.writeBoolean(this, isUseGameTextColor)
            ParcelCompat.writeBoolean(this, isUseGameBackgroundColor)
            ParcelCompat.writeBoolean(this, isUseGameLinkColor)
            ParcelCompat.writeBoolean(this, isUseFullscreenImages)
            ParcelCompat.writeBoolean(this, isUseImageDebug)
            ParcelCompat.writeBoolean(this, isUseMusicDebug)
            ParcelCompat.writeBoolean(this, isVideoMute)
            writeString(language)
            writeString(theme)
        }
    }

    companion object CREATOR : Parcelable.Creator<GameSettings> {
        override fun createFromParcel(source: Parcel): GameSettings? = GameSettings(source)
        override fun newArray(size: Int): Array<GameSettings?> = arrayOfNulls(size)
    }
}