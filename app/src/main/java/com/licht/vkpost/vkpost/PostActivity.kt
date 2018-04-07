package com.licht.vkpost.vkpost

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.widget.ImageView
import com.licht.vkpost.vkpost.data.model.ColorWrapper
import com.licht.vkpost.vkpost.data.model.Sticker
import com.licht.vkpost.vkpost.data.model.StickerItem
import com.licht.vkpost.vkpost.utils.getBrighterColor
import com.licht.vkpost.vkpost.utils.getLessColor
import com.licht.vkpost.vkpost.view.BottomSheetFragment
import com.licht.vkpost.vkpost.view.GestureHelper
import com.licht.vkpost.vkpost.view.IPostView
import com.licht.vkpost.vkpost.view.ItemManipulator


class PostActivity : AppCompatActivity(), IPostView, ItemManipulator {
    private lateinit var ivPost: ImageView

    var stickers: MutableList<StickerItem> = mutableListOf()
    private var selectedItem: StickerItem? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post)
        ivPost = findViewById(R.id.iv_post)

        findViewById<ImageView>(R.id.iv_sticket).setOnClickListener {
            val bottomSheetFragment = BottomSheetFragment()
            bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)

        }

        val gestureHelper = GestureHelper(ivPost)
        gestureHelper.addListener(this)

    }

    fun isTapOnSticker(sticker: StickerItem, x: Int, y: Int): Boolean {
        return x >= sticker.left && y >= sticker.top &&
                x <= sticker.left + sticker.width && y <= sticker.top + sticker.height
    }

    private fun getSelectedSticker(x: Int, y: Int): StickerItem? {
        return stickers.lastOrNull { stickerItem -> isTapOnSticker(stickerItem, x, y) }
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

        stickers.forEach { sticker -> drawSticker(canvas, sticker) }


        ivPost.setImageBitmap(bitmap)
    }

    private fun drawSticker(canvas: Canvas, sticker: StickerItem) = with(sticker.sticker.resource) {
        canvas.drawBitmap(this, buildMatrix(this, sticker), null)
    }

    private fun buildMatrix(bitmap: Bitmap, sticker: StickerItem): Matrix {

        val dx = bitmap.width * sticker.scaleFactor.toFloat() - bitmap.width
        val dy = bitmap.height * sticker.scaleFactor.toFloat() - bitmap.height

        val matrix = Matrix()
        matrix.postRotate(sticker.angle, bitmap.width / 2f, bitmap.height / 2f)
        matrix.postScale(sticker.scaleFactor.toFloat(), sticker.scaleFactor.toFloat())
        matrix.postTranslate(sticker.left.toFloat() - dx / 2, sticker.top.toFloat() - dy / 2)

        return matrix
    }

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

    override fun selectItem(x: Int, y: Int) {
        selectedItem = getSelectedSticker(x, y)
    }

    override fun releaseItem() {
        selectedItem = null
    }

    override fun moveTo(actualX: Int, actualY: Int, dx: Int, dy: Int) {
        if (selectedItem == null)
            return

        val item = selectedItem!!
        item.left += dx
        item.top += dy

        redrawBackground()
    }

    override fun scale(factor: Float, angle: Float) {
        if (selectedItem == null)
            return

        val item = selectedItem!!

        if (item.angle == 0f)
            item.angle = angle
        else
            item.angle += angle


        if (item.scaleFactor == 0.0) {
            item.scaleFactor = factor.toDouble()
        } else
            item.scaleFactor *= factor


        redrawBackground()

    }

}
