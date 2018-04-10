package com.licht.vkpost.vkpost.data.model

data class StickerItem(var sticker: Sticker,
                       var left: Int,
                       var top: Int,
                       var width: Int,
                       var height: Int,
                       var angle: Float = 0f,
                       var scaleFactor: Double = 1.0) {
    fun moveOn(dx: Int, dy: Int) {
        left += dx
        top += dy
    }

    fun scaleOn(factor: Float) {
        if (scaleFactor == 0.0) {
            scaleFactor = factor.toDouble()
        } else
            scaleFactor *= factor
    }

    fun rotateOn(angle: Float) {
        if (this.angle == 0f)
            this.angle = angle
        else
            this.angle += angle
    }
}