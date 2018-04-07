package com.licht.vkpost.vkpost.view

import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView

class GestureHelper(private val view: ImageView) : View.OnTouchListener {
    private val listeners: MutableList<ItemManipulator> = mutableListOf()

    private var mPtrCount = 0

    private var mPrimStartTouchEventX = -1f
    private var mPrimStartTouchEventY = -1f
    private var mSecStartTouchEventX = -1f
    private var mSecStartTouchEventY = -1f
    private var mPrimSecStartTouchDistance = 0f

    private var mViewScaledTouchSlop = 0


    private var x = 0
    private var y = 0
    private var distance = 0f
    private var currentAngle = 0f

    init {
        view.setOnTouchListener(this)
    }

    fun addListener(listener: ItemManipulator) {
        listeners.add(listener)
    }

    fun removeListener(listener: ItemManipulator) {
        listeners.remove(listener)
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        val action = event.action and MotionEvent.ACTION_MASK

        when (action) {
            MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_DOWN -> {
                mPtrCount++
                if (mPtrCount == 1 && mPrimStartTouchEventY == -1f && mPrimStartTouchEventY == -1f) {
                    mPrimStartTouchEventX = event.getX(0)
                    mPrimStartTouchEventY = event.getY(0)
                    x = mPrimStartTouchEventX.toInt()
                    y = mPrimStartTouchEventY.toInt()


                    notifyItemSelect(event.x.toInt(), event.y.toInt())
                }
                if (mPtrCount == 2) {
                    // Starting distance between fingers
                    mSecStartTouchEventX = event.getX(1)
                    mSecStartTouchEventY = event.getY(1)
                    mPrimSecStartTouchDistance = distance(event, 0, 1)

                    distance = mPrimSecStartTouchDistance
                    currentAngle = rotation(event)
                    Log.d("TAG", String.format("POINTER TWO X = %.5f, Y = %.5f", mSecStartTouchEventX, mSecStartTouchEventY))
                    selectTwoFingersItem()
                }
            }
            MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_UP -> {
                mPtrCount--
                if (mPtrCount < 2) {
                    mSecStartTouchEventX = -1f
                    mSecStartTouchEventY = -1f
                    notifyItemRelease()
                }
                if (mPtrCount < 1) {
                    mPrimStartTouchEventX = -1f
                    mPrimStartTouchEventY = -1f
                    notifyItemRelease()
                }
            }

            MotionEvent.ACTION_MOVE -> {
                val isPrimMoving = isScrollGesture(event, 0, mPrimStartTouchEventX, mPrimStartTouchEventY)
                val isSecMoving = mPtrCount > 1 && isScrollGesture(event, 1, mSecStartTouchEventX, mSecStartTouchEventY)

                if (mPtrCount > 1 && isPinchGesture(event)) {
                    handleTwoFingerAction(event)
                } else if (isPrimMoving || isSecMoving) {
                    if (isPrimMoving && isSecMoving) {
                        handleTwoFingerAction(event)
                    } else if (isPrimMoving) {
                        handleOneFingerAction(event)
                    }
                }
            }
        }

        return true
    }

    private fun handleOneFingerAction(event: MotionEvent) {
        val oldX = x
        val oldY = y

        x = event.x.toInt()
        y = event.y.toInt()

        notifyItemMovedTo(x, y, x - oldX, y - oldY)
    }

    private fun handleTwoFingerAction(event: MotionEvent) {
        val oldDistance = distance
        distance = distance(event, 0, 1)
        val oldAngle = currentAngle
        currentAngle = rotation(event)

        notifyItemScaleAndRotate(distance / oldDistance, currentAngle - oldAngle)
    }

    private fun rotation(event: MotionEvent): Float {
        val delta_x = (event.getX(0) - event.getX(1)).toDouble()
        val delta_y = (event.getY(0) - event.getY(1)).toDouble()
        val radians = Math.atan2(delta_y, delta_x)
        return Math.toDegrees(radians).toFloat()
    }


    private fun isScrollGesture(event: MotionEvent, ptrIndex: Int, originalX: Float, originalY: Float): Boolean {
        val moveX = Math.abs(event.getX(ptrIndex) - originalX)
        val moveY = Math.abs(event.getY(ptrIndex) - originalY)

        return moveX > mViewScaledTouchSlop || moveY > mViewScaledTouchSlop
    }

    private fun isPinchGesture(event: MotionEvent): Boolean {
        if (event.pointerCount == 2) {
            val distanceCurrent = distance(event, 0, 1)
            val diffPrimX = mPrimStartTouchEventX - event.getX(0)
            val diffPrimY = mPrimStartTouchEventY - event.getY(0)
            val diffSecX = mSecStartTouchEventX - event.getX(1)
            val diffSecY = mSecStartTouchEventY - event.getY(1)

            if (Math.abs(distanceCurrent - mPrimSecStartTouchDistance) > mViewScaledTouchSlop
                    && diffPrimY * diffSecY <= 0
                    && diffPrimX * diffSecX <= 0)
                return true
        }

        return false
    }

    private fun distance(event: MotionEvent, first: Int, second: Int): Float {
        return if (event.pointerCount >= 2) {
            val x = event.getX(first) - event.getX(second)
            val y = event.getY(first) - event.getY(second)

            Math.sqrt((x * x + y * y).toDouble()).toFloat()
        } else {
            0f
        }
    }

    private fun selectTwoFingersItem() {
        val midX = ((mPrimStartTouchEventX + mSecStartTouchEventX) / 2).toInt()
        val midY = ((mPrimStartTouchEventY + mSecStartTouchEventY) / 2).toInt()
        Log.e("GestureHelper", "selectTwoFingersItem: x: $midX, y: $midY")
        notifyItemSelect(midX, midY)
    }

    private fun notifyItemScaleAndRotate(scale: Float, angle: Float) {
        listeners.forEach { listener -> listener.scale(scale, angle) }
    }

    private fun notifyItemSelect(x: Int, y: Int) {
        listeners.forEach { listener -> listener.selectItem(x, y) }
    }

    private fun notifyItemRelease() {
        listeners.forEach { listener -> listener.releaseItem() }
    }

    private fun notifyItemMovedTo(actualX: Int, actualY: Int, dx: Int, dy: Int) {
        listeners.forEach { listener -> listener.moveTo(actualX, actualY, dx, dy) }
    }
}

/**
 * Подписчик на обрабатываемые действия
 */
interface ItemManipulator {
    fun selectItem(x: Int, y: Int)
    fun moveTo(actualX: Int, actualY: Int, dx: Int, dy: Int)
    fun scale(factor: Float, angle: Float)
    fun releaseItem()
}