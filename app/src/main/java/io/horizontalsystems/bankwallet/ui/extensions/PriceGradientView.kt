package io.horizontalsystems.bankwallet.ui.extensions

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Matrix
import android.graphics.drawable.Drawable
import android.graphics.drawable.TransitionDrawable
import android.util.AttributeSet
import android.widget.ImageView
import androidx.cardview.widget.CardView
import androidx.core.graphics.values
import io.horizontalsystems.bankwallet.R
import kotlinx.android.synthetic.main.view_price_gradient.view.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class PriceGradientView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : CardView(context, attrs, defStyleAttr) {

    private val matrixImage: MatrixImage
    private var diffPrev: Float? = null

    private var gradient: Drawable? = null
    private var gradientRed: Drawable? = null

    private val alphaDuration = 600L
    private val translateDuration = 1000L

    init {
        inflate(context, R.layout.view_price_gradient, this)

        val ta = context.obtainStyledAttributes(attrs, R.styleable.PriceGradientView)
        try {
            gradient = ta.getDrawable(R.styleable.PriceGradientView_gradient)
            gradientRed = ta.getDrawable(R.styleable.PriceGradientView_gradientSecond)
            setGradient(gradient)
        } finally {
            ta.recycle()
        }

        matrixImage = MatrixImage(gradientImage)
    }

    fun animateHorizontal(diff: Float?) {
        if (diff == null) {
            diffPrev = null
            animateAlpha(0f)
            return
        }

        val (moveFrom, moveTo) = matrixImage.getTranslateX(diff)

        if (diffPrev == null) {
            diffPrev = diff
            setGradient(gradient)
            matrixImage.setTranslateX(moveTo)
            animateAlpha(1f)
            return
        }

        ObjectAnimator.ofFloat(matrixImage, "translateX", moveFrom, moveTo).apply {
            duration = translateDuration
            start()
        }
    }

    fun setBackground(color: Int) {
        gradientImage.setImageDrawable(null)
        setBackgroundColor(color)
        animateAlpha(1f)
    }

    fun animateVertical(diff: Float?) {
        if (diff == null) {
            diffPrev = null
            animateAlpha(0f)
            return
        }

        val (moveFrom, moveTo) = matrixImage.getTranslateY(diff)

        val prevDiff = diffPrev
        if (prevDiff == null) {
            diffPrev = diff

            setGradient(if (diff >= 0f) gradient else gradientRed)
            matrixImage.setTranslateY(moveTo)
            animateAlpha(1f)
            return
        }

        diffPrev = diff

        // Switch gradient colors
        when {
            prevDiff <= 0 && diff > 0 -> {
                animateBgChange(arrayOf(gradientRed, gradient), moveTo)
                return
            }
            prevDiff >= 0 && diff < 0 -> {
                animateBgChange(arrayOf(gradient, gradientRed), moveTo)
                return
            }
        }

        ObjectAnimator.ofFloat(matrixImage, "translateY", moveFrom, moveTo).apply {
            duration = translateDuration
            start()
        }
    }

    private fun animateBgChange(arrayOf: Array<Drawable?>, diff: Float) {
        val transition = TransitionDrawable(arrayOf).apply {
            isCrossFadeEnabled = true
        }

        setGradient(transition)
        matrixImage.setTranslateY(diff)
        transition.startTransition(alphaDuration.toInt())
    }

    private fun animateAlpha(alpha: Float) {
        animate().alpha(alpha).duration = alphaDuration
    }

    private fun setGradient(drawable: Drawable?) {
        gradientImage.setImageDrawable(drawable)
    }

    class MatrixImage(private val imageView: ImageView) {
        private val matrix = Matrix(imageView.imageMatrix)

        fun setTranslateX(dx: Float) {
            matrix.reset()
            matrix.postTranslate(dx, 0f)

            imageView.imageMatrix = matrix
        }

        fun setTranslateY(dy: Float) {
            matrix.reset()
            matrix.postTranslate(0f, dy)

            imageView.imageMatrix = matrix
        }

        fun getTranslateY(to: Float): Pair<Float, Float> {
            val middle = imageView.drawable.intrinsicHeight / 2f

            val prevPosition = matrix.values()[Matrix.MTRANS_Y]
            val movePosition = if (to == 0f) {
                0f
            } else {
                abs(diff(to)) / 100 * middle
            }

            return Pair(prevPosition, -movePosition)
        }

        fun getTranslateX(to: Float): Pair<Float, Float> {
            val middle = (imageView.drawable.intrinsicWidth - imageView.width) / 2f

            val prevPosition = matrix.values()[Matrix.MTRANS_X]
            val movePosition = if (to == 0f) {
                middle
            } else {
                middle + (-diff(to) / 100 * middle)
            }

            return Pair(prevPosition, -movePosition)
        }

        private fun diff(to: Float): Float {
            return min(max(to, -100f), 100f)
        }
    }
}
