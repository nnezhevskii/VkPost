package com.licht.vkpost.vkpost.utils

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.*
import android.os.Environment
import android.os.Handler
import android.provider.MediaStore
import android.support.constraint.solver.widgets.Rectangle
import android.support.v4.content.ContextCompat
import android.util.Log
import android.util.TypedValue
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import com.licht.vkpost.vkpost.App
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


fun getColorByRes(resourceId: Int): Int {
    return ContextCompat.getColor(App.application, resourceId)
}

fun dpToPx(dp: Int): Int {
    val r = App.application.getResources()
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), r.getDisplayMetrics()).toInt()
}

fun getBitmapFromResoure(resources: Resources, id: Int): Bitmap = BitmapFactory.decodeResource(resources, id)


fun openKeyboard(context: Context, editText: EditText) {
    Handler().postDelayed({
        editText.requestFocus()
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
    }, 100)
}

fun isIntersectingWithRectangle(rect: RectF?, actualX: Int, actualY: Int): Boolean {
    return rect?.let { actualX in it.left..it.right && actualY in it.top..it.bottom }
            ?: false
}

fun buildIntentForPickImage(): Intent {
    val intent = Intent()
    intent.type = "image/*"
    intent.action = Intent.ACTION_GET_CONTENT
    return Intent.createChooser(intent, "Select Picture")
}

fun RectF.buildScaleMatrixTo(dst: RectF): Matrix {
    val matrix = Matrix()
    matrix.setRectToRect(this, dst, Matrix.ScaleToFit.FILL)
    return matrix
}

fun Rect.buildScaleMatrixTo(dst: Rect): Matrix {
    val matrix = Matrix()
    val rect1 = RectF(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())
    val rect2 = RectF(dst.left.toFloat(), dst.top.toFloat(), dst.right.toFloat(), dst.bottom.toFloat())
    return rect1.buildScaleMatrixTo(rect2)
}

fun Bitmap.buildRectF(): RectF = RectF(0f, 0f, width.toFloat(), height.toFloat())
fun Bitmap.buildRect(): Rect = Rect(0, 0, width, height)