package com.licht.vkpost.vkpost

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.os.PersistableBundle
import android.provider.MediaStore
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.InputType
import android.view.ViewTreeObserver
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RelativeLayout
import com.licht.vkpost.vkpost.data.model.*
import com.licht.vkpost.vkpost.utils.*
import com.licht.vkpost.vkpost.view.BottomSheetFragment
import com.licht.vkpost.vkpost.view.GestureHelper
import com.licht.vkpost.vkpost.view.IPostView
import com.licht.vkpost.vkpost.view.ItemManipulator
import java.util.*


class PostActivity : AppCompatActivity(), IPostView, ItemManipulator {
    private lateinit var ivPost: ImageView

    var stickers: MutableList<StickerItem> = mutableListOf()
    private var selectedItem: StickerItem? = null

    private lateinit var blurScript: ScriptIntrinsicBlur
    private lateinit var script: RenderScript

    private var inputedString: String = ""
    private lateinit var editText: EditText

    private var isTrashShowed: Boolean = false
    private var isTrashActivated: Boolean = false

    private var mode = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post)
        ivPost = findViewById(R.id.iv_post)

        findViewById<ImageView>(R.id.iv_sticket).setOnClickListener {
            val bottomSheetFragment = BottomSheetFragment()
            bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)

        }
        initializeTrashIcons()
        initUI()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && background == null) {
            setBackground(BackgroundWrapper.getDefaultsBackgrounds().first())
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            WRITE_EXTERNAL_STORAGE_REQUEST_CODE ->
                if (resultCode == Activity.RESULT_OK)
                    saveImage(applicationContext, getImageForExport(), "post-${Date()}")

            PICK_IMAGE -> {
                data?.let {
                    background = MediaStore.Images.Media.getBitmap(this.contentResolver, data.data)
                    redraw()
                }
            }

            else ->
                super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun getSelectedSticker(x: Int, y: Int): StickerItem? {
        return stickers.lastOrNull { stickerItem -> isTapOnSticker(stickerItem, x, y) }
    }


    private var oldHeightDiff = 0
    override fun onResume() {
        super.onResume()
        val rootLayout = findViewById<RelativeLayout>(R.id.root_layout)
        val keyboardLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
            val heightDiff = rootLayout.rootView.height - rootLayout.height;

            if (oldHeightDiff != heightDiff) {
                ivPost.setImageDrawable(null)
                oldHeightDiff = heightDiff
                redraw()
            }
        }
        rootLayout.viewTreeObserver.addOnGlobalLayoutListener(keyboardLayoutListener)
    }

    override fun onPostResume() {
        super.onPostResume()


        script = RenderScript.create(applicationContext)
        blurScript = ScriptIntrinsicBlur.create(script, Element.U8_4(script));

    }

    private fun initUI() {
        val rvBackground = findViewById<RecyclerView>(R.id.rv_background)
        val adapter = BackgroundAdapter(this);
        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvBackground.adapter = adapter
        rvBackground.layoutManager = layoutManager
        adapter.setData(BackgroundWrapper.getDefaultsBackgrounds())

        editText = findViewById<EditText>(R.id.ed_hided)


        editText.afterTextChanged {
            inputedString = it
            redraw()
        }

        findViewById<ImageView>(R.id.iv_change_font).setOnClickListener {
            mode = (mode + 1) % 3
            redraw()
        }

        findViewById<Button>(R.id.btn_save).setOnClickListener {
            if (appHasWriteExternalStoragePermission(applicationContext))
                saveImage(applicationContext, getImageForExport(), "post-${Date()}")
            else
                requestPermission(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        WRITE_EXTERNAL_STORAGE_REQUEST_CODE)
        }

        GestureHelper(ivPost).addListener(this)

    }

    fun addSticker(sticker: Sticker) {
        val bitmap = getBitmap()
        stickers.add(StickerItem(sticker, bitmap.width / 4, bitmap.height / 4,
                sticker.resource.width, sticker.resource.height))

        redraw()

    }

    private val PICK_IMAGE = 1

    private fun getBitmap() = (ivPost.drawable as BitmapDrawable).bitmap

    private var background: Bitmap? = null
    override fun setBackground(background: BackgroundWrapper) {
        this.background = background.buildBitmap(ivPost.width, ivPost.height)
        redraw()
    }



    override fun onCommandClick(background: BackgroundWrapper) {
        startActivityForResult(buildIntentForPickImage(), PICK_IMAGE)
    }

    override fun selectItem(x: Int, y: Int) {
        selectedItem = getSelectedSticker(x, y)
        selectedItem?.let { onItemSelected() } ?: openKeyboard(applicationContext, editText)
    }


    override fun releaseItem() {

        if (isTrashActivated && selectedItem != null) {
            stickers.remove(selectedItem!!)
        }

        selectedItem = null
        isTrashShowed = false

        redraw()
    }

    override fun moveTo(actualX: Int, actualY: Int, dx: Int, dy: Int) = selectedItem?.let {
        it.moveOn(dx, dy)
        isTrashActivated = isIntersectingWithRectangle(trashLocation, actualX, actualY)

        isTrashShowed = true

        redraw()
    } ?: Unit


    override fun scaleAndRotate(factor: Float, angle: Float) = selectedItem?.let {
        it.rotateOn(angle)
        it.scaleOn(factor)

        redraw()
    } ?: Unit

    private fun onItemSelected() {
        selectedItem?.let {
            stickers.remove(it)
            stickers.add(it)

            redraw()
        }
    }

    private fun redraw() {
        if ((ivPost.drawable as? BitmapDrawable)?.bitmap == null) {
            val bitmap = Bitmap.createBitmap(ivPost.width, ivPost.height, Bitmap.Config.ARGB_8888)
            ivPost.setImageBitmap(bitmap)
        }

        draw((ivPost.drawable as BitmapDrawable).bitmap)
    }

    private fun getImageForExport(): Bitmap {
        val bitmap = Bitmap.createBitmap(ivPost.width, ivPost.height, Bitmap.Config.ARGB_8888)
        draw(bitmap, true)

        return bitmap
    }

    private fun draw(bitmap: Bitmap, drawForExport: Boolean = false) {
        val canvas = Canvas(bitmap)

        redrawBackground(canvas)
        redrawStickers(canvas)

        if (!drawForExport)
            redrawTrash(canvas)

        redrawText(canvas, drawForExport)

        ivPost.invalidate()
    }

    private fun redrawBackground(canvas: Canvas) {
        background?.let {
            val src = RectF(0f, 0f, it.width.toFloat(), it.height.toFloat())
            val dst = RectF(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat())
            val mat = Matrix()
            mat.setRectToRect(src, dst, Matrix.ScaleToFit.FILL)
            canvas.drawBitmap(it, mat, null)
        }

    }

    private fun redrawStickers(canvas: Canvas) {
        stickers.forEach { sticker -> drawSticker(canvas, sticker) }
    }

    private lateinit var closedTrashBitmap: Bitmap
    private lateinit var openedTrashBitmap: Bitmap

    private var trashLocation: RectF? = null

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

    private fun redrawText(canvas: Canvas, drawForExport: Boolean = false) {

        val height = applicationContext.resources.getDimension(R.dimen.text_font_size)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.textSize = height

        val lines: List<String> = buildStringList(inputedString, paint, (ivPost.width * 0.8).toInt())

        if (mode == 0)
            drawTextInMode1(canvas, lines, drawForExport)
        else if (mode == 1)
            drawTextInMode2(canvas, lines, drawForExport)
        else if (mode == 2)
            drawTextInMode3(canvas, lines, drawForExport)
        else
            throw IllegalStateException("invalid mode")

    }

    private fun drawText(canvas: Canvas,
                         lines: List<String>,
                         textPaint: Paint,
                         cursorPaint: Paint,
                         hintPaint: Paint,
                         backPaint: Paint? = null,
                         drawForExport: Boolean) {
        val height = resources.getDimension(R.dimen.text_font_size)
        val paddingBetweenLine = resources.getDimension(R.dimen.padding_between_lines)

        val textCenterX = canvas.width / 2
        val textCenterY = canvas.height / 2

        var startY = textCenterY - (lines.size / 2) * height

        var index = 0
        var cursorX = 0f
        var cursorY = 0f

        combineLinesByWidth(textPaint, lines, 20).forEach { lineGroup ->

            backPaint?.let {
                val rect: RectF = RectF(textCenterX - lineGroup.first / 2f - 20,
                        startY - height,
                        textCenterX + lineGroup.first / 2f + 20,
                        startY + (lineGroup.second.size - 1) * (paddingBetweenLine + height) + paddingBetweenLine)

                canvas.drawRoundRect(rect, 20f, 20f, it)
            }

            lineGroup.second.forEach {
                canvas.drawText(lines[index], textCenterX - lineGroup.first / 2.toFloat(), startY, textPaint)
                cursorX = textCenterX - lineGroup.first / 2 + textPaint.measureText(it).toFloat()
                cursorY = startY

                startY += height + paddingBetweenLine

                ++index
            }
        }

        if (!drawForExport) {
            if (lines.isNotEmpty() && lines.first().isNotEmpty())
                canvas.drawLine(cursorX + 10, cursorY - height - 10, cursorX + 10, cursorY + 10, cursorPaint)
            else  {
                val hint = getString(R.string.hint)
                val x = textCenterX - hintPaint.measureText(hint) / 2.toFloat()
                canvas.drawText(hint, x, startY, hintPaint)
                canvas.drawLine(x - 5, startY - height - 10, x - 5, startY + 10, cursorPaint)
            }
        }


    }

    private fun drawTextInMode1(canvas: Canvas, lines: List<String>, drawForExport: Boolean) {
        val height = applicationContext.resources.getDimension(R.dimen.text_font_size)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.textSize = height;
        paint.color = Color.BLACK

        val hintPaint = Paint()
        hintPaint.textSize = height
        hintPaint.color = Color.GRAY

        val cursorPaint = Paint()
        cursorPaint.textSize = height
        cursorPaint.color = Color.WHITE

        drawText(canvas, lines, paint, cursorPaint, hintPaint, null, drawForExport)
    }

    private fun drawTextInMode2(canvas: Canvas, lines: List<String>, drawForExport: Boolean) {
        val height = applicationContext.resources.getDimension(R.dimen.text_font_size)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.BLACK
        paint.textSize = height;

        val backPaint = Paint()
        backPaint.color = Color.WHITE

        val hintPaint = Paint()
        hintPaint.textSize = height
        hintPaint.color = Color.GRAY

        val cursorPaint = Paint()
        cursorPaint.color = Color.GRAY
        cursorPaint.textSize = height

        drawText(canvas, lines, paint, cursorPaint, hintPaint, backPaint, drawForExport)
    }

    private fun drawTextInMode3(canvas: Canvas, lines: List<String>, drawForExport: Boolean) {
        val height = applicationContext.resources.getDimension(R.dimen.text_font_size)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.WHITE
        paint.textSize = height;

        val backPaint = Paint()
        backPaint.color = applicationContext.getColor(R.color.colorBackAlpha)

        val hintPaint = Paint()
        hintPaint.textSize = height
        hintPaint.color = Color.GRAY

        val cursorPaint = Paint()
        cursorPaint.color = Color.GRAY
        cursorPaint.textSize = height

        drawText(canvas, lines, paint, cursorPaint, hintPaint, backPaint, drawForExport)

    }

    private fun initializeTrashIcons() {
        val radius = resources.getDimension(R.dimen.closed_trash_radius)
        closedTrashBitmap = buildTrashBitmap(radius, R.drawable.ic_fab_trash)
        openedTrashBitmap = buildTrashBitmap(radius * 1.05f, R.drawable.ic_fab_trash_released)

    }

    private fun buildTrashBitmap(radius: Float, trashImageID: Int): Bitmap {

        val bitmap = Bitmap.createBitmap(2 * radius.toInt(), 2 * radius.toInt(), Bitmap.Config.ARGB_8888)
        val innerPadding = resources.getDimension(R.dimen.trash_inner_padding)
        val paintWhite = Paint()
        paintWhite.color = Color.WHITE

        val image = BitmapFactory.decodeResource(applicationContext.resources, trashImageID)
        val src = RectF(0f, 0f, image.width.toFloat(), image.height.toFloat())
        val dst = RectF(innerPadding, innerPadding, bitmap.width - innerPadding, bitmap.height - innerPadding.toFloat())
        val mat = Matrix()
        mat.setRectToRect(src, dst, Matrix.ScaleToFit.FILL)

        with(Canvas(bitmap)) {
            drawCircle(radius, radius, radius, paintWhite)
            drawBitmap(image, mat, null)
        }

        return bitmap
    }

    private fun drawSticker(canvas: Canvas, sticker: StickerItem) = with(sticker.sticker.resource) {
        canvas.drawBitmap(this, buildMatrix(this, sticker), null)
    }


}

