package com.licht.vkpost.vkpost

import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.widget.ImageView
import com.licht.vkpost.vkpost.data.model.*
import com.licht.vkpost.vkpost.utils.buildMatrix
import com.licht.vkpost.vkpost.utils.isTapOnSticker
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

    private fun getSelectedSticker(x: Int, y: Int): StickerItem? {
        return stickers.lastOrNull { stickerItem -> isTapOnSticker(stickerItem, x, y) }
    }


    private fun getBitmapFromResoure(id: Int): Bitmap = BitmapFactory.decodeResource(resources, id)

    override fun onPostResume() {
        super.onPostResume()

        val colors: MutableList<BackgroundWrapper> = mutableListOf<Int>(
                R.color.colorAccent, R.color.colorPrimary, R.color.colorPrimaryDark)
                .map { id -> ContextCompat.getColor(applicationContext, id) }
                .map { color -> ColorWrapper(color) }.toMutableList()
        colors.add(ColorWrapper(Color.RED))
        colors.add(ColorWrapper(Color.YELLOW))
        colors.add(ImageWrapper(getBitmapFromResoure(R.drawable.bg_stars_center)))

        colors.add(CompoundImageWrapper(
                getBitmapFromResoure(R.drawable.bg_beach_center),
                getBitmapFromResoure(R.drawable.bg_beach_top),
                getBitmapFromResoure(R.drawable.bg_beach_bottom)
        ))

        initializeTrashIcons()

        val rvBackground = findViewById<RecyclerView>(R.id.rv_background)
        val adapter = BackgroundAdapter(this);
        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvBackground.adapter = adapter
        rvBackground.layoutManager = layoutManager
        adapter.setData(colors)
    }

//    fun redrawStickers() {
//        val bitmap = (ivPost.drawable as BitmapDrawable).bitmap
//        val canvas = Canvas(bitmap)
//
//        stickers.forEach { sticker -> drawSticker(canvas, sticker) }
//
//
//        ivPost.setImageBitmap(bitmap)
//
//        drawTrash()
//    }


    fun addSticker(sticker: Sticker) {
        val bitmap = (ivPost.drawable as BitmapDrawable).bitmap
        val stickerItem = StickerItem(
                sticker,
                bitmap.width / 4, bitmap.height / 4,
                sticker.resource.width, sticker.resource.height)

        stickers.add(stickerItem)

        redraw()

    }

    private lateinit var background: Bitmap
    override fun setBackground(background: BackgroundWrapper) {
        this.background = background.buildBitmap(ivPost.width, ivPost.height)
        redraw()
    }


    private var isTrashShowed: Boolean = false
    private var isTrashActivated: Boolean = false

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

        redraw()
    }

//    isTrashActivated

    override fun moveTo(actualX: Int, actualY: Int, dx: Int, dy: Int) {
        if (selectedItem == null)
            return

        val item = selectedItem!!
        item.left += dx
        item.top += dy

        isTrashActivated = isIntersectingWithTrashButton(actualX, actualY)

        redraw()
    }

    fun isIntersectingWithTrashButton(actualX: Int, actualY: Int): Boolean {
        return actualX in trashLocation.left..trashLocation.right &&
                actualY in trashLocation.top..trashLocation.bottom
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


        redraw()

    }

    private fun onItemSelected() {
        selectedItem?.let {
            stickers.remove(it)
            stickers.add(it)

            isTrashShowed = true
            redraw()
        }

    }

    private fun redraw() {
        if ((ivPost.drawable as? BitmapDrawable)?.bitmap == null) {
            val bitmap = Bitmap.createBitmap(ivPost.width, ivPost.height, Bitmap.Config.ARGB_8888)
            ivPost.setImageBitmap(bitmap)
        }


        val canvas = Canvas((ivPost.drawable as BitmapDrawable).bitmap)
        redrawBackground(canvas)
        redrawStickers(canvas)
        redrawTrash(canvas)

        ivPost.invalidate()
    }

    private fun redrawBackground(canvas: Canvas) {
        canvas.drawBitmap(background, 0f, 0f, null)
    }

    private fun redrawStickers(canvas: Canvas) {
        stickers.forEach { sticker -> drawSticker(canvas, sticker) }
    }

    private lateinit var closedTrashBitmap: Bitmap
    private lateinit var openedTrashBitmap: Bitmap

    private lateinit var trashLocation: RectF

    private fun redrawTrash(canvas: Canvas) {
        if (!isTrashShowed)
            return

        val bottomPadding = resources.getDimension(R.dimen.trash_padding_bottom)

        if (isTrashActivated) {
            val left = ivPost.width / 2f - openedTrashBitmap.width / 2f
            val top = ivPost.height - openedTrashBitmap.height.toFloat() - bottomPadding

            canvas.drawBitmap(openedTrashBitmap, left, top, null)
        } else {

            val left = ivPost.width / 2f - closedTrashBitmap.width / 2f
            val top = ivPost.height - closedTrashBitmap.height.toFloat() - bottomPadding
            val right = left + closedTrashBitmap.width
            val bottom = top + closedTrashBitmap.height.toFloat()

            trashLocation = RectF(left, top, right, bottom)

            canvas.drawBitmap(closedTrashBitmap, ivPost.width / 2f - closedTrashBitmap.width / 2f,
                    ivPost.height - closedTrashBitmap.height.toFloat() - bottomPadding, null)
        }
    }

    private fun initializeTrashIcons() {
        val radius = resources.getDimension(R.dimen.closed_trash_radius)
        closedTrashBitmap = buildTrashBitmap(radius)
        openedTrashBitmap = buildTrashBitmap(radius * 1.1f)

    }

    private fun buildTrashBitmap(radius: Float): Bitmap {

        val bitmap = Bitmap.createBitmap(2 * radius.toInt(), 2 * radius.toInt(), Bitmap.Config.ARGB_8888)
        val innerPadding = resources.getDimension(R.dimen.trash_inner_padding)
        val paintWhite = Paint()
        paintWhite.color = Color.WHITE

        val closedTrashImage = BitmapFactory.decodeResource(applicationContext.resources, R.drawable.ic_fab_trash)
        val src = RectF(0f, 0f, closedTrashImage.width.toFloat(), closedTrashImage.height.toFloat())
        val dst = RectF(innerPadding, innerPadding, bitmap.width - innerPadding.toFloat(), bitmap.height - innerPadding.toFloat())
        val mat = Matrix()
        mat.setRectToRect(src, dst, Matrix.ScaleToFit.FILL)
        with(Canvas(bitmap)) {
            drawCircle(radius, radius, radius, paintWhite)
            drawBitmap(closedTrashImage, mat, null)
        }

        return bitmap
    }

}

private fun drawSticker(canvas: Canvas, sticker: StickerItem) = with(sticker.sticker.resource) {
    canvas.drawBitmap(this, buildMatrix(this, sticker), null)
}


