package com.licht.vkpost.vkpost

import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.ImageView
import com.licht.vkpost.vkpost.data.model.ColorWrapper
import com.licht.vkpost.vkpost.data.model.Sticker
import com.licht.vkpost.vkpost.data.model.StickerItem
import com.licht.vkpost.vkpost.utils.getBrighterColor
import com.licht.vkpost.vkpost.utils.getLessColor
import com.licht.vkpost.vkpost.view.BottomSheetFragment
import com.licht.vkpost.vkpost.view.IPostView


class PostActivity : AppCompatActivity(), IPostView {
    private lateinit var ivPost: ImageView

    private var selectedItem: StickerItem? = null

    private lateinit var mScaleDetector: ScaleGestureDetector


    private fun getSelectedSticker(x: Float, y: Float): StickerItem? {
        return stickers.lastOrNull { stickerItem -> isTapOnSticker(stickerItem, x, y) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post)
        ivPost = findViewById(R.id.iv_post)

        mScaleDetector = ScaleGestureDetector(this, MyPinchListener())

        findViewById<ImageView>(R.id.iv_sticket).setOnClickListener {
            val bottomSheetFragment = BottomSheetFragment()
            bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)

        }

//        setBackground(ColorWrapper(ContextCompat.getColor(applicationContext, android.R.color.white)))

//        ivPost.setOnS

        ivPost.setOnTouchListener(View.OnTouchListener { view, motionEvent ->

            if (mScaleDetector.onTouchEvent(motionEvent))
                return@OnTouchListener true

            when (motionEvent.action) {

                MotionEvent.ACTION_DOWN -> {
                    selectedItem = getSelectedSticker(motionEvent.x, motionEvent.y)
                    Log.e("PostActivity", "select: " + (selectedItem?.sticker?.title
                            ?: "<nothing>"))
                }

                MotionEvent.ACTION_MOVE -> {
                    if (selectedItem == null)
                        return@OnTouchListener true

                    val item = selectedItem!!
                    item.left = motionEvent.x.toInt() - item.width / 2
                    item.top = motionEvent.y.toInt() - item.height / 2

                    redrawBackground()

                    Log.e("PostActivity", "move: " + (selectedItem?.sticker?.title ?: "<nothing>"))
                }

                MotionEvent.ACTION_UP -> {
                    selectedItem = null
                    Log.e("PostActivity", "release")
                }
            }

            return@OnTouchListener true
        })
    }

    fun isTapOnSticker(sticker: StickerItem, x: Float, y: Float): Boolean {
        return x >= sticker.left && y >= sticker.top &&
                x <= sticker.left + sticker.width && y <= sticker.top + sticker.height
    }

    override fun onStart() {
        super.onStart()

        val colors: MutableList<ColorWrapper> = mutableListOf<Int>(
                R.color.colorAccent, R.color.colorPrimary, R.color.colorPrimaryDark)
                .map { id -> ContextCompat.getColor(applicationContext, id) }
                .map { color -> ColorWrapper(color) }.toMutableList()
        colors.add(ColorWrapper(Color.RED))
        colors.add(ColorWrapper(Color.YELLOW))

        val rvBackground = findViewById<RecyclerView>(R.id.rv_background)
        val adapter = ColorAdapter(this);
        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvBackground.adapter = adapter
        rvBackground.layoutManager = layoutManager
        adapter.setData(colors)
    }

    fun redrawStickers() {
        val bitmap = (ivPost.drawable as BitmapDrawable).bitmap
        val canvas = Canvas(bitmap)

        stickers.forEach { sticker ->
            drawSticker(canvas, sticker)
//            canvas.drawBitmap(sticker.sticker.resource,
//                    sticker.left.toFloat(), sticker.top.toFloat(), null)
        }


        ivPost.setImageBitmap(bitmap)
    }

    private fun drawSticker(canvas: Canvas, sticker: StickerItem) {
        val src: Rect = Rect(0, 0, sticker.sticker.resource.width, sticker.sticker.resource.height)
        val dst: RectF = RectF(sticker.left.toFloat(), sticker.top.toFloat(),
                (sticker.left + sticker.width).toFloat(), (sticker.top + sticker.height).toFloat())
        canvas.drawBitmap(sticker.sticker.resource, src, dst, null)
    }

    var stickers: MutableList<StickerItem> = mutableListOf()

    fun addSticker(sticker: Sticker) {
        val bitmap = (ivPost.drawable as BitmapDrawable).bitmap
        val stickerItem = StickerItem(
                sticker,
                bitmap.width / 4, bitmap.height / 4,
                sticker.resource.width, sticker.resource.height)

        stickers.add(stickerItem)

        redrawStickers()

    }

    private lateinit var background: ColorWrapper
    override fun setBackground(background: ColorWrapper) {
        this.background = background
        redrawBackground()
    }

    private fun redrawBackground() {
        val bitmap = Bitmap.createBitmap(ivPost.width, ivPost.height, Bitmap.Config.ARGB_8888)
        ivPost.setImageBitmap(bitmap)

        val color1 = getBrighterColor(background.color)
        val color2 = getLessColor(background.color)

        val gd = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(color1, color2))
        gd.cornerRadius = 0f

        ivPost.background = gd

        redrawStickers()
    }

    inner class MyPinchListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {

        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            selectedItem = getSelectedSticker(detector.focusX, detector.focusY)
            Log.e("PostActivity", "onScaleBegin: select sticker " + (selectedItem?.sticker?.title
                    ?: "<nothing>"))
            return super.onScaleBegin(detector)
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            Log.e("PostActivity", "onScaleEnd: release")
            super.onScaleEnd(detector)
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            Log.d("TAG", "PINCH! OUCH!: " + detector.scaleFactor)
            if (selectedItem == null)
                return true
            val item = selectedItem!!

            val oldWidth = item.width
            val oldHeight = item.height

            item.width = (item.width * detector.scaleFactor).toInt()
            item.height = (item.height * detector.scaleFactor).toInt()

            val diffWidth = item.width - oldWidth
            val diffHeight = item.height - oldHeight

            item.left -= diffWidth / 2
            item.top -= diffHeight / 2

            redrawBackground()

            return true
        }
    }

}
