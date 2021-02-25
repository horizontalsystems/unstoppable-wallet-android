package io.horizontalsystems.bankwallet.ui.extensions

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.views.helpers.LayoutHelper

class PicassoRoundedImageView(context: Context, attributeSet: AttributeSet?) : View(context, attributeSet), Target {
    constructor(context: Context) : this(context, null)

    private val radius: Int

    init {
        val attributes = context.obtainStyledAttributes(attributeSet, R.styleable.PicassoRoundedImageView)
        try {
            radius = attributes.getInt(R.styleable.PicassoRoundedImageView_radius, DEFAULT_RADIUS)
        } finally {
            attributes.recycle()
        }
    }

    private var drawable: Drawable? = null
        set(value) {
            field = value
            postInvalidate()
        }

    fun loadImage(url: String?) {
        if (url == null) {
            drawable = null
        } else {
            Picasso.get()
                    .load(url)
                    .into(this)
        }
    }

    override fun onDraw(canvas: Canvas?) {
        drawable?.setBounds(0, 0, width, height)
        canvas?.let { drawable?.draw(it) }
    }

    override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
        drawable = placeHolderDrawable
    }

    override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
        drawable = errorDrawable
    }

    override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
        val roundedDrawable = RoundedBitmapDrawableFactory.create(resources, bitmap)
        roundedDrawable.cornerRadius = LayoutHelper.dp(radius.toFloat(), context).toFloat()
        drawable = roundedDrawable
    }

    companion object {
        private const val DEFAULT_RADIUS = 16
    }
}
