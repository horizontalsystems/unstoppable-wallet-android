package io.horizontalsystems.seekbar

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.widget.SeekBar
import androidx.appcompat.widget.AppCompatSeekBar
import io.horizontalsystems.views.helpers.LayoutHelper

class FeeSeekBar @JvmOverloads constructor(context: Context, private val attrs: AttributeSet? = null, defStyleAttr: Int = R.attr.seekBarStyle)
    : AppCompatSeekBar(context, attrs, defStyleAttr) {

    interface Listener {
        fun onSelect(value: Int)
    }

    private val config = SeekBarConfig(context)

    private var isTracking = false
    private var listener: Listener? = null

    private val rect = RectF(0f, 0f, config.bubbleWidth, config.bubbleHeight)
    private val bubbleText = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bubbleTextSecondary = Paint(Paint.ANTI_ALIAS_FLAG)
    private var bubbleBackground = Paint()
    private var bubbleStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var linePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private fun initialize() {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.FeeSeekBar)
        try {
            ta.getInt(R.styleable.FeeSeekBar_textColor, config.textColor).let { config.textColor = it }
            ta.getInt(R.styleable.FeeSeekBar_textColorSecondary, config.textColorSecondary).let { config.textColorSecondary = it }
            ta.getInt(R.styleable.FeeSeekBar_controlsColor, config.controlsColor).let { config.controlsColor = it }
            ta.getInt(R.styleable.FeeSeekBar_bubbleBackground, config.bubbleBackground).let { config.bubbleBackground = it }
            ta.getInt(R.styleable.FeeSeekBar_bubbleStroke, config.bubbleStroke).let { config.bubbleStroke = it }
            ta.getString(R.styleable.FeeSeekBar_bubbleHint)?.let { config.bubbleHint = it }
        } finally {
            ta.recycle()
        }

        bubbleText.apply {
            color = config.textColor
            textSize = config.textSize
            typeface = config.notoSans
        }

        bubbleTextSecondary.apply {
            color = config.textColorSecondary
            textSize = config.textSizeSecondary
            typeface = config.notoSans
        }

        bubbleBackground.apply {
            style = Paint.Style.FILL
            color = config.bubbleBackground
        }

        bubbleStrokePaint.apply {
            style = Paint.Style.STROKE
            color = config.bubbleStroke
            strokeWidth = config.strokeWidth
        }

        linePaint.apply {
            style = Paint.Style.STROKE
            color = config.controlsColor
            strokeWidth = config.strokeWidth
        }
    }

    init {
        initialize()
        setPadding(config.linePadding, 0, config.linePadding, 0)

        setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (!fromUser) {
                    listener?.onSelect(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                isTracking = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                isTracking = false
                listener?.onSelect(progress)
            }
        })
    }

    fun setListener(listener: Listener) {
        this.listener = listener
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
        var x = bounds.left.toFloat() + config.linePadding

        //  if thumb reaches the edges no need to move bubble
        val offset = config.linePadding * 2.5f
        if (bounds.right + offset >= right)
            x = right - offset
        if (bounds.right - offset <= left)
            x = offset

        rect.offsetTo(x - config.bubbleWidth / 2, -config.bubbleHeight)

        val cornerRadius = LayoutHelper.dpToPx(8f, context)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bubbleBackground)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bubbleStrokePaint)

        //  Text 1
        val firstLine = "${progress}"
        val firstWidth = bubbleText.measureText(firstLine)

        canvas.drawText(firstLine, x - firstWidth / 2, -65f, bubbleText)

        //  Text 2
        val secondLine = config.bubbleHint
        val secondWidth = bubbleTextSecondary.measureText(secondLine)

        canvas.drawText(secondLine, x - secondWidth / 2, -25f, bubbleTextSecondary)
    }
}
