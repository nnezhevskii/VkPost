package com.licht.vkpost.vkpost.view

import com.licht.vkpost.vkpost.data.model.Sticker

interface IStickerSelector {
    fun onStickerSelecter(sticker: Sticker)
}