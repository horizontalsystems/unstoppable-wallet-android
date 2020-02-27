package io.horizontalsystems.views

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import io.horizontalsystems.views.helpers.LayoutHelper
import kotlinx.android.synthetic.main.view_icon.view.*

class IconView : ConstraintLayout {

    init {
        inflate(context, R.layout.view_icon, this)
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    fun bind(drawable: Drawable) {
        dynamicIcon.setImageDrawable(drawable)
    }

    fun bind(coinCode: String) {
        dynamicIcon.setImageResource(LayoutHelper.getCoinDrawableResource(context, coinCode))
        tintWithGreyColor()
    }

    fun bind(icon: Int, tintWithGrey: Boolean = false) {
        dynamicIcon.setImageResource(icon)
        if (tintWithGrey) {
            tintWithGreyColor()
        }
    }

    fun setTint(tintColor: ColorStateList) {
        dynamicIcon.imageTintList = tintColor
    }

    private fun tintWithGreyColor() {
        val greyColor = ContextCompat.getColor(context, R.color.grey)
        val tintColorStateList = ColorStateList.valueOf(greyColor)
        setTint(tintColorStateList)
    }
}
