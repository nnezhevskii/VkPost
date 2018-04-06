package com.licht.vkpost.vkpost

import android.graphics.Color
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.licht.vkpost.vkpost.data.model.ColorWrapper
import android.graphics.drawable.GradientDrawable
import com.licht.vkpost.vkpost.utils.getBrighterColor
import com.licht.vkpost.vkpost.utils.getLessColor
import com.licht.vkpost.vkpost.view.IPostView


class ColorAdapter(private val postView: IPostView) : RecyclerView.Adapter<ColorAdapter.ColorViewHolder>() {

    private val item: MutableList<ColorWrapper> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_background_color, parent, false)
        val cvh = ColorViewHolder(view);
        cvh.view.setOnClickListener{
            postView.setBackground(item[cvh.adapterPosition])
        }
        return cvh
    }

    override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
        return holder.bind(item[position])
    }

    override fun getItemCount(): Int = item.size

    fun setData(items: List<ColorWrapper>) {
        this.item.clear()
        this.item.addAll(items)
        notifyDataSetChanged()
    }

    class ColorViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        fun bind(colorWrapper: ColorWrapper) {
//            view.findViewById<View>(R.id.item).setBackgroundColor(colorWrapper.color)

            val color1 = getBrighterColor(colorWrapper.color)
            val color2 = getLessColor(colorWrapper.color)

            val gd = GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    intArrayOf(color1, color2))
            gd.cornerRadius = 0f

            view.findViewById<View>(R.id.item).background = gd
//            gd.

        }
    }
}
