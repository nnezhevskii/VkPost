package com.licht.vkpost.vkpost.data.model

data class StickerItem(var sticker: Sticker,
                       var left: Int,
                       var top: Int,
                       var width: Int,
                       var height: Int,
                       var angle: Float = 0f,
                       var scaleFactor: Double = 1.0)