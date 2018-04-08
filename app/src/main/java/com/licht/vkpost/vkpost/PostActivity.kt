package com.licht.vkpost.vkpost

import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
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

    private lateinit var blurScript: ScriptIntrinsicBlur
    private lateinit var script: RenderScript

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post)
        ivPost = findViewById(R.id.iv_post)

        findViewById<ImageView>(R.id.iv_sticket).setOnClickListener {
            val bottomSheetFragment = BottomSheetFragment()
            bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)

        }

        findViewById<ImageView>(R.id.iv_change_font).setOnClickListener {
            mode = (mode + 1) % 3
            redraw()
        }

        GestureHelper(ivPost).addListener(this)

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

//        final Allocation input = Allocation.createFromBitmap(renderScript, src);
//        final Allocation output = Allocation.createTyped(renderScript, input.getType());
        script = RenderScript.create(applicationContext)
        blurScript = ScriptIntrinsicBlur.create(script, Element.U8_4(script));

    }

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
//        redrawTrash(canvas)
        redrawText(canvas)


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

    private var mode = 0

    private fun redrawText(canvas: Canvas) {
        val lines: List<String> = listOf<String>(
                "Отправляюсь в отпуск",
                "до 10 сентября. Отвечать",
                "буду медленно. Если что-",
                "то срочное, упоминайте в",
                "беседах или звоните.")

        if (mode == 0)
            drawTextInMode1(canvas, lines)
        else if (mode == 1)
            drawTextInMode2(canvas, lines)
        else if (mode == 2)
            drawTextInMode3(canvas, lines)
        else
            throw IllegalStateException("invalid mode")

//        val height = 24 * resources.displayMetrics.density
//
//        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
//        paint.color = Color.BLACK
//        paint.textSize = height;
//
//        val textCenterX = canvas.width / 2
//        val textCenterY = canvas.height / 2
//
//        var startY = textCenterY - (lines.size / 2) * height
//
//        val myPaint = Paint()
//        myPaint.setColor(Color.WHITE)
//
//        for (i in 0..lines.size - 1) {
//            val lineWidth = paint.measureText(lines[i])
//
//            val rect: RectF = RectF(textCenterX - lineWidth / 2 - 20,
//                    startY - height / 2 - 25,
//                    textCenterX + lineWidth / 2 + 20,
//                    startY + height / 2)
//
//            canvas.drawRoundRect(rect, 20f, 20f, myPaint)
//
//            canvas.drawText(lines[i], textCenterX - lineWidth / 2.toFloat(), startY, paint)
//            startY += height
//        }

//        val scale = resources.displayMetrics.density

//
//        paint.textSize = height;
//
//        val text = "Отправлять в отпуск"
//        val width = paint.measureText(text)
//
//
//
//        val rect: RectF = RectF(canvas.width / 2 - width / 2 - 10,
//                canvas.height / 2f - height - 20,
//                canvas.width / 2 + width / 2 + 10,
//                canvas.height / 2f + 20
//                )
//

//
//        canvas.drawText(text, canvas.width / 2 - width / 2, canvas.height / 2f - height / 2, paint)
    }

    private fun drawText(canvas: Canvas, lines: List<String>, textPaint: Paint, backPaint: Paint? = null) {
        val height = 24 * resources.displayMetrics.density
        val paddingBetweenLine = 4 * resources.displayMetrics.density

        val textCenterX = canvas.width / 2
        val textCenterY = canvas.height / 2

        var startY = textCenterY - (lines.size / 2) * height


        for (i in 0 until lines.size) {
            val lineWidth = textPaint.measureText(lines[i])

            backPaint?.let {
                val rect: RectF = RectF(textCenterX - lineWidth / 2 - 20,
                        startY - height / 2 - 25,
                        textCenterX + lineWidth / 2 + 20,
                        startY + height / 2 + paddingBetweenLine)

                canvas.drawRoundRect(rect, 20f, 20f,it)
            }

            canvas.drawText(lines[i], textCenterX - lineWidth / 2.toFloat(), startY, textPaint)
            startY += height + paddingBetweenLine
        }
    }

    private fun drawTextInMode1(canvas: Canvas, lines: List<String>) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.WHITE
        paint.textSize = 24 * resources.displayMetrics.density;

        drawText(canvas, lines, paint)
    }

    private fun drawTextInMode2(canvas: Canvas, lines: List<String>) {
        val height = 24 * resources.displayMetrics.density

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.BLACK
        paint.textSize = height;

        val myPaint = Paint()
        myPaint.setColor(Color.WHITE)

        drawText(canvas, lines, paint, myPaint)
    }

    private fun drawTextInMode3(canvas: Canvas, lines: List<String>) {
        val height = 24 * resources.displayMetrics.density

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.WHITE
        paint.textSize = height;

        val myPaint = Paint()
        myPaint.setColor(Color.argb(64, 255, 255, 255))

        drawText(canvas, lines, paint, myPaint)
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

    private fun drawSticker(canvas: Canvas, sticker: StickerItem) = with(sticker.sticker.resource) {
//        val paint = Paint()
////    paint.maskFilter = BlurMaskFilter(5f, BlurMaskFilter.Blur.NORMAL)
//
//        val input = Allocation.createFromBitmap(script, this);
//        val output = Allocation.createTyped(script, input.getType());
//
//        val src = Bitmap.createBitmap(this)
//
//        blurScript.setRadius(8f);
//        blurScript.setInput(input);
//        blurScript.forEach(output);
//        output.copyTo(src)

        canvas.drawBitmap(this, buildMatrix(this, sticker), null)
    }


}

