package com.licht.vkpost.vkpost.utils

import android.support.v4.view.ViewCompat.setAlpha
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint


object RadialBlur {

    /**
     * use for 2D-Radial-Blur
     *
     * @param src
     * @param dx    It effetcs the speed. you can use 1/-1 for a test
     * @param dy    It effetcs the speed. you can use 1/-1 for a test
     * @param times effects motion length
     * @return
     */
    fun doRadialBlur(src: Bitmap, dx: Int, dy: Int, times: Int = 20): Bitmap {

        val dst = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(dst)
        val matrix = Matrix()
        val paint = Paint()
        canvas.drawBitmap(src, matrix, paint)

        paint.alpha = 51 // 51 / 255 =20%

        for (i in 0 until times) {
            matrix.setTranslate((i * dx).toFloat(), (i * dy).toFloat())
            canvas.drawBitmap(src, matrix, paint)
        }

        return dst
    }

    /**
     * use for Z-Radial-Blur
     *
     * @param src
     * @param centerX
     * @param centerY
     * @param factor  It effetcs the speed. you can use 0.01 for a test
     * @param times   effect motion length
     * @return
     */
    fun doRadialBlur(src: Bitmap, centerX: Int, centerY: Int, factor: Float, times: Int = 20): Bitmap {

        val dst = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(dst)
        val matrix = Matrix()
        val paint = Paint()
        canvas.drawBitmap(src, matrix, paint)
        paint.alpha = 51

        for (i in 0 until times) {
            matrix.setScale(i * factor + 1, i * factor + 1, centerX.toFloat(), centerY.toFloat())
            canvas.drawBitmap(src, matrix, paint)
        }
        return dst
    }
}
/**
 * use for 2D-Radial-Blur
 *
 * @param src
 * @param dx
 * @param dy
 * @return
 */
