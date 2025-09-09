package org.qp.dto

import android.graphics.Typeface
import android.os.Parcel
import kotlinx.parcelize.Parceler

internal object TypefaceParceler : Parceler<Typeface> {
    override fun Typeface.write(parcel: Parcel, flags: Int) {
        parcel.writeInt(
            when (this) {
                Typeface.SANS_SERIF -> 1
                Typeface.SERIF -> 2
                Typeface.MONOSPACE -> 3
                else -> 0
            }
        )
    }

    override fun create(parcel: Parcel): Typeface {
        return when (parcel.readInt()) {
            1 -> Typeface.SANS_SERIF
            2 -> Typeface.SERIF
            3 -> Typeface.MONOSPACE
            else -> Typeface.DEFAULT
        }
    }
}