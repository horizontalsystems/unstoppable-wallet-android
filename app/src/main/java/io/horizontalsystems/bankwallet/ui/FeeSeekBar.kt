package io.horizontalsystems.bankwallet.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.widget.SeekBar
import androidx.appcompat.widget.AppCompatSeekBar
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.views.helpers.LayoutHelper

class FeeSeekBar @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = R.attr.seekBarStyle)
    : AppCompatSeekBar(context, attrs, defStyleAttr) {

    private val bubbleWidth = LayoutHelper.dpToPx(78f, context)
    private val bubbleHeight = LayoutHelper.dpToPx(52f, context)

    private val linePadding = LayoutHelper.dpToPx(16f, context).toInt()
    private val rect = RectF(0f, 0f, bubbleWidth, bubbleHeight)

    private var isTracking = false

    private val bubbleText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#EFEFF4")
        textSize = LayoutHelper.spToPx(22f, context)
    }

    private val bubbleTextSecondary = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#808085")
        textSize = LayoutHelper.spToPx(14f, context)
    }

    private var fillPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#252933")
    }

    private var strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#336E7899")
        strokeWidth = LayoutHelper.dpToPx(1f, context)
    }

    private var linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#808085")
        strokeWidth = LayoutHelper.dpToPx(1f, context)
    }

    init {
        setPadding(linePadding, 0, linePadding, 0)

        setOnSeekBarChangeListener(object: OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {

            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isTracking = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                isTracking = false
            }
        })
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Bubble
        if (isTracking) {
            drawBubble(canvas)
        }

        val bounds = thumb.bounds

        //  controls
        val controlWidth = LayoutHelper.dpToPx(9f, context)
        val controlHalf = controlWidth / 2
        val thumbHalf = (bounds.bottom - bounds.top) / 2
        val thumbCenter = (bounds.top + thumbHalf).toFloat()

        canvas.drawLine(0f, thumbCenter, controlWidth, thumbCenter, linePaint)

        canvas.drawLine(right - controlWidth, thumbCenter, right.toFloat(), thumbCenter, linePaint)
        canvas.drawLine(right - controlHalf, thumbCenter - controlHalf, right - controlHalf, thumbCenter + controlHalf, linePaint)
    }

    private fun drawBubble(canvas: Canvas) {
        val bounds = thumb.bounds
        var x = bounds.left.toFloat() + linePadding

        //  if thumb reaches the edges no need to move bubble
        val offset = linePadding * 2.5f
        if (bounds.right + offset >= right)
            x = right - offset
        if (bounds.right - offset <= left)
            x = offset

        rect.offsetTo(x - bubbleWidth / 2, -bubbleHeight)

        val cornerRadius = LayoutHelper.dpToPx(8f, context)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, fillPaint)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, strokePaint)

        //  Text 1
        val firstLine = "${progress}"
        val firstWidth = bubbleText.measureText(firstLine)

        canvas.drawText(firstLine, x - firstWidth / 2, -65f, bubbleText)

        //  Text 2
        val secondLine = "sat/byte"
        val secondWidth = bubbleTextSecondary.measureText(secondLine)

        canvas.drawText(secondLine, x - secondWidth / 2, -25f, bubbleTextSecondary)
    }
}
