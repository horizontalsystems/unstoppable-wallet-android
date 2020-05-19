package io.horizontalsystems.bankwallet.ui.extensions

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import io.horizontalsystems.views.helpers.LayoutHelper
import kotlin.math.max

class RotatingCircleProgressView : View {

    private val mCirclePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mThickness: Float = LayoutHelper.dp(2f, context).toFloat()

    private val mCircleRect: RectF = RectF()
    private var mSize: Int = 0
    private var circlePadding = 0

    private var lastUpdateTime: Long = 0
    private var radOffset = 0f
    private var currentProgress = 0.1f
    private var animationProgressStart = 0f
    private var currentProgressTime: Long = 0
    private var animatedProgressValue = 0f

    private var decelerateInterpolator: DecelerateInterpolator = DecelerateInterpolator()

    init {
        lastUpdateTime = System.currentTimeMillis()

        mCirclePaint.style = Paint.Style.STROKE
        mCirclePaint.strokeCap = Paint.Cap.BUTT
        mCirclePaint.strokeWidth = mThickness
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    fun setProgressColored(value: Int, color: Int) {
        mCirclePaint.color = color

        if (value > 10) {
            currentProgress = value.toFloat() / 100
        }

        if (animatedProgressValue > currentProgress) {
            animatedProgressValue = currentProgress
        }
        animationProgressStart = animatedProgressValue
        currentProgressTime = 0

        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawArc(mCircleRect, radOffset, max(4f, 360 * animatedProgressValue), false, mCirclePaint)
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
