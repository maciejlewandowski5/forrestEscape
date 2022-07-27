package com.example.forestescape

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import com.mapbox.maps.MapView
import com.mapbox.maps.ScreenCoordinate
import kotlin.math.*

class MapViewCustom : MapView {

    private val targetPaint: Paint = Paint().apply {
        flags = Paint.ANTI_ALIAS_FLAG
        color = ContextCompat.getColor(context, R.color.middle_red)
        style = Paint.Style.FILL
        maskFilter = BlurMaskFilter(50f, BlurMaskFilter.Blur.NORMAL)
    }
    private val targetInnerPaint: Paint = Paint().apply {
        flags = Paint.ANTI_ALIAS_FLAG
        color = ContextCompat.getColor(context, R.color.middle_red)
        style = Paint.Style.FILL
    }
    private val targetPaintClose: Paint = Paint().apply {
        flags = Paint.ANTI_ALIAS_FLAG
        color = ContextCompat.getColor(context, R.color.dark_blue)
        style = Paint.Style.FILL
        maskFilter = BlurMaskFilter(50f, BlurMaskFilter.Blur.NORMAL)
    }
    private val targetInnerPaintClose: Paint = Paint().apply {
        flags = Paint.ANTI_ALIAS_FLAG
        color = ContextCompat.getColor(context, R.color.neon_green)
        style = Paint.Style.FILL
    }

    private val scannerPaint: Paint = Paint().apply {
        flags = Paint.ANTI_ALIAS_FLAG
        style = Paint.Style.STROKE
        color = ContextCompat.getColor(context, R.color.neon_green)
        strokeWidth = 0.5f
    }

    private val compassArrow: Drawable? =
        ContextCompat.getDrawable(context, R.drawable.compass_arrow)
    var startTime: Long = 0
    var screenCoordinate: ScreenCoordinate = ScreenCoordinate(0.0, 0.0)
    var angle = 0f

    constructor(context: Context) : super(context) {
        init(null, 0)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init(attrs, defStyle)
    }

    private fun init(attrs: AttributeSet?, defStyle: Int) {
        context.obtainStyledAttributes(attrs, R.styleable.MapView, defStyle, 0).recycle()
        this.startTime = System.currentTimeMillis()
        this.postInvalidate()
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        val elapsedTime = System.currentTimeMillis() - startTime
        drawScanner(canvas, elapsedTime)
        refreshScanner(elapsedTime)
        drawTarget(canvas, elapsedTime)
        setCompassArrowBounds()
        drawUserPosition(canvas, angle)
        this.postInvalidateDelayed((1000 / FRAMES_PER_SECOND).toLong())
    }

    private fun refreshScanner(elapsedTime: Long) {
        if (elapsedTime >= ANIMATON_DURATION) {
            this.startTime = System.currentTimeMillis()
            this.postInvalidate()
        }
    }

    private fun drawScanner(canvas: Canvas, elapsedTime: Long) {
        canvas.save()
        canvas.translate((width / 2).toFloat(), (height / 2).toFloat())
        canvas.drawCircle(0f, 0f, scannerRadius(elapsedTime), scannerPaint)
        canvas.restore()
    }

    private fun drawTarget(canvas: Canvas, elapsedTime: Long) {
        val fx = screenCoordinate.x.toFloat()
        val fy = screenCoordinate.y.toFloat()
        val distance = distanceToCenter(fx, fy)
        val delta = abs(scannerRadius(elapsedTime) - distance)
        val (targetPaint, targetInnerPaint) = targetPaint(distance)

        val rx: Float = if (fx == -1f) {
            val tx = screenCircleRadius() * cos(angle / 180f * PI) + width / 2f
            if (tx < width && tx > 0) {
                tx.toFloat()
            } else if (tx > width) {
                width.toFloat()
            } else {
                0f
            }
        } else {
            fx
        }
        val ry: Float = if (fy == -1f) {
            val ty = screenCircleRadius() * sin(angle / 180f * PI) + height / 2f
            if (ty < height && ty > 0) {
                ty.toFloat()
            } else if (ty > height) {
                height.toFloat()
            } else {
                0f
            }
        } else {
            fy
        }

        canvas.drawCircle(rx, ry, targetSize(delta) * 2, targetPaint)
        canvas.drawCircle(rx, ry, targetSize(delta), targetInnerPaint)
    }

    private fun screenCircleRadius() = sqrt(
        height.toFloat().pow(2) + width.toFloat().pow(2)
    ) / 2f

    private fun scannerRadius(time: Long) = 2 * time / 1000f * height / 8f - height / 8f

    private fun distanceToCenter(fx: Float, fy: Float) = sqrt(
        abs(fx - width / 2).pow(2) + abs(fy - height / 2).pow(
            2
        )
    )

    private fun targetPaint(distanceToCenter: Float) = if (distanceToCenter <= 100) {
        Pair(targetPaintClose, targetInnerPaintClose)
    } else {
        Pair(targetPaint, targetInnerPaint)
    }

    private fun targetSize(delta: Float) = if (delta <= 10) {
        (width / 56).toFloat()
    } else {
        width / 64.toFloat()
    }

    private fun drawUserPosition(canvas: Canvas, animatedAngle: Float) {
        canvas.save()
        canvas.translate((width / 2).toFloat(), (height / 2).toFloat())
        canvas.save()
        canvas.rotate(animatedAngle)
        compassArrow!!.draw(canvas)
        canvas.restore()
        canvas.restore()
    }

    private fun setCompassArrowBounds() {
        val aspectRatio = compassArrow?.intrinsicHeight!!
            .div(compassArrow.intrinsicWidth.toFloat())
        val derivedHeightInPx = height / 8
        val desiredWidthInPx = (derivedHeightInPx / aspectRatio).toInt()
        compassArrow.setBounds(
            -desiredWidthInPx / 2, -derivedHeightInPx / 2,
            desiredWidthInPx / 2, derivedHeightInPx / 2
        )
    }

    fun setTargetPosition(screenCoordinates: ScreenCoordinate) {
        this.screenCoordinate = screenCoordinates
    }

    companion object {
        private const val FRAMES_PER_SECOND = 60
        private const val ANIMATON_DURATION: Long = 3000
    }
}
