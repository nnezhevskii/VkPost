package com.licht.vkpost.vkpost

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.os.Handler
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
import android.view.inputmethod.InputMethodManager
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
import kotlin.math.abs
import kotlin.math.max


class PostActivity : AppCompatActivity(), IPostView, ItemManipulator {
    private lateinit var ivPost: ImageView

    var stickers: MutableList<StickerItem> = mutableListOf()
    private var selectedItem: StickerItem? = null

    private lateinit var blurScript: ScriptIntrinsicBlur
    private lateinit var script: RenderScript

    private var inputedString: String = ""
    private lateinit var editText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post)
        ivPost = findViewById(R.id.iv_post)

        findViewById<ImageView>(R.id.iv_sticket).setOnClickListener {
            val bottomSheetFragment = BottomSheetFragment()
            bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)

        }

        editText = findViewById<EditText>(R.id.ed_hided)
        editText.setSingleLine(false)
        editText.setSingleLine(false);
        editText.setImeOptions(EditorInfo.IME_FLAG_NO_ENTER_ACTION);
        editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE;



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
                saveImage(applicationContext, getBitmap(), "post-${Date()}")
            else
                requestPermission(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        WRITE_EXTERNAL_STORAGE_REQUEST_CODE)
        }

        GestureHelper(ivPost).addListener(this)

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            WRITE_EXTERNAL_STORAGE_REQUEST_CODE ->
                if (resultCode == Activity.RESULT_OK)
                    saveImage(applicationContext, getBitmap(), "post-${Date()}")

            PICK_IMAGE -> {
                data?.let {
                    background = MediaStore.Images.Media.getBitmap(this.contentResolver, data.data)
                    redraw()
                }
//                val temp = 0
//                saveImage(applicationContext, getBitmap(), "post-${Date()}")

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

        val colors: MutableList<BackgroundWrapper> = mutableListOf<Int>(
                R.color.colorAccent, R.color.colorPrimary, R.color.colorPrimaryDark)
                .map { id -> ContextCompat.getColor(applicationContext, id) }
                .map { color -> ColorWrapper(color) }.toMutableList()
        colors.add(ColorWrapper(Color.RED))
        colors.add(ColorWrapper(Color.YELLOW))
        colors.add(ImageWrapper(getBitmapFromResoure(resources, R.drawable.bg_stars_center)))

        colors.add(CompoundImageWrapper(
                getBitmapFromResoure(resources, R.drawable.bg_beach_center),
                getBitmapFromResoure(resources, R.drawable.bg_beach_top),
                getBitmapFromResoure(resources, R.drawable.bg_beach_bottom)
        ))

        colors.add(AddImageCommandWrapper())

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
        val bitmap = getBitmap()
        val stickerItem = StickerItem(
                sticker,
                bitmap.width / 4, bitmap.height / 4,
                sticker.resource.width, sticker.resource.height)

        stickers.add(stickerItem)

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
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE)
    }


    private var isTrashShowed: Boolean = false
    private var isTrashActivated: Boolean = false

    override fun selectItem(x: Int, y: Int) {
        selectedItem = getSelectedSticker(x, y)
        if (selectedItem == null) {

            Handler().postDelayed(Runnable {
                editText.requestFocus()
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)


            }, 100)

        } else
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
        return trashLocation?.let { actualX in it.left..it.right && actualY in it.top..it.bottom }
                ?: false
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
        redrawText(canvas)
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

    private var mode = 0

    private fun redrawText(canvas: Canvas) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.textSize = 24 * resources.displayMetrics.density;

        val lines: List<String> = buildStringList(inputedString, paint, (ivPost.width * 0.8).toInt())

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

    private fun drawText(canvas: Canvas, lines: List<String>,
                         textPaint: Paint,
                         cursorPaint: Paint,
                         hintPaint: Paint,
                         backPaint: Paint? = null) {
        val height = 24 * resources.displayMetrics.density
        val paddingBetweenLine = 4 * resources.displayMetrics.density

        val textCenterX = canvas.width / 2
        val textCenterY = canvas.height / 2

        var startY = textCenterY - (lines.size / 2) * height


        val combinedLines = combineLinesByWidth(textPaint, lines)


        var index = 0
        var cursorX = 0f
        var cursorY = 0f

        combinedLines.forEach { lineGroup ->

            backPaint?.let {
                val rect: RectF = RectF(textCenterX - lineGroup.first / 2f - 20,
                        startY - height,
                        textCenterX + lineGroup.first / 2f + 20,
                        startY + (lineGroup.second.size - 1) * (paddingBetweenLine + height) + paddingBetweenLine)

//                canvas.drawRect(rect, it)
                canvas.drawRoundRect(rect, 20f, 20f, it)
            }

            lineGroup.second.forEach {
                canvas.drawText(lines[index], textCenterX - lineGroup.first / 2.toFloat(), startY, textPaint)
                cursorX = textCenterX - lineGroup.first / 2 + textPaint.measureText(it).toFloat()
                cursorY = startY.toFloat()

                startY += height + paddingBetweenLine

                ++index
            }
        }

        if (lines.isNotEmpty() && lines.first().isNotEmpty())
            canvas.drawLine(cursorX + 10, cursorY - height - 10, cursorX + 10, cursorY + 10, cursorPaint)
        else {
            val hint = "Что у вас нового?"
            val x = textCenterX - hintPaint.measureText(hint) / 2.toFloat()
            canvas.drawText(hint, x, startY, hintPaint)
            canvas.drawLine(x - 5, startY - height - 10, x - 5, startY + 10, cursorPaint)
        }

    }

    private fun buildStringList(src: String, paint: Paint, maxWidth: Int): List<String> {
        if (src.isEmpty())
            return listOf()

        val words = src.split(' ')

        val res = mutableListOf<String>()

        var currentString = ""


        for (word in words) {
            if (paint.measureText(currentString + word) >= maxWidth) {
                res.add(currentString)

                currentString = word
            } else {
                if (paint.measureText(word) > maxWidth) {
                    res.add(word.substring(0, word.length / 2 - 1) + "-")
                    res.add(word.substring(word.length / 2))
                } else {
                    currentString = currentString.plus(" ").plus(word)
                }


            }
        }
        res.add(currentString)

        return res
    }


    private fun combineLinesByWidth(paint: Paint, lines: List<String>): List<Pair<Int, List<String>>> {
        if (lines.isEmpty())
            return emptyList()

        val res: MutableList<Pair<Int, MutableList<String>>> = mutableListOf()

        var crntWidth = paint.measureText(lines.first())
        var crntGroup: MutableList<String> = mutableListOf()

        for (line in lines) {
            val width = paint.measureText(line)
            if (abs(width - crntWidth) < 20) {
                crntGroup.add(line)
                crntWidth = max(width, crntWidth)
            } else {
                res.add(Pair(crntWidth.toInt(), crntGroup))
                crntWidth = width
                crntGroup = mutableListOf(line)
            }
        }

        res.add(Pair(crntWidth.toInt(), crntGroup))

        return res
    }

    private fun drawTextInMode1(canvas: Canvas, lines: List<String>) {

        val height = 24 * resources.displayMetrics.density
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.BLACK
        paint.textSize = height;

        val hintPaint = Paint()
        hintPaint.textSize = 24 * resources.displayMetrics.density;
        hintPaint.color = Color.GRAY

        val cursorPaint = Paint()
        cursorPaint.color = Color.WHITE
        cursorPaint.textSize = 24 * resources.displayMetrics.density;

        drawText(canvas, lines, paint, cursorPaint, hintPaint, null)
    }

    private fun drawTextInMode2(canvas: Canvas, lines: List<String>) {
        val height = 24 * resources.displayMetrics.density

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.BLACK
        paint.textSize = height;

        val backPaint = Paint()
        backPaint.setColor(Color.WHITE)

        val hintPaint = Paint()
        hintPaint.textSize = 24 * resources.displayMetrics.density;
        hintPaint.color = Color.GRAY

        val cursorPaint = Paint()
        cursorPaint.color = Color.GRAY
        cursorPaint.textSize = 24 * resources.displayMetrics.density;

        drawText(canvas, lines, paint, cursorPaint, hintPaint, backPaint)
    }

    private fun drawTextInMode3(canvas: Canvas, lines: List<String>) {
        val height = 24 * resources.displayMetrics.density

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.WHITE
        paint.textSize = height;

        val backPaint = Paint()
        backPaint.setColor(Color.argb(64, 255, 255, 255))

        val hintPaint = Paint()
        hintPaint.textSize = 24 * resources.displayMetrics.density;
        hintPaint.color = Color.GRAY

        val cursorPaint = Paint()
        cursorPaint.color = Color.GRAY
        cursorPaint.textSize = 24 * resources.displayMetrics.density;

        drawText(canvas, lines, paint, cursorPaint, hintPaint, backPaint)

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

