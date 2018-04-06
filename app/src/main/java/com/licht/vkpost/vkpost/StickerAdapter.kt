package com.licht.vkpost.vkpost

import android.content.Context
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.licht.vkpost.vkpost.data.model.Sticker
import com.licht.vkpost.vkpost.view.IStickerSelector

class StickerAdapter(private val stickerView: IStickerSelector):
        RecyclerView.Adapter<StickerAdapter.StickerViewHolder>() {

    private lateinit var context: Context

    private val stickers = mutableListOf<Sticker>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StickerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_sticker, parent, false)
        context = parent.context
        val vh = StickerViewHolder(view)
        vh.itemView.setOnClickListener {
            stickerView.onStickerSelecter(stickers[vh.adapterPosition])
        }

        return vh
    }

    override fun onBindViewHolder(holder: StickerViewHolder, position: Int) {
        holder.bind(stickers[position])
    }

    override fun getItemCount(): Int = stickers.size

    fun setData(stickers: List<Sticker>) {
        this.stickers.clear()
        this.stickers.addAll(stickers)
        notifyDataSetChanged()
    }

    inner class StickerViewHolder(view: View): RecyclerView.ViewHolder(view) {
        private val ivSticker = view.findViewById<ImageView>(R.id.iv_sticket)

        fun bind(sticker: Sticker) {
            ivSticker.setImageBitmap(sticker.resource)
        }
    }
}