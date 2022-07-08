package com.example.forestescape

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.location.Location
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import com.mapbox.maps.MapView
import com.mapbox.maps.ScreenCoordinate
import java.math.BigDecimal
import kotlin.math.*

class MapViewCustom : MapView {

    private lateinit var targetPaint: Paint
    private lateinit var targetInnerPaint: Paint
    private lateinit var targetPaintClose: Paint
    private lateinit var targetInnerPaintClose: Paint
    private lateinit var scannerPaint: Paint
    private var compassArrow: Drawable? = null
    private var backgrColor: Int = Color.BLACK
    var framesPerSecond = 60
    var animationDurationms: Long = 3000
    var startTime: Long = 0
    var rotationStartTime: Long = 0
    var rotationAnimationDurationMs: Long = 1000
    var screenCoordinate: ScreenCoordinate = ScreenCoordinate(0.0, 0.0)

    var startAngle = 0f
    var angle = 0f
        set(value) {
            // if (value != angle) {
            startAngle = angle
            field = value
            rotationStartTime = System.currentTimeMillis()
            // }
        }
    var playerLocation: Location? = null
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

        targetPaintClose = Paint().apply {
            flags = Paint.ANTI_ALIAS_FLAG
            color = neonGreenColor
            style = Paint.Style.FILL
            maskFilter = BlurMaskFilter(50f, BlurMaskFilter.Blur.NORMAL)
        }
        targetInnerPaintClose = Paint().apply {
            flags = Paint.ANTI_ALIAS_FLAG
            color = neonGreenColor
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

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        //   canvas.drawColor(backgrColor)

        //    canvas.drawCircle(0f, 0f, 50f, targetPaint)
        //    canvas.drawCircle(200f, 0f, 100f, targetPaint)

        canvas.save()
        canvas.translate(
            (width / 2).toFloat(),
            (height / 2).toFloat()
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
        drawTarget(canvas, animatedAngle, elapsedTime, 1)
        setUserPositionScreenGlobalCords()
        drawUserPosition(canvas, animatedAngle)

        this.postInvalidateDelayed(
            (1000 / framesPerSecond).toLong()
        )
    }

    private fun drawScanner(canvas: Canvas, elapsedTime: Long, delay: Long) {
        //  canvas.drawCircle(0f, 0f, 2 * elapsedTime / delay / 1000f * height / 8f, scannerPaint)
        canvas.drawCircle(
            0f,
            0f,
            2 * elapsedTime / delay / 1000f * height / 8f - height / 8f,
            scannerPaint
        )
        // canvas.drawCircle(
        //  0f,
        // 0f,
        // 2 * elapsedTime / delay / 1000f * height / 8f - 2 * height / 8f,
        // scannerPaint
        // )
    }

    private fun drawTarget(canvas: Canvas, animatedAngle: Float, elapsedTime: Long, delay: Long) {
        val fx = screenCoordinate.x.toFloat()
        val fy = screenCoordinate.y.toFloat()
        val distanceToCenter =
            sqrt(
                abs(fx - width / 2).pow(2) + abs(fy - height / 2).pow(
                    2
                )
            )
        val delta =
            abs(2 * elapsedTime / delay / 1000f * height / 8f - height / 8f - distanceToCenter)
        val targetSize =
            if (delta <= 10) {
                (width / 56).toFloat()
            } else {
                width / 64.toFloat()
            }

        val (targetPaint, targetInnerPaint) = if (distanceToCenter <= 100) {
            Pair(targetPaintClose, targetInnerPaintClose)
        } else {
            Pair(targetPaint, targetInnerPaint)
        }

        canvas.drawCircle(
            fx,
            fy,
            targetSize * 2,
            targetPaint
        )
        canvas.drawCircle(
            fx,
            fy,
            targetSize,
            targetInnerPaint
        )
    }

    private fun lengthOfOneDegreeOfLongitude(playerLatitude: BigDecimal) =
        cos(playerLatitude.toDouble().toRadians()) * LENGTH_OF_ONE_DEGREE_ON_EQUATOR_KM

    private fun drawUserPosition(canvas: Canvas, animatedAngle: Float) {
        canvas.save()
        canvas.translate(
            (width / 2).toFloat(),
            (height / 2).toFloat()
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
            (
                px * cos(animatedAngle / 180 * Math.PI) -
                    py * sin(animatedAngle / 180 * Math.PI)
                ) + width / 2
        val ry =
            (
                px * sin(animatedAngle / 180 * Math.PI) +
                    py * cos(animatedAngle / 180 * Math.PI)
                ) + 7 * height / 8
        return Pair(rx, ry)
    }

    private fun interpolateGameLocationToScreenCords(
        lon: BigDecimal,
        lat: BigDecimal
    ): Pair<Double, Double> {
        val px =
            BigDecimal.ZERO + (lon - SOUTH_WEST_CORNER_LAT) * (BigDecimal.valueOf(width.toLong()) - BigDecimal.ZERO) / (
                South_EAST_CORNER_LAT - SOUTH_WEST_CORNER_LAT
                )

        val py =
            BigDecimal.ZERO + (lat - SOUTH_EAST_CORNER_LON) * (BigDecimal.valueOf(height.toLong()) - BigDecimal.ZERO) / (
                SOUTH_EAST_CORNER_LON.multiply(BigDecimal("-1")) + NORTH_EAST_CORNER_LON
                )
        return Pair(px.toDouble(), py.toDouble())
    }

    fun setTargetPosition(screenCoordinates: ScreenCoordinate) {
        this.screenCoordinate = screenCoordinates
    }

    companion object {
        private val NORTH_WEST_CORNER_LON = BigDecimal("51.732961")
        private val NORTH_WEST_CORNER_LAT = BigDecimal("19.457447")
        private val SOUTH_WEST_CORNER_LON = BigDecimal("51.72907")
        private val SOUTH_WEST_CORNER_LAT = BigDecimal("19.457048")
        private val NORTH_EAST_CORNER_LON = BigDecimal("51.733059")
        private val NORTH_EAST_CORNER_LAT = BigDecimal("19.459644")
        private val SOUTH_EAST_CORNER_LON = BigDecimal("51.728952")
        private val South_EAST_CORNER_LAT = BigDecimal("19.462512")

        private val TARGET_LON = BigDecimal("51.731137")
        private val TARGET_LAT = BigDecimal("19.461066")

        private val EARTH_RADIUS: Int = 6378

        private val LENGTH_OF_ONE_DEGREE_ON_EQUATOR_KM = 110.567
    }
}

private operator fun BigDecimal.times(value: Float): BigDecimal =
    BigDecimal.valueOf(value.toDouble()) * this

private fun Double.toRadians() = this * PI / 180
