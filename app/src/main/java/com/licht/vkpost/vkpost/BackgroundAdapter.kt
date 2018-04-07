package com.licht.vkpost.vkpost

import android.content.Context
import android.graphics.*
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.licht.vkpost.vkpost.data.model.BackgroundWrapper
import com.licht.vkpost.vkpost.data.model.dpToPx
import com.licht.vkpost.vkpost.view.IPostView


class BackgroundAdapter(private val postView: IPostView) : RecyclerView.Adapter<BackgroundAdapter.ColorViewHolder>() {

    private var selectedPosition: Int? = null


    private val item: MutableList<BackgroundWrapper> = mutableListOf()
    private lateinit var context: Context
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_background_color, parent, false)
        val cvh = ColorViewHolder(view);
        cvh.view.setOnClickListener{
            selectedPosition = cvh.adapterPosition
            postView.setBackground(item[cvh.adapterPosition])
            onItemSelected()
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
        holder.bind(item[position])
        onItemSelected()
        Log.e("BA", "onBindViewHolder #${position}")
    }

    override fun getItemCount(): Int = item.size

    fun setData(items: List<BackgroundWrapper>) {
        this.item.clear()
        this.item.addAll(items)
        notifyDataSetChanged()
    }

    inner class ColorViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        private var backgroundWrapper: BackgroundWrapper? = null
        fun bind(backWrapper: BackgroundWrapper) {
            this.backgroundWrapper = backWrapper
            draw()
        }

        fun draw() {
            if (backgroundWrapper == null)
                return

            val backWrapper = backgroundWrapper!!

            val isSelected = view.findViewById<RoundedCornerLayout>(R.id.item).isSelectedItem
            if (isSelected) {
                val backBitmap = Bitmap.createBitmap(dpToPx(40), dpToPx(40), Bitmap.Config.ARGB_8888)
                val canvas = Canvas(backBitmap)

                val myPaint = Paint()
                myPaint.setColor(Color.RED)
                myPaint.setStyle(Paint.Style.STROKE);

                myPaint.setStrokeWidth(10f)

                val rect: RectF = RectF(10f, 10f, canvas.width - 10f, canvas.height - 10f)
                canvas.drawRoundRect(rect, 10f, 10f, myPaint)


                view.findViewById<ImageView>(R.id.item).setImageBitmap(backBitmap)

                val bitmap = backWrapper.buildBitmap(dpToPx(40), dpToPx(40))

                val src = RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
                val dst = RectF(25f, 25f, canvas.width - 25.toFloat(), canvas.height - 25.toFloat())

                val matrix = Matrix()
                matrix.setRectToRect(src, dst, Matrix.ScaleToFit.FILL)
                canvas.drawBitmap(bitmap, matrix, null)
            }

            else {
                view.findViewById<ImageView>(R.id.item).setImageDrawable(ContextCompat.getDrawable(context, R.drawable.sticker1))
                backWrapper.drawOn(view.findViewById<ImageView>(R.id.item), dpToPx(40), dpToPx(40))
            }

        }

        fun updateSelection(isSelected: Boolean) {
            view.findViewById<RoundedCornerLayout>(R.id.item).isSelectedItem = isSelected
            draw()
            Log.e("BackgroundAdapter", "For VH#${adapterPosition} isSelected: ${isSelected}")
        }
    }
}
