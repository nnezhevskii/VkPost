package com.licht.vkpost.vkpost.data.model

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.support.v4.content.ContextCompat
import com.licht.vkpost.vkpost.R

//data class Sticker(val drawable: Drawable)
data class Sticker(val title: String,
                   val resource: Bitmap)
{

    companion object {
        private var stickers: List<Sticker>? = null
        fun getStickers(context: Context): List<Sticker> {
            if (stickers != null)
                return stickers!!
            val ids = listOf<Int>(
                    R.drawable.sticker1
                    , R.drawable.sticker2, R.drawable.sticker3, R.drawable.sticker4
                    ,R.drawable.sticker5, R.drawable.sticker6, R.drawable.sticker7, R.drawable.sticker8
                    ,R.drawable.sticker9, R.drawable.sticker10, R.drawable.sticker11, R.drawable.sticker12
                    ,R.drawable.sticker13, R.drawable.sticker14, R.drawable.sticker15, R.drawable.sticker16
                    ,R.drawable.sticker17, R.drawable.sticker18, R.drawable.sticker19, R.drawable.sticker20
                    ,R.drawable.sticker21, R.drawable.sticker22, R.drawable.sticker23, R.drawable.sticker24
            )

            var index = 1
            stickers = ids.map { id -> Sticker((index++).toString(), BitmapFactory.decodeResource(context.resources, id)) }
//            stickers = ids.map {id -> ContextCompat.getDrawable(context,id) }
//                    .map { image -> Sticker(image) }
            val options = BitmapFactory.Options()


            val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.sticker1)
            return stickers!!
        }
    }

}