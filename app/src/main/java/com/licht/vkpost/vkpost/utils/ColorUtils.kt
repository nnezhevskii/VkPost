package com.licht.vkpost.vkpost.utils

import android.graphics.Color

fun getBrighterColor(sourceColor: Int): Int {
    val hsv = FloatArray(3)
    Color.colorToHSV(sourceColor, hsv)
    hsv[2] *= 1.5f

    return Color.HSVToColor(hsv)
}

fun getLessColor(sourceColor: Int): Int {
    val hsv = FloatArray(3)
    Color.colorToHSV(sourceColor, hsv)
    hsv[2] *= 0.75f

    return Color.HSVToColor(hsv)
}
