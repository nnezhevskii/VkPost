package com.licht.vkpost.vkpost.utils

import android.graphics.Color

fun getBrighterColor(sourceColor: Int): Int {
    val hsv = FloatArray(3)
    Color.colorToHSV(sourceColor, hsv)
    hsv[2] *= 1.3f

    return Color.HSVToColor(hsv)
}

fun getLessColor(sourceColor: Int): Int {
    val hsv = FloatArray(3)
    Color.colorToHSV(sourceColor, hsv)
    hsv[2] *= 0.7f

    return Color.HSVToColor(hsv)
}