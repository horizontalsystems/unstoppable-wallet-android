package io.horizontalsystems.seekbar

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.widget.SeekBar
import androidx.appcompat.widget.AppCompatSeekBar

class FeeSeekBar @JvmOverloads constructor(context: Context, private val attrs: AttributeSet? = null, defStyleAttr: Int = R.attr.seekBarStyle)
    : AppCompatSeekBar(context, attrs, defStyleAttr) {

    interface Listener {
        fun onSelect(value: Int)
    }

    private val config = SeekBarConfig(context)

    private var isTracking = false
    private var listener: Listener? = null

    private val rect = RectF(0f, 0f, config.bubbleWidth, config.bubbleHeight)
    private val bubblePrimaryTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bubbleSecondaryTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var bubbleBackground = Paint()
    private var bubbleStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var seekBarLinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var sideSymbolLinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var primaryTextHeight = 0f
    private var secondaryTextHeight = 0f

    private fun initialize() {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.FeeSeekBar)
        try {
            ta.getInt(R.styleable.FeeSeekBar_textColor, config.textColor).let { config.textColor = it }
            ta.getInt(R.styleable.FeeSeekBar_textColorSecondary, config.textColorSecondary).let { config.textColorSecondary = it }
            ta.getInt(R.styleable.FeeSeekBar_controlsColor, config.symbolColor).let { config.symbolColor = it }
            ta.getInt(R.styleable.FeeSeekBar_bubbleBackground, config.bubbleBackground).let { config.bubbleBackground = it }
            ta.getInt(R.styleable.FeeSeekBar_bubbleStroke, config.bubbleStroke).let { config.bubbleStroke = it }
            ta.getString(R.styleable.FeeSeekBar_bubbleHint)?.let { config.bubbleHint = it }
        } finally {
            ta.recycle()
        }

        bubblePrimaryTextPaint.apply {
            color = config.textColor
            textSize = config.primaryTextSize
            typeface = config.notoSans
        }

        bubbleSecondaryTextPaint.apply {
            color = config.textColorSecondary
            textSize = config.secondaryTextSize
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

        seekBarLinePaint.apply {
            style = Paint.Style.STROKE
            color = config.mainLineColor
            strokeCap = Paint.Cap.ROUND
            strokeWidth = config.seekbarLineStrokeWidth
        }

        sideSymbolLinePaint.apply {
            style = Paint.Style.STROKE
            color = config.symbolColor
            strokeWidth = config.strokeWidth
        }

        val primTextBounds = Rect()
        bubblePrimaryTextPaint.getTextBounds("0", 0, 1, primTextBounds)
        primaryTextHeight = primTextBounds.height().toFloat()

        val secTextBounds = Rect()
        bubblePrimaryTextPaint.getTextBounds(config.bubbleHint, 0, config.bubbleHint.length, secTextBounds)
        secondaryTextHeight = secTextBounds.height().toFloat()
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

    fun setListener(listener: Listener?) {
        this.listener = listener
    }

    override fun onDraw(canvas: Canvas) {

        // Bubble
        if (isTracking) {
            drawBubble(canvas)
        }

        val bounds = thumb.bounds

        //  symbols
        val symbolHalf = config.sideSymbolWidth / 2
        val thumbHalf = (bounds.bottom - bounds.top) / 2
        val thumbCenter = (bounds.top + thumbHalf).toFloat()

        //seekbar line
        canvas.drawLine(config.sideSymbolWidth + config.seekbarSideMargin, thumbCenter, width.toFloat() - config.sideSymbolWidth - config.seekbarSideMargin , thumbCenter, seekBarLinePaint)

        //minus symbol
        canvas.drawLine(0f, thumbCenter, config.sideSymbolWidth, thumbCenter, sideSymbolLinePaint)

        //plus symbol
        canvas.drawLine(right - config.sideSymbolWidth, thumbCenter, right.toFloat(), thumbCenter, sideSymbolLinePaint)
        canvas.drawLine(right - symbolHalf, thumbCenter - symbolHalf, right - symbolHalf, thumbCenter + symbolHalf, sideSymbolLinePaint)

        super.onDraw(canvas)
    }

    private fun drawBubble(canvas: Canvas) {
        canvas.save()
        val bounds = thumb.bounds
        var x = bounds.left.toFloat() + config.linePadding

        //  if thumb reaches the edges no need to move bubble
        val offset = config.linePadding * 2.5f
        if (bounds.right + offset >= right)
            x = right - offset
        if (bounds.right - offset <= left)
            x = offset

        rect.offsetTo(x - config.bubbleWidth / 2, -config.bubbleHeight)

        canvas.drawRoundRect(rect, config.bubbleCornerRadius, config.bubbleCornerRadius, bubbleBackground)
        canvas.drawRoundRect(rect, config.bubbleCornerRadius, config.bubbleCornerRadius, bubbleStrokePaint)

        //  Text 1
        val firstLine = "${progress}"
        val firstWidth = bubblePrimaryTextPaint.measureText(firstLine)

        canvas.translate(0f, -config.bubbleHeight + primaryTextHeight + config.primaryTextTopMargin)
        canvas.drawText(firstLine, x - firstWidth / 2, 0f, bubblePrimaryTextPaint)

        //  Text 2
        val secondLine = config.bubbleHint
        val secondWidth = bubbleSecondaryTextPaint.measureText(secondLine)

        canvas.translate(0f, secondaryTextHeight)
        canvas.drawText(secondLine, x - secondWidth / 2, 0f, bubbleSecondaryTextPaint)
        canvas.restore()
    }

    fun setBubbleHint(hint: String) {
        config.bubbleHint = hint
    }
}
