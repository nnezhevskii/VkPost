package com.licht.vkpost.vkpost.data.model

import android.graphics.*
import android.support.v4.content.ContextCompat
import android.util.Log
import android.widget.ImageView
import com.licht.vkpost.vkpost.App
import com.licht.vkpost.vkpost.R
import com.licht.vkpost.vkpost.utils.*

sealed class BackgroundWrapper {
    fun drawOn(imageView: ImageView, width: Int, height: Int) {
        imageView.setImageBitmap(buildBitmap(width, height))
    }

    open fun isCommandItem(): Boolean = false

    abstract fun buildBitmap(width: Int, height: Int): Bitmap

    public companion object {
        public fun getDefaultsBackgrounds(): List<BackgroundWrapper> {
            val context = App.application.applicationContext
            val backgrounds: MutableList<BackgroundWrapper> = mutableListOf(
                    ContextCompat.getColor(context, android.R.color.white),
                    ContextCompat.getColor(context, R.color.colorGreen),
                    ContextCompat.getColor(context, R.color.colorOrange),
                    ContextCompat.getColor(context, R.color.colorDarkPink),
                    ContextCompat.getColor(context, R.color.colorLilac)
            ).map { color -> ColorWrapper(color) }.toMutableList()
            backgrounds.add(ImageWrapper(getBitmapFromResoure(context.resources, R.drawable.bg_stars_center)))
            backgrounds.add(CompoundImageWrapper(
                    getBitmapFromResoure(context.resources, R.drawable.bg_beach_center),
                    getBitmapFromResoure(context.resources, R.drawable.bg_beach_top),
                    getBitmapFromResoure(context.resources, R.drawable.bg_beach_bottom)))

            backgrounds.add(AddImageCommandWrapper())

            return backgrounds
        }
    }
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
        canvas.drawBitmap(bitmap, bitmap.buildRect(), Rect(0, 0, width, height), null)

        return bitmap.copy(Bitmap.Config.ARGB_8888, true)
    }
}

data class CompoundImageWrapper(val background: Bitmap,
                                val top: Bitmap,
                                val bottom: Bitmap) : BackgroundWrapper() {
    override fun buildBitmap(width: Int, height: Int): Bitmap {
        val backBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(backBitmap)

        val dst = RectF(0F, 0F, width.toFloat(), height.toFloat())
        val dstTop = RectF(0F, 0F, width.toFloat(), height.toFloat() / 3)
        val dstBottom = RectF(0f, 2f * height / 3, width.toFloat(), height.toFloat())

        canvas.drawBitmap(background, background.buildRectF().buildScaleMatrixTo(dst), null)
        canvas.drawBitmap(top, top.buildRectF().buildScaleMatrixTo(dstTop), null)
        canvas.drawBitmap(bottom, bottom.buildRectF().buildScaleMatrixTo(dstBottom), null)

        return backBitmap
    }
}

class AddImageCommandWrapper: BackgroundWrapper() {
    override fun buildBitmap(width: Int, height: Int): Bitmap {
        val backBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(backBitmap)

        val context = App.application.applicationContext
        val bitmap: Bitmap = getBitmapFromResoure(context.resources, R.drawable.ic_toolbar_new)

        canvas.drawColor(getColorByRes(R.color.colorAddBackground))
        canvas.drawBitmap(bitmap, bitmap.buildRect(), Rect(0, 0, width, height), null)

        return backBitmap.copy(Bitmap.Config.ARGB_8888, true)
    }

    override fun isCommandItem() = true
}