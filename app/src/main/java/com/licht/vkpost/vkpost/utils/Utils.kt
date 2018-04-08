package com.licht.vkpost.vkpost.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.licht.vkpost.vkpost.data.model.StickerItem
import java.io.File
import java.io.FileOutputStream

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

public fun saveImage(context: Context, finalBitmap: Bitmap, image_name: String) {

    MediaStore.Images.Media.insertImage(
            context.getContentResolver(),
            finalBitmap,
            image_name, ""
    );

}