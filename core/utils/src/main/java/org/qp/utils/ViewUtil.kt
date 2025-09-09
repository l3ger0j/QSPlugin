package org.qp.utils

import android.graphics.Typeface

object ViewUtil {
    fun Typeface.asString(): String {
        return when (this) {
            Typeface.SANS_SERIF -> "sans-serif"
            Typeface.SERIF -> "serif"
            Typeface.MONOSPACE -> "courier"
            Typeface.DEFAULT -> "default"
            else -> "default"
        }
    }
}