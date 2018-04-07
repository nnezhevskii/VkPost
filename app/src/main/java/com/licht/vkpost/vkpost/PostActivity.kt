package com.licht.vkpost.vkpost

import android.graphics.*
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

        drawTrash()
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

    private var isTrashShowed: Boolean = false
    private var isTrashActivated: Boolean = false

    private var trashLeft = 0
    private var trashTop = 0
    private var trashRight = 0
    private var trashBottom = 0

    private fun drawTrash() {
        if (!isTrashShowed)
            return

        val bitmap = (ivPost.drawable as BitmapDrawable).bitmap
        val canvas = Canvas(bitmap)

        var xCenter: Float = 0f
        var yCenter: Float = 0f
        var rad: Float = 0f

        if (isTrashActivated) {

            xCenter = bitmap.width / 2f
            yCenter = bitmap.height - 150f
            rad = 167f
            trashLeft = xCenter.toInt()   - 125
            trashTop = yCenter.toInt()    - 125
            trashRight = xCenter.toInt()  + 125
            trashBottom = yCenter.toInt() + 125

        }
        else {
            xCenter = bitmap.width / 2f
            yCenter = bitmap.height - 150f

            trashLeft = xCenter.toInt() - 75
            trashTop = yCenter.toInt() - 75
            trashRight = xCenter.toInt() + 75
            trashBottom = yCenter.toInt() + 75

            rad = 100f
        }



        val closedTrashImage = BitmapFactory.decodeResource(applicationContext.resources, R.drawable.ic_fab_trash)
        val openedTrashImage = BitmapFactory.decodeResource(applicationContext.resources, R.drawable.ic_fab_trash_released)

        val paint = Paint()
        paint.color = Color.WHITE

        val src = Rect(0, 0, closedTrashImage.width, closedTrashImage.height)
        val dst = Rect(trashLeft, trashTop, trashRight, trashBottom)

        canvas.drawCircle(xCenter, yCenter, rad, paint)
        if (isTrashActivated)
            canvas.drawBitmap(openedTrashImage, src, dst, null)
        else
            canvas.drawBitmap(closedTrashImage, src, dst, null)


        ivPost.setImageBitmap(bitmap)
    }

    override fun selectItem(x: Int, y: Int) {
        selectedItem = getSelectedSticker(x, y)
        onItemSelected()
    }

    override fun releaseItem() {

        if (isTrashActivated && selectedItem != null) {
            stickers.remove(selectedItem!!)
        }

        selectedItem = null
        isTrashShowed = false

        redrawBackground()
    }

//    isTrashActivated

    override fun moveTo(actualX: Int, actualY: Int, dx: Int, dy: Int) {
        if (selectedItem == null)
            return

        val item = selectedItem!!
        item.left += dx
        item.top += dy

        isTrashActivated = isIntersectingWithTrashButton(actualX, actualY)

        redrawBackground()
    }

    fun isIntersectingWithTrashButton(actualX: Int, actualY: Int): Boolean {
        return actualX in trashLeft..trashRight &&
                actualY in trashTop..trashBottom
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

    private fun onItemSelected() {
        selectedItem?.let {
            stickers.remove(it)
            stickers.add(it)

            isTrashShowed = true
            redrawBackground()
        }


    }

}
