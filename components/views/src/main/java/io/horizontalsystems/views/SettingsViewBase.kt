package io.horizontalsystems.views

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import io.horizontalsystems.views.helpers.LayoutHelper
import kotlinx.android.synthetic.main.view_settings_item.view.*

abstract class SettingsViewBase @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : ConstraintLayout(context, attrs, defStyleAttr) {

    private val singleLineHeight = 48f
    private val doubleLineHeight = 60f

    fun showSubtitle(text: String?) {
        settingsSubtitle.text = text
        settingsSubtitle.isVisible = text != null
        layoutParams?.height = LayoutHelper.dp(if (text == null) singleLineHeight else doubleLineHeight, context)
    }

    fun showTitle(text: String?) {
        settingsTitle.text = text
    }

    fun showIcon(drawable: Drawable?) {
        settingsIcon.isVisible = drawable != null
        settingsIcon.setImageDrawable(drawable)
    }

    fun setListPosition(listPosition: ListPosition){
        findViewById<View>(R.id.frame)?.let {
            it.setBackgroundResource(listPosition.getBackground())
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        if (!settingsSubtitle.text.isNullOrBlank()) {
            layoutParams.height = LayoutHelper.dp(doubleLineHeight, context)
        }
    }
}

sealed class ListPosition{
    object First: ListPosition()
    object Middle: ListPosition()
    object Last: ListPosition()
    object Single: ListPosition()

    fun getBackground(): Int {
        return when (this) {
            First -> R.drawable.rounded_lawrence_background_top
            Middle -> R.drawable.rounded_lawrence_background_middle
            Last -> R.drawable.rounded_lawrence_background_bottom
            Single -> R.drawable.rounded_lawrence_background_single
        }
    }

    companion object{
        fun getListPosition(size: Int, position: Int): ListPosition {
            return when  {
                size == 1 -> Single
                position == 0 -> First
                position == size - 1 -> Last
                else -> Middle
            }
        }
    }
}
