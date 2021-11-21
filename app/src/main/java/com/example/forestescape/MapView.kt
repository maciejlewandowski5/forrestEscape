package com.example.forestescape

import android.content.Context
import android.graphics.drawable.Drawable
import android.location.Location
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import kotlin.math.*
import android.graphics.*


class MapView : View {
    private lateinit var targetPaint: Paint
    private lateinit var targetInnerPaint: Paint
    private lateinit var scannerPaint: Paint
    private var compassArrow: Drawable? = null
    private var backgrColor: Int = Color.BLACK
    var framesPerSecond = 60
    var animationDurationms: Long = 3000
    var startTime: Long = 0
    var rotationStartTime: Long = 0
    var rotationAnimationDurationMs: Long = 1000

    var startAngle = 0f
    var angle = 0f
        set(value) {
           // if (value != angle) {
                startAngle = angle
                field = value
                rotationStartTime = System.currentTimeMillis()
            //}
        }
    var location: Location? = null
        set(value) {
            field = value
            //  invalidate()
        }


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

        val a = context.obtainStyledAttributes(
            attrs, R.styleable.MapView, defStyle, 0
        )
        a.recycle()
        val redColor = ContextCompat.getColor(context, R.color.middle_red)
        val neonGreenColor = ContextCompat.getColor(context, R.color.neon_green)
        backgrColor = ContextCompat.getColor(context, R.color.dark_blue)

        targetPaint = Paint().apply {
            flags = Paint.ANTI_ALIAS_FLAG
            color = redColor
            style = Paint.Style.FILL
            maskFilter = BlurMaskFilter(50f, BlurMaskFilter.Blur.NORMAL)
        }
        targetInnerPaint = Paint().apply {
            flags = Paint.ANTI_ALIAS_FLAG
            color = redColor
            style = Paint.Style.FILL
        }

        scannerPaint = Paint().apply {
            flags = Paint.ANTI_ALIAS_FLAG
            style = Paint.Style.STROKE
            color = neonGreenColor
            strokeWidth = 0.5f
        }

        compassArrow = ContextCompat.getDrawable(context, R.drawable.compass_arrow)


        this.startTime = System.currentTimeMillis()
        this.postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(backgrColor)

        canvas.save()
        canvas.translate(
            (width / 2).toFloat(),
            (height - height / 8).toFloat()
        )
        val elapsedTime = System.currentTimeMillis() - startTime
        drawScanner(canvas, elapsedTime, 1)
        val rotationAnimationElapsedTime = System.currentTimeMillis() - rotationStartTime


        if (elapsedTime >= animationDurationms) {
            this.startTime = System.currentTimeMillis()
            this.postInvalidate()
        }


        canvas.restore()


        val animatedAngle = angle
            //min(
             //   startAngle + (rotationAnimationElapsedTime) * (angle - startAngle) / (rotationAnimationDurationMs),
            //    angle
          //  )
        drawTarget(canvas, animatedAngle)
        setUserPositionScreenGlobalCords()
        drawUserPosition(canvas, animatedAngle)

        this.postInvalidateDelayed(
            (1000 / framesPerSecond).toLong()
        )
    }

    private fun drawScanner(canvas: Canvas, elapsedTime: Long, delay: Long) {
        canvas.save()
        canvas.scale(2 * elapsedTime / delay / 1000f, 2 * elapsedTime / delay / 1000f)
        canvas.drawCircle(0f, 0f, height / 8f, scannerPaint)
        canvas.restore()
    }

    private fun drawTarget(canvas: Canvas, animatedAngle: Float) {
        location?.latitude?.let { lat ->
            location?.longitude?.let { lon ->
                val (px, py) = interpolateGameLocationToScreenCords(lon, lat)
                val (rx, ry) = rotateAroundUserRepresentationOnScreenInGlobalCords(
                    px,
                    py,
                    animatedAngle,
                    // angle
                )
                val (x, y) = preventEscapingScreen(rx, ry)
                canvas.drawCircle(x.toFloat(), y.toFloat(), (width / 32).toFloat(), targetPaint)
                canvas.drawCircle(x.toFloat(), y.toFloat(), width / 64.toFloat(), targetInnerPaint)

            }
        }
    }

    private fun drawUserPosition(canvas: Canvas, animatedAngle: Float) {
        canvas.save()
        canvas.translate(
            (width / 2).toFloat(),
            (height - height / 8).toFloat()
        )
        canvas.save()
        canvas.rotate((/*angle*/ animatedAngle))
        compassArrow!!.draw(canvas)
        canvas.restore()
        canvas.restore()
    }

    private fun setUserPositionScreenGlobalCords() {
        val aspectRatio = compassArrow?.intrinsicHeight!!
            .div(compassArrow!!.intrinsicWidth.toFloat())
        val derivedHeightInPx = height / 8
        val desiredWidthInPx = (derivedHeightInPx / aspectRatio).toInt()
        compassArrow!!.setBounds(
            -desiredWidthInPx / 2, -derivedHeightInPx / 2,
            desiredWidthInPx / 2, derivedHeightInPx / 2
        )
    }

    private fun preventEscapingScreen(
        rx: Double,
        ry: Double
    ): Pair<Double, Double> {
        val x =
            max(min(rx, width.toDouble() - (width / 32).toFloat()), 0.0 + (width / 32).toFloat())
        val y =
            max(min(ry, height.toDouble() - (width / 32).toFloat()), 0.0 + (width / 32).toFloat())
        return Pair(x, y)
    }

    private fun rotateAroundUserRepresentationOnScreenInGlobalCords(
        px: Double,
        py: Double,
        animatedAngle: Float
    ): Pair<Double, Double> {

        val rx =
            (px * cos(animatedAngle / 180 * Math.PI) -
                    py * sin(animatedAngle / 180 * Math.PI)) + width / 2
        val ry =
            (px * sin(animatedAngle / 180 * Math.PI) +
                    py * cos(animatedAngle / 180 * Math.PI)) + 7 * height / 8
        return Pair(rx, ry)
    }

    private fun interpolateGameLocationToScreenCords(
        lon: Double,
        lat: Double
    ): Pair<Double, Double> {
        val px =
            0 + (lon - 19.419914111495018) * (width - 0) / (19.42165955901146 - 19.419914111495018)

        val py =
            0 + (lat - 51.64834767856383) * (height - 0) / (-51.64834767856383 + 51.6475415387574)
        return Pair(px, py)
    }

}