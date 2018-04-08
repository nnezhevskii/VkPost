package com.licht.vkpost.vkpost.view

import com.licht.vkpost.vkpost.data.model.BackgroundWrapper

interface IPostView {
    fun setBackground(background: BackgroundWrapper)
    fun onCommandClick(background: BackgroundWrapper)
}