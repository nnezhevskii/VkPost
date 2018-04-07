package com.licht.vkpost.vkpost.utils

import android.graphics.Bitmap
import android.graphics.Matrix
import com.licht.vkpost.vkpost.data.model.StickerItem

public fun buildMatrix(bitmap: Bitmap, sticker: StickerItem): Matrix {

    val dx = bitmap.width * sticker.scaleFactor.toFloat() - bitmap.width
    val dy = bitmap.height * sticker.scaleFactor.toFloat() - bitmap.height

    val matrix = Matrix()
    matrix.postRotate(sticker.angle, bitmap.width / 2f, bitmap.height / 2f)
    matrix.postScale(sticker.scaleFactor.toFloat(), sticker.scaleFactor.toFloat())
    matrix.postTranslate(sticker.left.toFloat() - dx / 2, sticker.top.toFloat() - dy / 2)

    return matrix
}


public fun isTapOnSticker(sticker: StickerItem, x: Int, y: Int): Boolean {
    return x >= sticker.left && y >= sticker.top &&
            x <= sticker.left + sticker.width && y <= sticker.top + sticker.height
}
