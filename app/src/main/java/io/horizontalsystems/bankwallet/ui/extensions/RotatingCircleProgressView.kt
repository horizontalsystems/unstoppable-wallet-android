package io.horizontalsystems.bankwallet.ui.extensions

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.support.annotation.ColorInt
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.viewHelpers.LayoutHelper


class RotatingCircleProgressView : View {

    private lateinit var mCirclePaint: Paint
    private lateinit var circleBackgroundPaint: Paint
    private var mThickness: Float = 0.toFloat()

    private lateinit var mCircleRect: RectF
    @ColorInt
    private var mCircleColor: Int = 0
    @ColorInt
    private var circleBackgroundColor: Int = 0
    private var mSize: Int = 0
    private var circlePadding = 0

    private var lastUpdateTime: Long = 0
    private var radOffset = 0f
    private var currentProgress = 0.1f
    private var animationProgressStart = 0f
    private var currentProgressTime: Long = 0
    private var animatedProgressValue = 0f

    private lateinit var decelerateInterpolator: DecelerateInterpolator


    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    private fun init() {
        decelerateInterpolator = DecelerateInterpolator()
        mCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        circleBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        mCircleRect = RectF()
        mThickness = LayoutHelper.dp(2f, context).toFloat()
        mCircleColor = ContextCompat.getColor(context, R.color.dark)
        circleBackgroundColor = ContextCompat.getColor(context, R.color.grey)
        setPaint()
        lastUpdateTime = System.currentTimeMillis()
    }

    fun setProgress(value: Float) {
        if (value > 10) {
            currentProgress = value/100
        }
        if (animatedProgressValue > currentProgress) {
            animatedProgressValue = currentProgress
        }
        animationProgressStart = animatedProgressValue
        currentProgressTime = 0

        invalidate()
    }

    private fun setPaint() {
        mCirclePaint.color = mCircleColor
        mCirclePaint.style = Paint.Style.STROKE
        mCirclePaint.strokeWidth = mThickness
        mCirclePaint.strokeCap = Paint.Cap.BUTT

        circleBackgroundPaint.style = Paint.Style.FILL
        circleBackgroundPaint.color = circleBackgroundColor
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val radius = (mSize/2).toFloat()
        canvas.drawCircle(radius, radius, radius, circleBackgroundPaint)
        canvas.drawArc(mCircleRect, radOffset, Math.max(4f, 360 * animatedProgressValue), false, mCirclePaint)
        updateAnimation()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        mSize = measuredWidth
        circlePadding = (mSize * 0.10).toInt()
        setMeasuredDimension(mSize, mSize)
        updateRectAngleBounds()
    }

    private fun updateRectAngleBounds() {
        mCircleRect.set(circlePadding + mThickness, circlePadding + mThickness,
                mSize.toFloat() - circlePadding.toFloat() - mThickness, mSize.toFloat() - circlePadding.toFloat() - mThickness)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mSize = if (w < h) w else h
        updateRectAngleBounds()
    }

    private fun updateAnimation() {
        val newTime = System.currentTimeMillis()
        val timeDiff = newTime - lastUpdateTime
        lastUpdateTime = newTime

        if (animatedProgressValue != 1f) {
            radOffset += 360 * timeDiff / 1100.0f
            val progressDiff = currentProgress - animationProgressStart
            if (progressDiff > 0) {
                currentProgressTime += timeDiff
                if (currentProgressTime >= 300) {
                    animatedProgressValue = currentProgress
                    animationProgressStart = currentProgress
                    currentProgressTime = 0
                } else {
                    animatedProgressValue = animationProgressStart + progressDiff * decelerateInterpolator.getInterpolation(currentProgressTime / 300.0f)
                }
            }
            invalidate()
        }
    }

}
