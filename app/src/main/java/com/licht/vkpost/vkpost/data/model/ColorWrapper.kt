package com.licht.vkpost.vkpost.data.model

import android.graphics.*
import android.util.TypedValue
import android.widget.ImageView
import com.licht.vkpost.vkpost.App
import com.licht.vkpost.vkpost.utils.getBrighterColor
import com.licht.vkpost.vkpost.utils.getLessColor


sealed class BackgroundWrapper {
    fun drawOn(imageView: ImageView, width: Int, height: Int) {
        imageView.setImageBitmap(buildBitmap(width, height))
    }
    abstract fun buildBitmap(width: Int, height: Int): Bitmap
}

fun dpToPx(dp: Int): Int {
    val r = App.application.getResources()
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), r.getDisplayMetrics()).toInt()
}

data class ColorWrapper(val color: Int) : BackgroundWrapper() {

    override fun buildBitmap(width: Int, height: Int): Bitmap {
        val color1 = getBrighterColor(color)
        val color2 = getLessColor(color)

        val gradient = LinearGradient(0f, 0f, width.toFloat(), height.toFloat(), color1, color2, Shader.TileMode.CLAMP)

        val p = Paint()
        p.isDither = true
        p.shader = gradient

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), p)

        return bitmap
    }
}

data class ImageWrapper(val bitmap: Bitmap) : BackgroundWrapper() {

    override fun buildBitmap(width: Int, height: Int): Bitmap {

        val backBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(backBitmap)

        val src = Rect(0, 0, bitmap.width, bitmap.height)
        val dst = Rect(0, 0, width, height)

        canvas.drawBitmap(bitmap, src, dst, null)

        return bitmap.copy(Bitmap.Config.ARGB_8888, true)
    }
}

data class CompoundImageWrapper(val background: Bitmap,
                                val top: Bitmap,
                                val bottom: Bitmap) : BackgroundWrapper() {

    override fun buildBitmap(width: Int, height: Int): Bitmap {

        val backBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(backBitmap)

        val srcBackground = RectF(0F, 0F, background.width.toFloat(), background.height.toFloat())
        val dstBackground = RectF(0F, 0F, width.toFloat(), height.toFloat())
        val matrixBack = Matrix()
        matrixBack.setRectToRect(srcBackground, dstBackground, Matrix.ScaleToFit.FILL)
        canvas.drawBitmap(background, matrixBack, null)


        val srcTop = RectF(0F, 0F, top.width.toFloat(), top.height.toFloat())
        val dstTop = RectF(0F, 0F, width.toFloat(), height.toFloat() / 3)
        val matrixTop = Matrix()
        matrixTop.setRectToRect(srcTop, dstTop, Matrix.ScaleToFit.FILL)
        canvas.drawBitmap(top, matrixTop, null)

        val srcBottom = RectF(0f, 0f, bottom.width.toFloat(), bottom.height.toFloat())
        val dstBottom = RectF(0f, 2f * height / 3, width.toFloat(), height.toFloat())

        val matrixBottom = Matrix()
        matrixBottom.setRectToRect(srcBottom, dstBottom, Matrix.ScaleToFit.FILL)
        canvas.drawBitmap(bottom, matrixBottom, null)

        return backBitmap
    }
}