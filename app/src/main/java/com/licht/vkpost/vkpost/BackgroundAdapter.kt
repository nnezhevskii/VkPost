package com.licht.vkpost.vkpost

import android.content.Context
import android.graphics.*
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.licht.vkpost.vkpost.data.model.BackgroundWrapper
import com.licht.vkpost.vkpost.utils.buildRectF
import com.licht.vkpost.vkpost.utils.buildScaleMatrixTo
import com.licht.vkpost.vkpost.utils.dpToPx
import com.licht.vkpost.vkpost.view.IPostView


class BackgroundAdapter(private val postView: IPostView) : RecyclerView.Adapter<BackgroundAdapter.ColorViewHolder>() {

    private var selectedPosition: Int? = null

    private val items: MutableList<BackgroundWrapper> = mutableListOf()
    private lateinit var context: Context
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_background_color, parent, false)
        val cvh = ColorViewHolder(view);
        cvh.view.setOnClickListener{
            val item = items[cvh.adapterPosition]
            if (item.isCommandItem()) {
                postView.onCommandClick(item)
            }
            else {
                selectedPosition = cvh.adapterPosition
                postView.setBackground(items[cvh.adapterPosition])
                onItemSelected()
            }

        }
        context = parent.context
        return cvh
    }
    private var recyclerView: RecyclerView? = null
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView

        recyclerView.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
                onItemSelected()
            }
        })
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        this.recyclerView = null
    }

    private fun onItemSelected() {
        val position = selectedPosition ?: -1
        val layoutManager: LinearLayoutManager = recyclerView?.layoutManager as? LinearLayoutManager ?: return

        for (i in layoutManager.findFirstVisibleItemPosition()..layoutManager.findLastVisibleItemPosition()) {
            val vh: ColorViewHolder? = recyclerView!!.findViewHolderForAdapterPosition(i) as? ColorViewHolder?
            vh?.updateSelection(i == position)
        }

    }

    override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
        holder.bind(items[position])
        onItemSelected()
    }

    override fun getItemCount(): Int = items.size

    fun setData(items: List<BackgroundWrapper>) {
        this.items.clear()
        this.items.addAll(items)
        notifyDataSetChanged()
    }

    inner class ColorViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        private var backgroundWrapper: BackgroundWrapper? = null
        private val item = view.findViewById<RoundedCornerImageView>(R.id.item)

        fun bind(backWrapper: BackgroundWrapper) {
            this.backgroundWrapper = backWrapper
            draw()
        }

        fun draw() {

            val size = context.resources.getDimension(R.dimen.background_item_size).toInt()

            if (backgroundWrapper == null)
                return

            val backWrapper = backgroundWrapper!!

            val isSelected = item.isSelectedItem
            if (isSelected) {
                val backBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(backBitmap)

                val myPaint = Paint()
                myPaint.color = ContextCompat.getColor(context, R.color.colorBackgroundFrame)
                myPaint.style = Paint.Style.STROKE;

                val outerPadding = context.resources.getDimension(R.dimen.padding_background_item_outer)
                myPaint.strokeWidth = outerPadding

                val rect: RectF = RectF(outerPadding, outerPadding, canvas.width - outerPadding, canvas.height - outerPadding)
                canvas.drawRoundRect(rect, outerPadding, outerPadding, myPaint)

                item.setImageBitmap(backBitmap)

                val innerPadding = context.resources.getDimension(R.dimen.padding_background_item_inner)
                val bitmap = backWrapper.buildBitmap(size, size)

                val dst = RectF(innerPadding, innerPadding, canvas.width - innerPadding, canvas.height - innerPadding)

                canvas.drawBitmap(bitmap, bitmap.buildRectF().buildScaleMatrixTo(dst), null)
            }

            else {
                backWrapper.drawOn(item, size, size)
            }

        }

        fun updateSelection(isSelected: Boolean) {
            view.findViewById<RoundedCornerImageView>(R.id.item).isSelectedItem = isSelected
            draw()
        }
    }
}
